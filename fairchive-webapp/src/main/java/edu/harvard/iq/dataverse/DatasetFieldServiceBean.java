package edu.harvard.iq.dataverse;

import java.util.List;
import java.util.Optional;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabAlternate;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValueRepository;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldTypeRepository;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;

/**
 * @author xyang
 */
@SuppressWarnings("serial")
@Stateless
public class DatasetFieldServiceBean implements java.io.Serializable {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;
    @Inject
    private DatasetFieldTypeRepository fieldTypeRepository;
    @Inject 
    private ControlledVocabularyValueRepository controlledVocabularyRepository;

    public List<DatasetFieldType> findAllAdvancedSearchFieldTypesByMetadataBlockIds(final List<Long> blockIds) {
        return this.fieldTypeRepository.findAllAdvancedSearchFieldTypesByMetadataBlockIds(blockIds);
    }

    public List<DatasetFieldType> findAllFacetableFieldTypes() {
        return this.fieldTypeRepository.findAllFacetable();
    }

    public List<DatasetFieldType> findFacetableFieldTypesByMetadataBlock(Long blockId) {
        return this.fieldTypeRepository.findFacetableByMetadataBlock(blockId);
    }

    public List<DatasetFieldType> findAllRequiredFields() {
        return this.fieldTypeRepository.findAllRequired();
    }

    public List<DatasetFieldType> findAllOrderedById() {
        return this.fieldTypeRepository.findAllOrderedById();
    }

    public List<DatasetFieldType> findAllOrderedByName() {
        return this.fieldTypeRepository.findAllOrderedByName();
    }

    public DatasetFieldType find(final Long id) {
        return this.fieldTypeRepository.findById(id).orElse(null);
    }

    public DatasetFieldType findByName(final String name) {
    	return this.fieldTypeRepository.findByName(name).orElse(null);
    }

    /**
     * Gets the dataset field type, or returns {@code null}. Does not throw
     * exceptions.
     *
     * @param name the name do the field type
     * @return the field type, or {@code null}
     * @see #findByName(java.lang.String)
     */
    public DatasetFieldType findByNameOpt(final String name) {
    	return this.fieldTypeRepository.findByName(name).orElse(null);
    }

    public ControlledVocabularyValue findControlledVocabularyValueByIdentifier(final Long id) {
        return this.controlledVocabularyRepository.findById(id).orElse(null);
    }

    /**
     * @param dsft     The DatasetFieldType in which to look up a
     *                 ControlledVocabularyValue.
     * @param strValue String value that may exist in a controlled vocabulary of
     *                 the provided DatasetFieldType.
     * @param lenient  should we accept alternate spellings for value from mapping table
     * @return The ControlledVocabularyValue found or null.
     */
    public ControlledVocabularyValue findControlledVocabularyValueByDatasetFieldTypeAndStrValue(
    		DatasetFieldType dsft, String strValue, boolean lenient) {
    	
    	final Optional<ControlledVocabularyValue> result = this.controlledVocabularyRepository.
    			findByFieldTypeAndStrValue(dsft, strValue);
    	
    	if(result.isPresent()) {
    		return result.get();
    	} else {
            if (lenient) {
                // if the value isn't found, check in the list of alternate values for this datasetFieldType
                TypedQuery<ControlledVocabAlternate> alternateQuery = em.createQuery("SELECT OBJECT(o) FROM ControlledVocabAlternate as o WHERE o.strValue = :strvalue AND o.datasetFieldType = :dsft", ControlledVocabAlternate.class);
                alternateQuery.setParameter("strvalue", strValue);
                alternateQuery.setParameter("dsft", dsft);
                try {
                    ControlledVocabAlternate alternateValue = alternateQuery.getSingleResult();
                    return alternateValue.getControlledVocabularyValue();
                } catch (NoResultException | NonUniqueResultException ex2) {
                    return null;
                }

            } else {
                return null;
            }
        }
    }

    public ControlledVocabAlternate findControlledVocabAlternateByControlledVocabularyValueAndStrValue(ControlledVocabularyValue cvv, String strValue) {
        TypedQuery<ControlledVocabAlternate> typedQuery = em.createQuery("SELECT OBJECT(o) FROM ControlledVocabAlternate AS o WHERE o.strValue = :strvalue AND o.controlledVocabularyValue = :cvv", ControlledVocabAlternate.class);
        typedQuery.setParameter("strvalue", strValue);
        typedQuery.setParameter("cvv", cvv);
        try {
            ControlledVocabAlternate alt = typedQuery.getSingleResult();
            return alt;
        } catch (NoResultException e) {
            return null;
        } catch (NonUniqueResultException ex) {
            List<ControlledVocabAlternate> results = typedQuery.getResultList();
            return results.get(0);
        }
    }

    /**
     * @param dsft       The DatasetFieldType in which to look up a
     *                   ControlledVocabularyValue.
     * @param identifier String Identifier that may exist in a controlled vocabulary of
     *                   the provided DatasetFieldType.
     * @return The ControlledVocabularyValue found or null.
     */
    public ControlledVocabularyValue findControlledVocabularyValueByDatasetFieldTypeAndIdentifier(
    		final DatasetFieldType type, final String id) {
        return this.controlledVocabularyRepository.findByFieldTypeAndIdentifier(type, id).orElse(null);
    }

    public DatasetFieldType save(final DatasetFieldType type) {
        return this.fieldTypeRepository.save(type);
    }

    public MetadataBlock save(MetadataBlock mdb) {
        return em.merge(mdb);
    }

    public ControlledVocabularyValue save(final ControlledVocabularyValue value) {
        return this.controlledVocabularyRepository.save(value);
    }

    public ControlledVocabAlternate save(ControlledVocabAlternate alt) {
        return em.merge(alt);
    }

}
