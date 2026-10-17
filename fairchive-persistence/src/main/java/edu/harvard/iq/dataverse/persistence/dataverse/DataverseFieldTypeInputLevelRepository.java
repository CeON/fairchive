package edu.harvard.iq.dataverse.persistence.dataverse;

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Optional;

import javax.ejb.Stateless;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

@Stateless
public class DataverseFieldTypeInputLevelRepository extends JpaRepository<Long, DataverseFieldTypeInputLevel> {

    public DataverseFieldTypeInputLevelRepository() {
        super(DataverseFieldTypeInputLevel.class);
    }
    
	public List<DataverseFieldTypeInputLevel> findByDataverseId(final Long id) {
		return createQuery(
				"select f from DataverseFieldTypeInputLevel f where f.dataverse.id = :id")
				.setParameter("id", id)
				.getResultList();

	}
	
    public List<DataverseFieldTypeInputLevel> findByDataverseIdAndDatasetFieldTypeIdList(
    		final Long id, final List<Long> fieldIdList) {      
        if (fieldIdList.isEmpty()) {
            return emptyList();
        } else {
            return createQuery(
            		"select f from DataverseFieldTypeInputLevel f where" + 
            		" f.dataverse.id = :id and f.datasetFieldType.id in :fieldIdList")
                    .setParameter("id", id)
                    .setParameter("fieldIdList", fieldIdList)
                    .getResultList();

        } 
    }
    public Optional<DataverseFieldTypeInputLevel> findByDataverseIdDatasetFieldTypeId(
    		final Long dataverseId, final Long fieldTypeId) {
        return getSingleResult(createQuery(
        		"select f from DataverseFieldTypeInputLevel f" + 
        		" where f.dataverse.id = :id and f.datasetFieldType.id = :fieldTypeId")
        		.setParameter("id", dataverseId)
        		.setParameter("fieldTypeId", fieldTypeId));
    }
    
    public void deleteFor(final Long dataverseId) {
        createQuery(
        		"DELETE FROM DataverseFieldTypeInputLevel f WHERE f.dataverse.id=:id")
                .setParameter("id", dataverseId)
                .executeUpdate();
    }

}
