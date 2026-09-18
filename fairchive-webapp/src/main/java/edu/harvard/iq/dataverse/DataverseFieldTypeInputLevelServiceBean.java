package edu.harvard.iq.dataverse;

import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFieldTypeInputLevel;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFieldTypeInputLevelRepository;
import edu.harvard.iq.dataverse.util.LruCache;

@Stateless
public class DataverseFieldTypeInputLevelServiceBean {

    public static final LruCache<Long, List<DataverseFieldTypeInputLevel>> cache = new LruCache<>();
    
    private DataverseFieldTypeInputLevelRepository repository;
    
    public DataverseFieldTypeInputLevelServiceBean() {}

    @Inject
    public DataverseFieldTypeInputLevelServiceBean(
    		final DataverseFieldTypeInputLevelRepository repository) {
		this.repository = repository;
	}

	public List<DataverseFieldTypeInputLevel> findByDataverseId(final Long id) {
        List<DataverseFieldTypeInputLevel> res = cache.get(id);
        if (res == null) {
        	res = this.repository.findByDataverseId(id);
            cache.put(id, res);
        }
        return res;
    }

    public List<DataverseFieldTypeInputLevel> findByDataverseIdAndDatasetFieldTypeIdList(
    		final Long id, final List<Long> fieldIdList) {
    	return this.repository.findByDataverseIdAndDatasetFieldTypeIdList(
    			id, fieldIdList);
    }

    public DataverseFieldTypeInputLevel findByDataverseIdDatasetFieldTypeId(
    		final Long dataverseId, final Long fieldTypeId) {
    	return this.repository.findByDataverseIdDatasetFieldTypeId(
    			dataverseId, fieldTypeId).orElse(null);
    }

    public void deleteFacetsFor(final Dataverse dataverse) {
    	this.repository.deleteFacetsFor(dataverse.getId());
        cache.invalidate(dataverse.getId());
    }

    public void create(final DataverseFieldTypeInputLevel fieldTypeInputLevel) {
        this.repository.save(fieldTypeInputLevel);
    }
}
