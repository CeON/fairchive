package edu.harvard.iq.dataverse.persistence.dataset;

import static java.lang.Math.max;
import static java.time.Instant.now;

import java.sql.Timestamp;
import java.util.List;

import javax.ejb.Singleton;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

@Singleton
public class DatasetRepository extends JpaRepository<Long, Dataset> {

    // -------------------- CONSTRUCTORS --------------------

    public DatasetRepository() {
        super(Dataset.class);
    }

    // -------------------- LOGIC --------------------

    public List<Dataset> findByOwnerId(Long ownerId) {
        return this.em.createQuery(
                "SELECT o FROM Dataset o WHERE o.owner.id=:ownerId", 
                Dataset.class)
                .setParameter("ownerId", ownerId)
                .getResultList();
    }

    public List<Long> findIdsByOwnerId(Long ownerId) {
        return this.em.createQuery(
                "SELECT o.id FROM Dataset o WHERE o.owner.id=:ownerId", 
                Long.class)
                .setParameter("ownerId", ownerId)
                .getResultList();
    }

    public List<Long> findIdsByNullHarvestedFrom() {
        return this.em.createQuery(
                "SELECT o.id FROM Dataset o WHERE o.harvestedFrom IS null ORDER BY o.id", 
                Long.class)
                .getResultList();
    }

    public List<Dataset> findByNonRegisteredIdentifier() {
        return this.em.createQuery(
                "SELECT o FROM Dataset o WHERE o.dtype = 'Dataset'" +
                " AND o.identifierRegistered = false AND o.harvestedFrom IS NULL ", 
                Dataset.class)
          .getResultList();
    }
    
    public List<Long> findAllLocalDatasetIds() {
        return this.em.createQuery(
                "SELECT o.id FROM Dataset o WHERE o.harvestedFrom IS null ORDER BY o.id", 
                Long.class)
                .getResultList();
    }
    
    public List<Dataset> findNotIndexedAfterEmbargo() {
        return this.em.createQuery(
                "select d from Dataset d, DvObject o " + 
                "where d.id = o.id and d.embargoDate < :actualTimestamp and d.embargoDate > o.indexTime",
                Dataset.class)
                .setParameter("actualTimestamp", Timestamp.from(now()))
                .getResultList();
    }
    
    public List<Long> findAllOrSubset(final long numPartitions,
            final long partitionId) {
        return this.em.createQuery(
                "SELECT o.id FROM Dataset o " + 
                "WHERE MOD( o.id, :numPartitions) = :partitionId " +
                "ORDER BY o.id",
                Long.class)
                .setParameter("numPartitions", max(numPartitions, 1))
                .setParameter("partitionId", partitionId)
                .getResultList();
    }
    
    public List<Long> findAllOrSubsetSkippingIndexed(final long numPartitions,
            final long partitionId) {
        return this.em.createQuery(
                "SELECT o.id FROM Dataset o " + 
                "WHERE MOD( o.id, :numPartitions) = :partitionId AND o.indexTime is null " +
                "ORDER BY o.id",
                Long.class)
                .setParameter("numPartitions", max(numPartitions, 1))
                .setParameter("partitionId", partitionId)
                .getResultList();
    }
}
