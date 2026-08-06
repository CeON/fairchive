package edu.harvard.iq.dataverse.persistence.group;

import java.util.List;

import javax.ejb.Singleton;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

@Singleton
public class SamlGroupRepository extends JpaRepository<Long, SamlGroup> {

    public SamlGroupRepository() {
        super(SamlGroup.class);
    }

    public List<SamlGroup> findByEntityId(final String id) {
        return createQuery("SELECT g FROM SamlGroup g WHERE g.entityId = :id")
                .setParameter("id", id)
                .getResultList();
    }
}
