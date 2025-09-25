package edu.harvard.iq.dataverse;

import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.Shoulder;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

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
import javax.persistence.TypedQuery;

import org.slf4j.Logger;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnailService;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.impl.FinalizeDatasetPublicationCommand;
import edu.harvard.iq.dataverse.globalid.GlobalIdServiceBean;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
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

    private static final Logger logger = getLogger(DatasetDao.class);

    @Inject
    SettingsServiceBean settings;

    @EJB
    DvObjectServiceBean dvObjectService;

    @EJB
    AuthenticationServiceBean authentication;

    @EJB
    DataFileServiceBean fileService;

    @EJB
    EjbDataverseEngine commandEngine;

    @EJB
    private DatasetRepository datasetRepository;

    @EJB
    private DatasetThumbnailService datasetThumbnailService;
    @EJB
    private DatasetLockRepository datasetLockRepo;
    @EJB
    private DatasetVersionRepository datasetVersionRepo;

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    protected EntityManager em;

    public Dataset find(Object pk) {
        return em.find(Dataset.class, pk);
    }

    public List<Dataset> findByOwnerId(Long ownerId) {
        return datasetRepository.findByOwnerId(ownerId);
    }

    public List<Dataset> findAll() {
        return this.datasetRepository.findAllOrderedById();
    }
    
    public List<Dataset> findStaleOrMissingDatasets() {
        return findAll().stream().filter(DvObject::isStale).collect(toList());
    }

    public List<Dataset> findNotIndexedAfterEmbargo() {
        return this.datasetRepository.findNotIndexedAfterEmbargo();
    }

    public List<Long> findAllLocalDatasetIds() {
        return this.datasetRepository.findAllLocalDatasetIds();
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
    public List<Long> findAllOrSubset(final long numPartitions, 
            final long partitionId, final boolean skipIndexed) { 
        return skipIndexed 
                ? this.datasetRepository.findAllOrSubsetSkippingIndexed(numPartitions, partitionId)
                : this.datasetRepository.findAllOrSubset(numPartitions, partitionId);
    }

    /**
     * Merges the passed dataset to the persistence context.
     *
     * @param ds the dataset whose new state we want to persist.
     * @return The managed entity representing {@code ds}.
     */
    public Dataset merge(Dataset ds) {
        return em.merge(ds);
    }

    public Dataset mergeAndFlush(Dataset ds) {
        Dataset merged = em.merge(ds);
        em.flush();
        return merged;
    }

    public Dataset findByGlobalId(final String globalId) {
        Dataset retVal = (Dataset) dvObjectService.findByGlobalId(globalId, "Dataset");
        if (retVal != null) {
            return retVal;
        } else {
            //try to find with alternative PID
            return (Dataset) dvObjectService.findByGlobalId(globalId, "Dataset", true);
        }
    }

    public String generateDatasetIdentifier(final Dataset dataset) {
        final String shoulder = this.settings.getValueForKey(Shoulder);  
        for(;;) {
            final String id = shoulder.concat(randomAlphanumeric(6).toUpperCase());
            if(this.datasetRepository.isIdentifierLocallyUnique(id, dataset)) {
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
    public boolean isIdentifierUnique(String userIdentifier, Dataset dataset, GlobalIdServiceBean persistentIdSvc) {
        if (!this.datasetRepository.isIdentifierLocallyUnique(userIdentifier, dataset)) {
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
        return this.datasetRepository.isIdentifierLocallyUnique(dataset.getIdentifier(), dataset);
    }

    public DatasetVersion storeVersion(DatasetVersion dsv) {
        em.persist(dsv);
        return dsv;
    }


    public DatasetVersionUser getDatasetVersionUser(DatasetVersion version, User user) {

        TypedQuery<DatasetVersionUser> query = em.createNamedQuery("DatasetVersionUser.findByVersionIdAndUserId", DatasetVersionUser.class);
        query.setParameter("versionId", version.getId());
        String identifier = user.getIdentifier();
        identifier = identifier.startsWith("@") ? identifier.substring(1) : identifier;
        AuthenticatedUser au = authentication.getAuthenticatedUser(identifier);
        query.setParameter("userId", au.getId());
        try {
            return query.getSingleResult();
        } catch (javax.persistence.NoResultException e) {
            return null;
        }
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
        lock.setStartTime(new Date());
        em.persist(lock);
        em.merge(dataset);
        return lock;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW) /*?*/
    public DatasetLock addDatasetLock(Long datasetId, DatasetLock.Reason reason, Long userId, String info) {

        Dataset dataset = em.find(Dataset.class, datasetId);

        AuthenticatedUser user = null;
        if (userId != null) {
            user = em.find(AuthenticatedUser.class, userId);
        }

        // Check if the dataset is already locked for this reason:
        // (to prevent multiple, duplicate locks on the dataset!)
        DatasetLock lock = dataset.getLockFor(reason);
        if (lock != null) {
            return lock;
        }

        // Create new:
        lock = new DatasetLock(reason, user);
        lock.setDataset(dataset);
        lock.setInfo(info);
        lock.setStartTime(new Date());

        if (userId != null) {
            lock.setUser(user);
            if (user.getDatasetLocks() == null) {
                user.setDatasetLocks(new ArrayList<>());
            }
            user.getDatasetLocks().add(lock);
        }

        return addDatasetLock(dataset, lock);
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
        if (dataset != null) {
            new HashSet<>(dataset.getLocks()).stream()
                    .filter(l -> l.getReason() == aReason)
                    .forEach(lock -> {
                        lock = em.merge(lock);
                        dataset.removeLock(lock);

                        AuthenticatedUser user = lock.getUser();
                        user.getDatasetLocks().remove(lock);

                        em.remove(lock);
                    });
        }
    }

    /*
    getTitleFromLatestVersion methods use native query to return a dataset title

        There are two versions:
     1) The version with datasetId param only will return the title regardless of version state
     2)The version with the param 'includeDraft' boolean  will return the most recently published title if the param is set to false
    If no Title found return empty string - protects against calling with
    include draft = false with no published version
    */

    public String getTitleFromLatestVersion(Long datasetId) {
        return getTitleFromLatestVersion(datasetId, true);
    }

    public String getTitleFromLatestVersion(Long datasetId, boolean includeDraft) {

        String whereDraft = "";
        //This clause will exclude draft versions from the select
        if (!includeDraft) {
            whereDraft = " and v.versionstate !='DRAFT' ";
        }

        try {
            return (String) em.createNativeQuery("select df.fieldvalue  from dataset d "
                                                         + " join datasetversion v on d.id = v.dataset_id "
                                                         + " join datasetfield df on v.id = df.datasetversion_id "
                                                         + " join datasetfieldtype dft on df.datasetfieldtype_id  = dft.id "
                                                         + " where dft.name = '" + DatasetFieldConstant.title + "' and  v.dataset_id =" + datasetId
                                                         + " and df.source = '" + DatasetField.DEFAULT_SOURCE + "'"
                                                         + whereDraft
                                                         + " order by v.versionnumber desc, v.minorVersionNumber desc limit 1 "
                                                         + ";").getSingleResult();

        } catch (Exception ex) {
            logger.info("exception trying to get title from latest version: {0}", ex);
            return "";
        }

    }

    public Dataset getDatasetByHarvestInfo(Dataverse dataverse, String harvestIdentifier) {
        String queryStr = "SELECT d FROM Dataset d, DvObject o WHERE d.id = o.id AND o.owner.id = " 
                + dataverse.getId() + " and d.harvestIdentifier = '" + harvestIdentifier + "'";
        List<Dataset> list = em.createQuery(queryStr, Dataset.class).getResultList();
        if (list.size() > 1) {
            throw new EJBException("More than one dataset found in the dataverse (id= " 
                    + dataverse.getId() + "), with harvestIdentifier= " + harvestIdentifier);
        }
        return list.size() == 1 ? list.get(0) : null;
    }

    public void updateLastExportTimeStamp(Long datasetId) {
        Date now = new Date();
        em.createNativeQuery("UPDATE Dataset SET lastExportTime='" + now.toString() + "' WHERE id=" + datasetId).executeUpdate();
    }

    public Dataset setNonDatasetFileAsThumbnail(Dataset dataset, InputStream inputStream) {
        if (dataset == null) {
            return null;
        }
        if (inputStream == null) {
            return null;
        }
        dataset = datasetThumbnailService.persistDatasetLogoToStorageAndCreateThumbnail(dataset, inputStream);
        dataset.setThumbnailFile(null);
        dataset.setUseGenericThumbnail(false);
        return merge(dataset);
    }

    public Dataset setDatasetFileAsThumbnail(Dataset dataset, DataFile datasetFileThumbnailToSwitchTo) {
        if (dataset == null) {
            return null;
        }
        if (datasetFileThumbnailToSwitchTo == null) {
            return null;
        }
        datasetThumbnailService.deleteDatasetLogo(dataset);
        dataset.setThumbnailFile(datasetFileThumbnailToSwitchTo);
        dataset.setUseGenericThumbnail(false);
        return merge(dataset);
    }

    public Dataset removeDatasetThumbnail(Dataset dataset) {
        if (dataset == null) {
            return null;
        }
        datasetThumbnailService.deleteDatasetLogo(dataset);
        dataset.setThumbnailFile(null);
        dataset.setUseGenericThumbnail(true);
        return merge(dataset);
    }

    public void assignDatasetThumbnailByNativeQuery(Dataset dataset, DataFile dataFile) {
        this.datasetRepository.assignThumbnail(dataset.getId(), dataFile.getId());
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
            logger.warn("Failed to sleep for 15 seconds.");
        }
        Dataset theDataset = find(datasetId);
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
