package edu.harvard.iq.dataverse;

import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabAlternate;
import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
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

    public DatasetFieldType find(Object pk) {
        return em.find(DatasetFieldType.class, pk);
    }

    public DatasetFieldType findByName(String name) {
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
    public DatasetFieldType findByNameOpt(String name) {
    	return this.fieldTypeRepository.findByName(name).orElse(null);
    }

    public ControlledVocabularyValue findControlledVocabularyValueByIdentifier(Object pk) {
        return em.find(ControlledVocabularyValue.class, pk);
    }

    /**
     * @param dsft     The DatasetFieldType in which to look up a
     *                 ControlledVocabularyValue.
     * @param strValue String value that may exist in a controlled vocabulary of
     *                 the provided DatasetFieldType.
     * @param lenient  should we accept alternate spellings for value from mapping table
     * @return The ControlledVocabularyValue found or null.
     */
    public ControlledVocabularyValue findControlledVocabularyValueByDatasetFieldTypeAndStrValue(DatasetFieldType dsft, String strValue, boolean lenient) {
        TypedQuery<ControlledVocabularyValue> typedQuery = em.createQuery("SELECT OBJECT(o) FROM ControlledVocabularyValue AS o WHERE o.strValue = :strvalue AND o.datasetFieldType = :dsft", ControlledVocabularyValue.class);
        typedQuery.setParameter("strvalue", strValue);
        typedQuery.setParameter("dsft", dsft);
        try {
            ControlledVocabularyValue cvv = typedQuery.getSingleResult();
            return cvv;
        } catch (NoResultException | NonUniqueResultException ex) {
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
    public ControlledVocabularyValue findControlledVocabularyValueByDatasetFieldTypeAndIdentifier(DatasetFieldType dsft, String identifier) {
        TypedQuery<ControlledVocabularyValue> typedQuery = em.createQuery("SELECT OBJECT(o) FROM ControlledVocabularyValue AS o WHERE o.identifier = :identifier AND o.datasetFieldType = :dsft", ControlledVocabularyValue.class);
        typedQuery.setParameter("identifier", identifier);
        typedQuery.setParameter("dsft", dsft);
        try {
            ControlledVocabularyValue cvv = typedQuery.getSingleResult();
            return cvv;
        } catch (NoResultException | NonUniqueResultException ex) {
            return null;
        }
    }

    // return singleton NA Controled Vocabulary Value
    public ControlledVocabularyValue findNAControlledVocabularyValue() {
        TypedQuery<ControlledVocabularyValue> typedQuery = em.createQuery("SELECT OBJECT(o) FROM ControlledVocabularyValue AS o WHERE o.datasetFieldType is null AND o.strValue = :strvalue", ControlledVocabularyValue.class);
        typedQuery.setParameter("strvalue", DatasetField.NA_VALUE);
        return typedQuery.getSingleResult();
    }

    public DatasetFieldType save(final DatasetFieldType type) {
        return this.fieldTypeRepository.save(type);
    }

    public MetadataBlock save(MetadataBlock mdb) {
        return em.merge(mdb);
    }

    public ControlledVocabularyValue save(ControlledVocabularyValue cvv) {
        return em.merge(cvv);
    }

    public ControlledVocabAlternate save(ControlledVocabAlternate alt) {
        return em.merge(alt);
    }

}
