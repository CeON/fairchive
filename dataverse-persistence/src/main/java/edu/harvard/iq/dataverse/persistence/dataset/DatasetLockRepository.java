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
        return executeQueryWithId("select l from DatasetLock l where l.dataset.id = :id", datasetId);
    }

    public List<DatasetLock> findByUser(final AuthenticatedUser user) {      
        return executeQueryWithId("SELECT lock FROM DatasetLock lock WHERE lock.user.id=:id", user.getId());
    }
    
    private List<DatasetLock> executeQueryWithId(final String query, final Long id) {
        return this.em.createQuery(query, DatasetLock.class)
                .setParameter("id", id)
                .getResultList();
    }
}
