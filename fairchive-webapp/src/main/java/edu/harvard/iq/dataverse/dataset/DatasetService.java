package edu.harvard.iq.dataverse.dataset;

import static edu.harvard.iq.dataverse.persistence.dataset.DatasetLock.Reason.InReview;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.Shoulder;
import static java.time.Clock.systemDefaultZone;
import static java.time.Instant.now;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import edu.harvard.iq.dataverse.DatasetDao;
import edu.harvard.iq.dataverse.DatasetPage;
import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.DvObjectServiceBean;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.PermissionServiceBean;
import edu.harvard.iq.dataverse.annotations.PermissionNeeded;
import edu.harvard.iq.dataverse.api.AbstractApiBean;
import edu.harvard.iq.dataverse.engine.command.exception.NotAuthenticatedException;
import edu.harvard.iq.dataverse.engine.command.impl.CreateNewDatasetCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetGuestbookCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetThumbnailCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateDatasetVersionCommand;
import edu.harvard.iq.dataverse.globalid.GlobalIdServiceBean;
import edu.harvard.iq.dataverse.ingest.IngestServiceBean;
import edu.harvard.iq.dataverse.interceptors.LoggedCall;
import edu.harvard.iq.dataverse.interceptors.Restricted;
import edu.harvard.iq.dataverse.interceptors.SuperuserRequired;
import edu.harvard.iq.dataverse.notification.NotificationObjectType;
import edu.harvard.iq.dataverse.notification.UserNotificationService;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersionUser;
import edu.harvard.iq.dataverse.persistence.dataset.Template;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.NotificationType;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.provenance.ProvPopupFragmentBean;
import edu.harvard.iq.dataverse.search.index.IndexServiceBean;
import edu.harvard.iq.dataverse.search.index.SolrIndexServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

@Stateless
public class DatasetService {

    private static final Logger logger = Logger.getLogger(DatasetPage.class.getCanonicalName());
    private static final String DATASET_LOCKED_FOR_UPDATE_MESSAGE = "Update embargo date failed. Dataset is locked. ";
    private static final String DATASET_IN_WRONG_STATE_MESSAGE = "Setting embargo date failed. Dataset is in a non-editable state.";

    private EjbDataverseEngine commandEngine;
    private UserNotificationService userNotificationService;
    private DatasetRepository datasetRepo;
    private DataverseSession session;
    private DataverseRequestServiceBean dvRequestService;
    private IngestServiceBean ingestService;
    private SettingsServiceBean settingsService;
    private ProvPopupFragmentBean provPopupFragmentBean;
    private SolrIndexServiceBean solrIndexService;
    private IndexServiceBean indexService;
    private DatasetDao datasetDao;
    private DvObjectServiceBean dvObjectService;
    private DatasetThumbnailService datasetThumbnailService;


    // -------------------- CONSTRUCTORS --------------------

    @Deprecated
    public DatasetService() {
    }

    @Inject
    public DatasetService(final EjbDataverseEngine commandEngine, 
            final UserNotificationService userNotificationService,
            final DatasetRepository datasetRepo, 
            final DataverseSession session, 
            final DataverseRequestServiceBean dvRequestService,
            final IngestServiceBean ingestService, 
            final SettingsServiceBean settingsService,
            final ProvPopupFragmentBean provPopupFragmentBean, 
            final PermissionServiceBean permissionService,
            final SolrIndexServiceBean solrIndexService, 
            final IndexServiceBean indexService,
            final DatasetDao datasetDao,
            final DvObjectServiceBean dvObjectService,
            final DatasetThumbnailService datasetThumbnailService) {
        this.commandEngine = commandEngine;
        this.userNotificationService = userNotificationService;
        this.datasetRepo = datasetRepo;
        this.session = session;
        this.dvRequestService = dvRequestService;
        this.ingestService = ingestService;
        this.settingsService = settingsService;
        this.provPopupFragmentBean = provPopupFragmentBean;
        this.solrIndexService = solrIndexService;
        this.indexService = indexService;      
        this.datasetDao = datasetDao;
        this.dvObjectService = dvObjectService;
        this.datasetThumbnailService = datasetThumbnailService;
    }


    // -------------------- LOGIC --------------------

    public Dataset find(Long id) {
        return this.datasetRepo.getById(id);
    }
    
    public List<Dataset> findAll() {
        return this.datasetRepo.findAllOrderedById();
    }
    
    public List<Long> findAllLocalDatasetIds() {
        return this.datasetRepo.findAllLocalDatasetIds();
    }
    
    public List<Long> findAllOrSubset(final long numPartitions, 
            final long partitionId, final boolean skipIndexed) { 
        return skipIndexed 
                ? this.datasetRepo.findAllOrSubsetSkippingIndexed(numPartitions, partitionId)
                : this.datasetRepo.findAllOrSubset(numPartitions, partitionId);
    }
    
    public List<Dataset> findStaleOrMissingDatasets() {
        return findAll().stream().filter(DvObject::isStale).collect(toList());
    }
    
