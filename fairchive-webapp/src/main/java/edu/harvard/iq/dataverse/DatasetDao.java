package edu.harvard.iq.dataverse;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.slf4j.Logger;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.dataset.DatasetThumbnailService;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.impl.FinalizeDatasetPublicationCommand;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLock;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetLockRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersion;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersionRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetVersionUser;
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


    /**
     * Merges the passed dataset to the persistence context.
     *
     * @param ds the dataset whose new state we want to persist.
     * @return The managed entity representing {@code ds}.
     */
    private Dataset merge(Dataset ds) {
        return this.datasetRepository.save(ds);
        
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
    
    public Dataset removeDatasetThumbnail(Dataset dataset) {
        if (dataset == null) {
            return null;
        }
        datasetThumbnailService.deleteDatasetLogo(dataset);
        dataset.setThumbnailFile(null);
        dataset.setUseGenericThumbnail(true);
        return merge(dataset);
    }

    public WorkflowComment addWorkflowComment(WorkflowComment workflowComment) {
        em.persist(workflowComment);
        return workflowComment;
    }

    @Asynchronous
    public void callFinalizePublishCommandAsynchronously(Long datasetId, 
            CommandContext ctxt, DataverseRequest request, boolean isPidPrePublished)  {

        // Since we are calling the next command asynchronously anyway - sleep here
        // for a few seconds, just in case, to make sure the database update of
        // the dataset initiated by the PublishDatasetCommand has finished,
        // to avoid any concurrency/optimistic lock issues.
        try {
            Thread.sleep(15000);
        } catch (Exception ex) {
            logger.warn("Failed to sleep for 15 seconds.");
        }
        Dataset theDataset = this.datasetRepository.getById((datasetId));
        commandEngine.submit(new FinalizeDatasetPublicationCommand(theDataset, request, isPidPrePublished));
    }
    
}
