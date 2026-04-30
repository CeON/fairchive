package edu.harvard.iq.dataverse.persistence.dataset;

import java.util.List;
import java.util.Optional;

import javax.ejb.Singleton;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

@Singleton
public class DatasetVersionRepository extends JpaRepository<Long, DatasetVersion> {

    // -------------------- CONSTRUCTORS --------------------

    public DatasetVersionRepository() {
        super(DatasetVersion.class);
    }

    // -------------------- LOGIC --------------------

    public Optional<DatasetVersion> findByDatasetIdAndVersionNumber(
            final long datasetId, final long majorVersionNumber) {
        return getSingleResult(createQuery(
                "select v from DatasetVersion v " +
                "where v.dataset.id = :datasetId " +
                "and v.versionNumber= :majorVersionNumber " +
                "order by v.minorVersionNumber desc")
                .setParameter("datasetId", datasetId)
                .setParameter("majorVersionNumber", majorVersionNumber)
                .setMaxResults(1));
    }

    public Optional<DatasetVersion> findByDatasetIdAndVersionNumber(
            final DatasetVersionIdentifier versionIdentifier) {
        return getSingleResult(createQuery(
                "select v from DatasetVersion v " +
                "where v.dataset.id = :datasetId " +
                "and v.versionNumber= :versionNumber " +
                "and v.minorVersionNumber= :minorVersionNumber")
                .setParameter("datasetId", versionIdentifier.getDatasetId())
                .setParameter("versionNumber", versionIdentifier.getVersionNumber())
                .setParameter("minorVersionNumber",
                        versionIdentifier.getMinorVersionNumber()));
    }
    
    public List<DatasetVersionUser> getDatasetVersionUsersByAuthenticatedUser(
            final long userId) {
        return this.em.createQuery(
                "SELECT u from DatasetVersionUser u where u.authenticatedUser.id = :id",
                DatasetVersionUser.class)
                .setParameter("id", userId)
                .getResultList();
    }
}
