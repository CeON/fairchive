package edu.harvard.iq.dataverse.persistence.dataset;

import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Singleton;
import javax.persistence.StoredProcedureQuery;
import javax.persistence.TypedQuery;

import static java.lang.Math.max;
import static java.util.stream.Collectors.toList;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Singleton
public class DatasetRepository extends JpaRepository<Long, Dataset> {

    // -------------------- CONSTRUCTORS --------------------

    public DatasetRepository() {
        super(Dataset.class);
    }

    // -------------------- LOGIC --------------------

    public List<Dataset> findByOwnerId(Long ownerId) {
        return this.em
                .createQuery("SELECT o FROM Dataset o WHERE o.owner.id=:ownerId",
                        Dataset.class)
                .setParameter("ownerId", ownerId)
                .getResultList();
    }

    public List<Long> findIdsByOwnerId(Long ownerId) {
        return this.em
                .createQuery("SELECT o.id FROM Dataset o WHERE o.owner.id=:ownerId",
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
        return this.em.createQuery("SELECT o FROM Dataset o WHERE o.dtype = 'Dataset'"
                + " AND o.identifierRegistered = false AND o.harvestedFrom IS NULL ",
                Dataset.class)
                .getResultList();
    }

    public List<Dataset> findStaleOrMissingDatasets() {
        return findAll().stream().filter(DvObject::isStale).collect(toList());
    }

    public List<Dataset> findNotIndexedAfterEmbargo() {
        return this.em.createQuery("select d from Dataset d, DvObject o " +
                "where d.id = o.id and d.embargoDate < :actualTimestamp and d.embargoDate > o.indexTime",
                Dataset.class)
                .setParameter("actualTimestamp", Timestamp.from(Instant.now()))
                .getResultList();
    }

    public List<Long> findAllLocalDatasetIds() {
        return this.em.createQuery(
                "SELECT o.id FROM Dataset o WHERE o.harvestedFrom IS null ORDER BY o.id",
                Long.class)
                .getResultList();
    }

    public List<Long> findAllUnindexed() {
        return this.em.createQuery(
                "SELECT o.id FROM Dataset o WHERE o.indexTime IS null ORDER BY o.id DESC",
                Long.class)
                .getResultList();
    }
    
    public List<Long> findAllOrSubset(final long numPartitions, final long partitionId,
            final boolean skipIndexed) {
        return skipIndexed
                ? findAllOrSubsetSkippingIndexed(numPartitions, partitionId)
                : findAllOrSubset(numPartitions, partitionId);
    }

    public List<Long> findAllOrSubset(final long numPartitions,
            final long partitionId) {
        return this.em.createQuery("SELECT o.id FROM Dataset o " +
                "WHERE MOD(o.id, :numPartitions) = :partitionId ORDER BY o.id",
                Long.class)
                .setParameter("numPartitions", max(numPartitions, 1))
                .setParameter("partitionId", partitionId)
                .getResultList();
    }

    public List<Long> findAllOrSubsetSkippingIndexed(final long numPartitions,
            final long partitionId) {
        return this.em.createQuery("SELECT o.id FROM Dataset o " +
                "WHERE MOD(o.id, :numPartitions) = :partitionId AND o.indexTime is null ORDER BY o.id",
                Long.class)
                .setParameter("numPartitions", max(numPartitions, 1))
                .setParameter("partitionId", partitionId)
                .getResultList();
    }
    
    public List<Dataset> findByIdentifierAuthorityAndProtocol(final String identifier,
            final String authority, final String protocol) {
        return this.em.createQuery("SELECT d FROM Dataset d "
                + "WHERE d.identifier = :identifier AND d.protocol = :protocol AND d.authority = :authority",
                Dataset.class)
                .setParameter("identifier", identifier)
                .setParameter("authority", authority)
                .setParameter("protocol", protocol)
                .getResultList();
    }
    
    public List<Dataset> findByOwnerIdAndHarvestIdentifier(final Long ownerId,
            final String harvestIdentifier) {
        return this.em.createQuery("SELECT d FROM Dataset d, DvObject o " +
                "WHERE d.id = o.id AND o.owner.id = :ownerId and d.harvestIdentifier = :harvestIdentifier",
                Dataset.class)
                .setParameter("ownerId", ownerId)
                .setParameter("harvestIdentifier", harvestIdentifier)
                .getResultList();
    }
    
    public String generateIdentifierAsSequentialNumber() {
        final StoredProcedureQuery query = this.em.createNamedStoredProcedureQuery(
                "Dataset.generateIdentifierAsSequentialNumber");
        query.execute();
        return query.getOutputParameterValue(1).toString();
    }
}
