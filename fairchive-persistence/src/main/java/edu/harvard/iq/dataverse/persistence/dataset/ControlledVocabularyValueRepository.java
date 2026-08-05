package edu.harvard.iq.dataverse.persistence.dataset;

import java.util.Optional;

import javax.ejb.Stateless;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

@Stateless
public class ControlledVocabularyValueRepository extends JpaRepository<Long, ControlledVocabularyValue> {

    public ControlledVocabularyValueRepository() {
        super(ControlledVocabularyValue.class);
    }

    public Optional<ControlledVocabularyValue> findByFieldTypeAndIdentifier(
    		final DatasetFieldType type, final String id) {
        return getSingleResult(createQuery(
        		"SELECT c FROM ControlledVocabularyValue c " + 
        		"WHERE c.identifier = :id AND c.datasetFieldType = :type")
        		.setParameter("id", id)
        		.setParameter("type", type));
    }
    
    public Optional<ControlledVocabularyValue> findByFieldTypeAndStrValue(
    		final DatasetFieldType type, final String strValue) {
        return getSingleResult(createQuery(
        		"SELECT c FROM ControlledVocabularyValue c " + 
        		"WHERE c.strValue = :value AND c.datasetFieldType = :type")
        .setParameter("value", strValue)
        .setParameter("type", type));
    }
}
