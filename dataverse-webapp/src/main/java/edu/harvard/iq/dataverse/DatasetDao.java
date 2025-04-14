package edu.harvard.iq.dataverse;

import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.IdentifierGenerationStyle;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.Shoulder;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Logger;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TemporalType;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnailService;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.impl.FinalizeDatasetPublicationCommand;
import edu.harvard.iq.dataverse.globalid.GlobalIdServiceBean;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLockRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersionRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersionUser;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.persistence.workflow.WorkflowComment;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;

/**
 * @author skraffmiller
 */


@SuppressWarnings("serial")
@Stateless
public class DatasetDao implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(DatasetDao.class.getCanonicalName());

    @Inject
    SettingsServiceBean settingsService;

    @EJB
    DvObjectServiceBean dvObjectService;

    @EJB
    AuthenticationServiceBean authentication;

    @EJB
    DataFileServiceBean fileService;

    @EJB
    EjbDataverseEngine commandEngine;

    @EJB
    private DatasetThumbnailService datasetThumbnailService;
    
    @Inject
    private DatasetRepository datasetRepo;
    @Inject
    private DatasetLockRepository datasetLockRepo;
    @Inject 
    private DatasetVersionRepository datasetVersionRepo;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    protected EntityManager em;

    public Dataset find(Object pk) {
        return this.datasetRepo.getById((Long)pk);
    }

    public List<Dataset> findByOwnerId(Long ownerId) {
        return this.datasetRepo.findByOwnerId(ownerId);
    }

    public List<Dataset> findAll() {
        return this.datasetRepo.findAll();
    }
    
    public List<Dataset> findStaleOrMissingDatasets() {
        return this.datasetRepo.findStaleOrMissingDatasets();
    }

    public List<Dataset> findNotIndexedAfterEmbargo() {
        return this.datasetRepo.findNotIndexedAfterEmbargo();
    }

    public List<Long> findAllLocalDatasetIds() {
        return this.datasetRepo.findAllLocalDatasetIds();
    }

    public List<Long> findAllUnindexed() {
        return this.datasetRepo.findAllUnindexed();
    }

    /**
     * For docs, see the equivalent method on the DataverseServiceBean.
     *
     * @param numPartitions
     * @param partitionId
     * @param skipIndexed
     * @return a list of datasets
     * @see DataverseDao#findAllOrSubset(long, long, boolean)
     */
    public List<Long> findAllOrSubset(final long numPartitions, final long partitionId,
            final boolean skipIndexed) {
         return this.datasetRepo.findAllOrSubset(numPartitions, partitionId, skipIndexed);
    }

    /**
     * Merges the passed dataset to the persistence context.
     *
     * @param ds the dataset whose new state we want to persist.
     * @return The managed entity representing {@code ds}.
     */
    public Dataset merge(final Dataset ds) {
        return this.datasetRepo.save(ds);
    }

    public Dataset mergeAndFlush(final Dataset ds) {
        return this.datasetRepo.saveAndFlush(ds);
    }

    public Dataset findByGlobalId(String globalId) {
        Dataset retVal = (Dataset) dvObjectService.findByGlobalId(globalId, "Dataset");
        if (retVal != null) {
            return retVal;
        } else {
            //try to find with alternative PID
            return (Dataset) dvObjectService.findByGlobalId(globalId, "Dataset", true);
        }
    }

    public String generateDatasetIdentifier(final Dataset dataset) {
        final String identifierType = this.settingsService.getValueForKey(IdentifierGenerationStyle);
        final String shoulder = this.settingsService.getValueForKey(Shoulder);
        final Supplier<String> generator = identifierType.equals("sequentialNumber") 
            ? () -> shoulder.concat(this.datasetRepo.generateIdentifierAsSequentialNumber())
            : () -> shoulder.concat(randomAlphanumeric(6).toUpperCase());
        
        return generateId(dataset, generator);
    }
    
    private String generateId(final Dataset dataset, final Supplier<String> generator) {      
        while (true) {
            final String id = generator.get();
            if (isIdentifierLocallyUnique(id, dataset)) {
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
    public boolean isIdentifierUnique(final String userIdentifier,
            final Dataset dataset, final GlobalIdServiceBean persistentIdSvc) {
        if (!isIdentifierLocallyUnique(userIdentifier, dataset)) {
            return false; // duplication found in local database
        } else {
            // not in local DB, look in the persistent identifier service
            try {
                return !persistentIdSvc.alreadyExists(dataset);
            } catch (final Exception e) {
                // we can live with failure - means identifier not found remotely
                return true;
            }
        }
    }

    public boolean isIdentifierLocallyUnique(Dataset dataset) {
        return isIdentifierLocallyUnique(dataset.getIdentifier(), dataset);
    }

    public boolean isIdentifierLocallyUnique(String identifier, Dataset dataset) {
        return this.datasetRepo.findByIdentifierAuthorityAndProtocol(identifier,
                dataset.getAuthority(), dataset.getProtocol()).isEmpty();
    }

    public DatasetVersion storeVersion(DatasetVersion dsv) {
        return this.datasetVersionRepo.save(dsv);
    }


    public DatasetVersionUser getDatasetVersionUser(DatasetVersion version, User user) {
        String identifier = user.getIdentifier();
        identifier = identifier.startsWith("@") ? identifier.substring(1) : identifier;
        AuthenticatedUser au = authentication.getAuthenticatedUser(identifier);
        return this.datasetVersionRepo.getDatasetVersionUser(version.getId(), au.getId()).orElse(null);
    }

    public List<DatasetVersionUser> getDatasetVersionUsersByAuthenticatedUser(
            final AuthenticatedUser user) {
        return this.datasetVersionRepo
                .getDatasetVersionUsersByAuthenticatedUser(user.getId());
    }



    public List<DatasetLock> getDatasetLocksByUser(final AuthenticatedUser user) {
        return this.datasetLockRepo.findByUser(user);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public DatasetLock addDatasetLock(Dataset dataset, DatasetLock lock) {
        lock.setDataset(dataset);
        dataset.addLock(lock);
        this.datasetLockRepo.save(lock);
        this.datasetRepo.save(dataset);
        return lock;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW) /*?*/
    public DatasetLock addDatasetLock(Long datasetId, DatasetLock.Reason reason,
            Long userId, String info) {
        final Dataset dataset = this.datasetRepo.getById(datasetId);
        final AuthenticatedUser user = this.authentication.findByID(userId);

        // Check if the dataset is already locked for this reason:
        // (to prevent multiple, duplicate locks on the dataset!)
        final Optional<DatasetLock> lock = dataset.getLockFor(reason);
        if (lock.isPresent()) {
            return lock.get();
        } else {
            final DatasetLock newLock = new DatasetLock(reason, dataset, user, info);
            return addDatasetLock(dataset, newLock);
        }
    }

    /**
     * Removes all {@link DatasetLock}s for the dataset whose id is passed and reason
     * is {@code aReason}.
     *
     * @param dataset the dataset whose locks (for {@code aReason}) will be removed.
     * @param aReason The reason of the locks that will be removed.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void removeDatasetLocks(Dataset dataset, DatasetLock.Reason aReason) {
        dataset.getAllLocksFor(aReason).forEach(this::remove);
    }
    
    private void remove(DatasetLock lock) {
        lock.removeFromDataset();
        this.datasetLockRepo.save(lock);

        AuthenticatedUser user = lock.getUser();
        user.getDatasetLocks().remove(lock);

        this.datasetLockRepo.delete(lock);
    }

    public String getTitleFromLatestVersion(Long datasetId) {
        return this.datasetRepo.getTitleFromLatestVersion(datasetId);
    }

    public Dataset getDatasetByHarvestInfo(Dataverse dataverse,
            String harvestIdentifier) {
        final List<Dataset> resultList = this.datasetRepo
                .findByOwnerIdAndHarvestIdentifier(dataverse.getId(), harvestIdentifier);
        if (resultList.size() > 1) {
            throw new EJBException("More than one dataset found in the dataverse (id= "
                    + dataverse.getId() + "), with harvestIdentifier= "
                    + harvestIdentifier);
        } else if (resultList.size() == 1) {
            return resultList.get(0);
        } else {
            return null;
        }
    }

    public Dataset setNonDatasetFileAsThumbnail(Dataset dataset, InputStream inputStream) {
        dataset = datasetThumbnailService.persistDatasetLogoToStorageAndCreateThumbnail(dataset, inputStream);
        dataset.setThumbnailFile(null);
        dataset.setUseGenericThumbnail(false);
        return merge(dataset);
    }

    public Dataset setDatasetFileAsThumbnail(Dataset dataset, DataFile datasetFileThumbnailToSwitchTo) {
        datasetThumbnailService.deleteDatasetLogo(dataset);
        dataset.setThumbnailFile(datasetFileThumbnailToSwitchTo);
        dataset.setUseGenericThumbnail(false);
        return merge(dataset);
    }

    public Dataset removeDatasetThumbnail(Dataset dataset) {
        datasetThumbnailService.deleteDatasetLogo(dataset);
        dataset.setThumbnailFile(null);
        dataset.setUseGenericThumbnail(true);
        return merge(dataset);
    }

    public void assignDatasetThumbnailByNativeQuery(Dataset dataset, DataFile dataFile) {
        try {
            em.createNativeQuery("UPDATE dataset SET thumbnailfile_id=" + dataFile.getId() + " WHERE id=" + dataset.getId()).executeUpdate();
        } catch (Exception ex) {
            // it's ok to just ignore...
        }
    }

    public WorkflowComment addWorkflowComment(WorkflowComment workflowComment) {
        em.persist(workflowComment);
        return workflowComment;
    }

    @Asynchronous
    public void callFinalizePublishCommandAsynchronously(Long datasetId, CommandContext ctxt, DataverseRequest request, boolean isPidPrePublished)  {

        // Since we are calling the next command asynchronously anyway - sleep here
        // for a few seconds, just in case, to make sure the database update of
        // the dataset initiated by the PublishDatasetCommand has finished,
        // to avoid any concurrency/optimistic lock issues.
        try {
            Thread.sleep(15000);
        } catch (Exception ex) {
            logger.warning("Failed to sleep for 15 seconds.");
        }
        logger.fine("Running FinalizeDatasetPublicationCommand, asynchronously");
        Dataset theDataset = this.datasetRepo.getById(datasetId);
        commandEngine.submit(new FinalizeDatasetPublicationCommand(theDataset, request, isPidPrePublished));
    }

    public void updateAllLastChangeForExporterTime() {
        Date date = new Date();
        Query query = em.createQuery(
                "UPDATE Dataset ds SET ds.lastChangeForExporterTime=:date WHERE ds.harvestedFrom IS NULL");
        query.setParameter("date", date, TemporalType.TIMESTAMP);
        query.executeUpdate();
    }
    
}
