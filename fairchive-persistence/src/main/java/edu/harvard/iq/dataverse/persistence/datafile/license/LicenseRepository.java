package edu.harvard.iq.dataverse.persistence.datafile.license;

import java.util.List;
import java.util.Optional;

import javax.ejb.Stateless;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

@Stateless
public class LicenseRepository extends JpaRepository<Long, License> {

    public LicenseRepository() {
        super(License.class);
    }

    // -------------------- LOGIC --------------------

    public License findFirstActive() {
        return createQuery(
                "SELECT l FROM License l WHERE l.active = true ORDER BY l.position ASC")
                .setMaxResults(1)
                .getSingleResult();
    }

    public Optional<License> findLicenseByName(String licenseName) {
        return JpaRepository.getSingleResult(
                createQuery("SELECT l FROM License l WHERE l.name=:licenseName")
                        .setParameter("licenseName", licenseName));
    }

    public List<License> findAllOrderedByPosition() {
        return createQuery("SELECT l FROM License l ORDER BY l.position ASC")
        		.getResultList();
    }

    public List<License> findActiveOrderedByPosition() {
        return createQuery(
                "SELECT l FROM License l WHERE l.active = true ORDER BY l.position ASC")
                .getResultList();
    }

    public Long countActiveLicenses() {
        return this.em.createQuery("SELECT count(l) FROM License l where l.active = true",
                Long.class).getSingleResult();
    }

    public Long countInactiveLicenses() {
        return this.em.createQuery(
                "SELECT count(l) FROM License l where l.active = false ", Long.class)
                .getSingleResult();
    }

    public Long findMaxLicensePosition() {
        return this.em.createQuery("SELECT MAX(l.position) FROM License l", Long.class)
                .getSingleResult();
    }
}
