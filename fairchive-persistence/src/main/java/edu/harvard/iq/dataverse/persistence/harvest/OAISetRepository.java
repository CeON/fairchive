package edu.harvard.iq.dataverse.persistence.harvest;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Stateless;

import java.util.List;
import java.util.Optional;

@Stateless
public class OAISetRepository extends JpaRepository<Long, OAISet> {

    // -------------------- CONSTRUCTORS --------------------

    public OAISetRepository() {
        super(OAISet.class);
    }

    // -------------------- LOGIC --------------------

    public Optional<OAISet> findBySpecName(final String name) {
        return getSingleResult(createQuery(
        		"SELECT o FROM OAISet o WHERE o.spec = :name")
                .setParameter("name", name));
    }

    public List<OAISet> findAllBySpecNameNot(final String name) {
        return createQuery(
        		"SELECT o FROM OAISet o WHERE o.spec != :name ORDER BY o.spec")
                .setParameter("name", name)
                .getResultList();
    }
}
