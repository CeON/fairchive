package edu.harvard.iq.dataverse.persistence.dataset;

import java.util.Optional;

import javax.ejb.Stateless;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

@Stateless
public class MetadataBlockRepository extends JpaRepository<Long, MetadataBlock> {

    public MetadataBlockRepository() {
        super(MetadataBlock.class);
    }

    public Optional<MetadataBlock> findByName(final String name) {
        return getSingleResult(this.em.createQuery(
                        "SELECT mdb FROM MetadataBlock mdb WHERE mdb.name=:name",
                        MetadataBlock.class)
                .setParameter("name", name));
    }
}
