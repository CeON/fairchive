package edu.harvard.iq.dataverse.persistence.dataset;

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Optional;

import javax.ejb.Singleton;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

@Singleton
public class DatasetFieldTypeRepository extends JpaRepository<Long, DatasetFieldType> {

    // -------------------- CONSTRUCTORS --------------------

    public DatasetFieldTypeRepository() {
        super(DatasetFieldType.class);
    }

    // -------------------- LOGIC --------------------

    public Optional<DatasetFieldType> findByName(String name) {
        return getSingleResult(createQuery(
                "select t from DatasetFieldType t where t.name= :name")
                .setParameter("name", name));
    }
    
    public List<DatasetFieldType> findAllFacetable() {
        return createQuery(
        		"select t from DatasetFieldType t " + 
        		"where t.facetable = true and t.title != '' " +
        		"order by t.id")
                .getResultList();
    }
    
    public List<DatasetFieldType> findFacetableByMetadataBlock(final Long blockId) {
        return createQuery(
        		"select t from DatasetFieldType t " +
        		"where t.facetable = true and t.title != '' and t.metadataBlock.id = :id  " +
        		"order by t.id")
                .setParameter("id", blockId)
                .getResultList();
    }
    
    public List<DatasetFieldType> findAllAdvancedSearchFieldTypesByMetadataBlockIds(final List<Long> blockIds) {
    	if(blockIds.isEmpty()) {
    		return emptyList();
    	} else {
	        return createQuery(
	        		"select t from DatasetFieldType t " + 
	        		"where t.advancedSearchFieldType = true and t.title != '' and t.metadataBlock.id in :ids " +
	        		"order by t.metadataBlock.id, t.displayOrder")
	                .setParameter("ids", blockIds)
	                .getResultList();
    	}
    }

    public List<DatasetFieldType> findAllRequired() {
        return createQuery(
        		"select t from DatasetFieldType t where t.required = true order by t.id")
        		.getResultList();
    }
    
    public List<DatasetFieldType> findAllOrderedById() {
        return createQuery(
        		"select t from DatasetFieldType t order by t.id")
        		.getResultList();
    }
    
    public List<DatasetFieldType> findAllOrderedByName() {
        return createQuery("select t from DatasetFieldType t order by t.name")
        		.getResultList();
    }
}
