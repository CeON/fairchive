package edu.harvard.iq.dataverse;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFacet;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFacetRepository;
import edu.harvard.iq.dataverse.util.LruCache;

/**
 * @author xyang
 * @author Michael Bar-Sinai
 */
@SuppressWarnings("serial")
@Stateless
@Named
public class DataverseFacetServiceBean implements java.io.Serializable {

    public static final LruCache<Long, List<DataverseFacet>> cache = new LruCache<>();

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    
    @Inject
    private DataverseFacetRepository repository;

    @EJB
    DataverseDao dataverses;

    public List<DataverseFacet> findByDataverseId(Long dataverseId) {
        List<DataverseFacet> res = cache.get(dataverseId);

        if (res == null) {
            res = em.createNamedQuery("DataverseFacet.findByDataverseId", DataverseFacet.class)
                    .setParameter("dataverseId", dataverseId).getResultList();
            cache.put(dataverseId, res);
        }

        return res;
    }

    public void delete(DataverseFacet dataverseFacet) {
        em.remove(em.merge(dataverseFacet));
        cache.invalidate();
    }

    public void deleteFacetsFor(Dataverse d) {
        em.createNamedQuery("DataverseFacet.removeByOwnerId")
                .setParameter("ownerId", d.getId())
                .executeUpdate();
        cache.invalidate(d.getId());

    }

    public DataverseFacet create(final int displayOrder, final DatasetFieldType fieldType, 
    		final Dataverse dataverse) {
        final DataverseFacet facet = dataverse.addFacet(fieldType);
        facet.setDisplayOrder(displayOrder);
        em.persist(facet);
        return facet;
    }

    public DataverseFacet create(int displayOrder, Long datasetFieldTypeId, Long dataverseId) {
        return create(displayOrder, em.find(DatasetFieldType.class, datasetFieldTypeId),
                      dataverses.find(dataverseId));
    }
    
    public void save(final DataverseFacet facet) {
    	this.repository.save(facet);
    }

}

