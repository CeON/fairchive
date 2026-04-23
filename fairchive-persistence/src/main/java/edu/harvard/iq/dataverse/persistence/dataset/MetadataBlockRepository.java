package edu.harvard.iq.dataverse.persistence.dataset;

import java.util.List;
import java.util.Optional;

import javax.ejb.Stateless;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

@Stateless
public class MetadataBlockRepository extends JpaRepository<Long, MetadataBlock> {

    public MetadataBlockRepository() {
        super(MetadataBlock.class);
    }

    public Optional<MetadataBlock> findByName(final String name) {
        return getSingleResult(createQuery(
                        "SELECT mdb FROM MetadataBlock mdb WHERE mdb.name=:name")
                .setParameter("name", name));
    }
    
    public List<MetadataBlock> findSystemMetadataBlocks() {
        return createQuery(
                "select object(o) from MetadataBlock as o where o.owner.id=null  order by o.displayOrder")
                .getResultList();
    }
}
