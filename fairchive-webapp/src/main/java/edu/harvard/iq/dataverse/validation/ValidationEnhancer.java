package edu.harvard.iq.dataverse.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.validation.field.FieldValidator;
import edu.harvard.iq.dataverse.validation.field.ValidationDescriptor;
import org.slf4j.Logger;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;

/**
 * The class is used to add validation info for fields that are not
 * DatasetFieldType-based (eg. some programmatically constructed search
 * fields).
 */
public class ValidationEnhancer {
    private static final Logger logger = getLogger(ValidationEnhancer.class);

    private final ObjectMapper mapper = new ObjectMapper();

    // -------------------- LOGIC --------------------

    public DatasetFieldType createDatasetFieldType(final String name, 
            final String displayName, final String desctiprtion, 
            final ValidationDescriptor validation) {
        
        final SimpleDatasetFieldType fieldType = new SimpleDatasetFieldType();
        fieldType.setName(name);
        fieldType.setDisplayName(displayName);
        fieldType.setDescription(desctiprtion);
        try {
            final String validationJson = 
                    this.mapper.writeValueAsString(singletonList(validation));
            fieldType.setValidation(validationJson);
        } catch (final JsonProcessingException jpe) {
            logger.warn("Cannot write validator as string", jpe);
        }
        return fieldType;
    }

    public ValidationDescriptor createValidation(final FieldValidator validator, 
            final Map<String, Object> params) {
        
        final ValidationDescriptor descriptor = new ValidationDescriptor();
        descriptor.setName(validator.getName());
        descriptor.getParameters().putAll(params);
        return descriptor;
    }

    // -------------------- INNER CLASSES --------------------

    @SuppressWarnings("serial")
    private final static class SimpleDatasetFieldType extends DatasetFieldType {
        private String displayName;

        public SimpleDatasetFieldType() {
            setControlledVocabularyValues(emptyList());
        }

        @Override
        public String getDisplayName() {
            return this.displayName;
        }

        public void setDisplayName(final String displayName) {
            this.displayName = displayName;
        }
    }
}