    public Dataset findByGlobalId(final String globalId) {
        final Dataset retVal = (Dataset) this.dvObjectService.findByGlobalId(globalId, "Dataset");
        if (retVal != null) {
            return retVal;
        } else {
            //try to find with alternative PID
            return (Dataset) this.dvObjectService.findByGlobalId(globalId, "Dataset", true);
        }
    }
    
    public List<Dataset> findByOwnerId(Long ownerId) {
        return datasetRepo.findByOwnerId(ownerId);
    }
    
    public List<Dataset> findNotIndexedAfterEmbargo() {
        return this.datasetRepo.findNotIndexedAfterEmbargo();
    }
    
    public Dataset saveAndFlush(Dataset ds) {
        return this.datasetRepo.saveAndFlush(ds);
    }
    
    public Dataset save(Dataset ds) {
        return this.datasetRepo.save(ds);
        
    }
    
    public Dataset getDatasetByHarvestInfo(final Dataverse dataverse, 
            final String harvestIdentifier) {
        return this.datasetRepo.getDatasetByHarvestInfo(dataverse.getId(), 
                harvestIdentifier);
    }
    
    public String generateDatasetIdentifier(final Dataset dataset) {
        final String shoulder = this.settingsService.getValueForKey(Shoulder);  
        for(;;) {
            final String id = shoulder.concat(randomAlphanumeric(6).toUpperCase());
            if(this.datasetRepo.isIdentifierLocallyUnique(id, dataset)) {
                return id;
            }
        }
    }
    
    /**
     * Check that a identifier entered by the user is unique (not currently used
     * for any other study in this Dataverse Network) also check for duplicate
     * in EZID if needed
     *
     * @param userIdentifier
     * @param dataset
     * @param persistentIdSvc
     * @return {@code true} if the identifier is unique, {@code false} otherwise.
     */
    public boolean isIdentifierUnique(String userIdentifier, Dataset dataset, 
            GlobalIdServiceBean persistentIdSvc) {
        if (!this.datasetRepo.isIdentifierLocallyUnique(userIdentifier, dataset)) {
            return false; // duplication found in local database
        }

        // not in local DB, look in the persistent identifier service
        try {
            return !persistentIdSvc.alreadyExists(dataset);
        } catch (Exception e) {
            //we can live with failure - means identifier not found remotely
            return true;
        }
    }
    
    public boolean isIdentifierLocallyUnique(final Dataset dataset) {
        return this.datasetRepo.isIdentifierLocallyUnique(dataset.getIdentifier(), 
                dataset);
    }
    
    public DatasetLock addDatasetLock(Long datasetId, DatasetLock.Reason reason, 
            Long userId, String info) {
        return this.datasetDao.addDatasetLock(datasetId, reason, userId, info);
    }
    
    public DatasetLock addDatasetLock(Dataset dataset, DatasetLock lock) {
        return this.datasetDao.addDatasetLock(dataset, lock);
    }
    
    public void removeDatasetLocks(Dataset dataset, DatasetLock.Reason aReason) {
        this.datasetDao.removeDatasetLocks(dataset, aReason);
    }
    
    public List<DatasetLock> getDatasetLocksByUser(final AuthenticatedUser user) {
        return this.datasetDao.getDatasetLocksByUser(user);
    }
    
    public List<DatasetVersionUser> getDatasetVersionUsersByAuthenticatedUser(
            final AuthenticatedUser user) {
        return this.datasetDao.getDatasetVersionUsersByAuthenticatedUser(user);
    }
    
