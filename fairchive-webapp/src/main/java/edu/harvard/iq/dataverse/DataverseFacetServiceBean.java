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

    public List<DataverseFacet> findByDataverseId(final Long id) {
        List<DataverseFacet> res = cache.get(id);
        
        if (res == null) {
            res = this.repository.findByDataverseId(id);
            cache.put(id, res);
        }

        return res;
    }

    public void delete(final DataverseFacet facet) {
    	this.repository.delete(this.repository.save(facet));
        cache.invalidate();
    }

    public void deleteFacetsFor(final Dataverse dataverse) {
        this.repository.deleteFor(dataverse.getId());
        cache.invalidate(dataverse.getId());
    }

    public DataverseFacet create(final int displayOrder, final DatasetFieldType fieldType, 
    		final Dataverse dataverse) {
        final DataverseFacet facet = dataverse.addFacet(fieldType);
        facet.setDisplayOrder(displayOrder);
        this.repository.save(facet);
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

