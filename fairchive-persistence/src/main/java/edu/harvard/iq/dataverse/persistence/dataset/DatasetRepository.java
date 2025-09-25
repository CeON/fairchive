package edu.harvard.iq.dataverse.persistence.dataset;

import static java.lang.Math.max;
import static java.time.Instant.now;
import static javax.persistence.TemporalType.TIMESTAMP;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import javax.ejb.Singleton;
import javax.persistence.Query;
import javax.persistence.TemporalType;

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
    
    public List<Dataset> findAllOrderedById() {
        return this.em.createQuery("select object(o) from Dataset as o order by o.id",
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
    
    public boolean isIdentifierLocallyUnique(final String identifier, final Dataset dataset) {
        return this.em.createQuery(
                    "SELECT d FROM Dataset d " +
                    "WHERE d.identifier=:id AND d.protocol=:protocol AND d.authority=:authority")
                .setParameter("id", identifier)
                .setParameter("authority", dataset.getAuthority())
                .setParameter("protocol", dataset.getProtocol())
                .getResultList().isEmpty();
    }
    
    public void assignThumbnail(final Long datasetId, final Long datafileId) {
        try {
            this.em.createNativeQuery("UPDATE dataset SET thumbnailfile_id=" 
                                    + datasetId + " WHERE id=" + datasetId)
                .executeUpdate();
        } catch (final Exception e) {
            // it's ok to just ignore...
        }
    }
    
    public void updateAllLastChangeForExporterTime() {
        this.em.createQuery(
                "UPDATE Dataset ds SET ds.lastChangeForExporterTime=:date " +
                "WHERE ds.harvestedFrom IS NULL")
                .setParameter("date", new Date(), TIMESTAMP)
                .executeUpdate();
    }
    
}
