package edu.harvard.iq.dataverse.persistence.dataset;

import java.util.List;

import javax.ejb.Singleton;

import edu.harvard.iq.dataverse.persistence.JpaRepository;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;

@Singleton
public class DatasetLockRepository extends JpaRepository<Long, DatasetLock> {

    // -------------------- CONSTRUCTORS --------------------

    public DatasetLockRepository() {
        super(DatasetLock.class);
    }

    // -------------------- LOGIC --------------------

    public List<DatasetLock> findByDatasetId(final long datasetId) {
        return this.em.createQuery(
                "select l from DatasetLock l where l.dataset.id = :id",
                DatasetLock.class)
                .setParameter("id", datasetId)
                .getResultList();
    }

    public List<DatasetLock> findByUser(final AuthenticatedUser user) {
        return this.em.createQuery(
                "SELECT lock FROM DatasetLock lock WHERE lock.user.id=:id",
                DatasetLock.class)
                .setParameter("id", user.getId())
                .getResultList();
    }
}