    public void assignDatasetThumbnailByNativeQuery(Dataset dataset, DataFile dataFile) {
        this.datasetRepo.assignThumbnail(dataset.getId(), dataFile.getId());
    }
    
    
    public Dataset createDataset(Dataset dataset, Template usedTemplate) {

        AuthenticatedUser user = retrieveAuthenticatedUser();

        CreateNewDatasetCommand createCommand = new CreateNewDatasetCommand(dataset, dvRequestService.getDataverseRequest(), false, usedTemplate);
        dataset = commandEngine.submit(createCommand);


        userNotificationService.sendNotificationWithEmail(user, dataset.getCreateDate(), NotificationType.CREATEDS,
                                                          dataset.getLatestVersion().getId(), NotificationObjectType.DATASET_VERSION);

        return dataset;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public AddFilesResult addFilesToDataset(Dataset dataset, List<DataFile> newFiles) {

        List<DataFile> savedFiles = ingestService.saveAndAddFilesToDataset(dataset.getEditVersion(), newFiles);

        return new AddFilesResult(dataset, savedFiles.size(), newFiles.size());
    }

    /**
     * Replaces thumbnail (default if none is set) with the one provided.
     *
     * @param datasetForNewThumbnail dataset that will have new thumbnail
     * @param thumbnailFile          thumbnail that will be set for dataset
     */
    public DatasetThumbnail changeDatasetThumbnail(Dataset datasetForNewThumbnail, DataFile thumbnailFile) {
        return commandEngine.submit(new UpdateDatasetThumbnailCommand(dvRequestService.getDataverseRequest(),
                                                                      datasetForNewThumbnail,
                                                                      UpdateDatasetThumbnailCommand.UserIntent.setDatasetFileAsThumbnail,
                                                                      thumbnailFile.getId(),
                                                                      null));

    }

    /**
     * Replaces thumbnail (default if none is set) with the one provided.
     *
     * @param datasetForNewThumbnail dataset that will have new thumbnail
     * @param fileStream             thumbnail that will be set for dataset
     */
    public DatasetThumbnail changeDatasetThumbnail(Dataset datasetForNewThumbnail, InputStream fileStream) {
        return commandEngine.submit(new UpdateDatasetThumbnailCommand(dvRequestService.getDataverseRequest(),
                                                                      datasetForNewThumbnail,
                                                                      UpdateDatasetThumbnailCommand.UserIntent.setNonDatasetFileAsThumbnail,
                                                                      null,
                                                                      fileStream));

    }

    public DatasetThumbnail removeDatasetThumbnail(Dataset datasetWithThumbnail) {
        return commandEngine.submit(new UpdateDatasetThumbnailCommand(dvRequestService.getDataverseRequest(),
                                                                      datasetWithThumbnail,
                                                                      UpdateDatasetThumbnailCommand.UserIntent.removeThumbnail,
                                                                      null,
                                                                      null));

    }

    public Dataset updateDatasetGuestbook(Dataset editedDataset) {
        return commandEngine.submit(new UpdateDatasetGuestbookCommand(dvRequestService.getDataverseRequest(), editedDataset));
    }

    @LoggedCall
    @Restricted(@PermissionNeeded(needs = {Permission.EditDataset}))
    public Dataset setDatasetEmbargoDate(@PermissionNeeded Dataset dataset, Date embargoDate) throws IllegalStateException {
        if(dataset.hasEverBeenPublished() && !session.getUser().isSuperuser()) {
            throw new IllegalStateException(getDatasetInWrongStateMessage());
        }
        return updateDatasetEmbargoDate(dataset, embargoDate);
    }

    @LoggedCall
    @Restricted(@PermissionNeeded(needs = {Permission.EditDataset}))
    public Dataset liftDatasetEmbargoDate(@PermissionNeeded Dataset dataset) {
        return updateDatasetEmbargoDate(dataset, null);
    }

    String getDatasetLockedMessage(Dataset dataset) {
        return DATASET_LOCKED_FOR_UPDATE_MESSAGE.concat(dataset.getLocks().toString());
    }

    String getDatasetInWrongStateMessage() {
        return DATASET_IN_WRONG_STATE_MESSAGE;
    }

    @SuperuserRequired
    public void updateAllLastChangeForExporterTime() {
        this.datasetRepo.updateAllLastChangeForExporterTime();
    }

    @SuperuserRequired
    public void updateLastChangeForExporterTime(Dataset dataset) {
        dataset.setLastChangeForExporterTime(new Date());
        this.datasetRepo.save(dataset);
    }
    
    public Dataset setNonDatasetFileAsThumbnail(Dataset dataset, InputStream inputStream) {
        if (dataset == null) {
            return null;
        }
        if (inputStream == null) {
            return null;
        }
        dataset = this.datasetThumbnailService.persistDatasetLogoToStorageAndCreateThumbnail(dataset, inputStream);
        dataset.setThumbnailFile(null);
        dataset.setUseGenericThumbnail(false);
        return this.datasetRepo.save(dataset);
    }

    public Dataset setDatasetFileAsThumbnail(Dataset dataset, 
            DataFile datasetFileThumbnailToSwitchTo) {
        if (dataset == null) {
            return null;
        }
        if (datasetFileThumbnailToSwitchTo == null) {
            return null;
        }
        this.datasetThumbnailService.deleteDatasetLogo(dataset);
        dataset.setThumbnailFile(datasetFileThumbnailToSwitchTo);
        dataset.setUseGenericThumbnail(false);
        return this.datasetRepo.save(dataset);
    }
    
    // -------------------- PRIVATE --------------------

    private AuthenticatedUser retrieveAuthenticatedUser() {
        if (!session.isUserLoggedIn()) {
            throw new NotAuthenticatedException();
        }
        return (AuthenticatedUser) session.getUser();
    }

    private Dataset updateDatasetEmbargoDate(Dataset dataset, final Date embargoDate) 
            throws IllegalStateException {
        if(dataset.isLockedForOtherThan(InReview)) {
            logger.log(Level.WARNING, "Dataset is locked. Cannot perform update embargo date");
            throw new IllegalStateException(getDatasetLockedMessage(dataset));
        }

        dataset.setEmbargoDate(embargoDate);
        dataset.setLastChangeForExporterTime(Date.from(now(systemDefaultZone())));
        dataset = this.datasetRepo.saveAndFlush(dataset);
        this.indexService.indexDataset(dataset, false);

        solrIndexService.indexPermissionsForDatasetWithDataFiles(dataset);

        return dataset;
    }

}
