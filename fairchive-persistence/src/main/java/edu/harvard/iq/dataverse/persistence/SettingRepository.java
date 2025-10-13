package edu.harvard.iq.dataverse.persistence;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * Database access object to {@link Setting} entity.
 *
 * @author madryk
 */
@Stateless
public class SettingRepository {

    @PersistenceContext
    private EntityManager em;

    // -------------------- LOGIC --------------------

    public Setting find(final String name) {
        return this.em.find(Setting.class, name);
    }

    public List<Setting> findAll() {
        return this.em.createQuery("SELECT s FROM Setting s", Setting.class)
                .getResultList();
    }

    public Setting save(final Setting setting) {
        return this.em.merge(setting);
    }

    public void delete(final String name) {
        this.em.createQuery("DELETE FROM Setting s WHERE s.name=:name")
                .setParameter("name", name)
                .executeUpdate();
    }
}
