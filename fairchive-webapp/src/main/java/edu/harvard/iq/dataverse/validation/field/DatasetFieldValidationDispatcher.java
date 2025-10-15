package edu.harvard.iq.dataverse.validation.field;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.ConditionalRendering;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.InputFieldRenderer;
import edu.harvard.iq.dataverse.dataset.metadata.inputRenderer.InputFieldRendererManager;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFieldTypeInputLevel;

import static edu.harvard.iq.dataverse.validation.field.FieldValidationResult.invalid;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DatasetFieldValidationDispatcher {
    private ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, List<DatasetField>> fieldIndex = emptyMap();
    private Map<String, List<ValidationDescriptor>> descriptorsCache = new HashMap<>();

    private FieldValidatorRegistry registry;
    private InputFieldRendererManager inputFieldRendererManager;

    // -------------------- CONSTRUCTORS --------------------

    DatasetFieldValidationDispatcher(final FieldValidatorRegistry registry, 
            final InputFieldRendererManager inputFieldRendererManager) {
        this.registry = registry;
        this.inputFieldRendererManager = inputFieldRendererManager;
    }

    // -------------------- LOGIC --------------------

    DatasetFieldValidationDispatcher init(final List<DatasetField> parentAndChildrenFields) {
        this.fieldIndex = parentAndChildrenFields.stream()
                .collect(groupingBy(DatasetField::getTypeName));
        return this;
    }

    public List<FieldValidationResult> executeValidations() {
        return this.fieldIndex.values().stream()
                .flatMap(Collection::stream)
                .filter(this::isNotTemplateField)
                .map(this::validateField)
                .filter(r -> !r.isOk())
                .collect(Collectors.toList());
    }

    // -------------------- PRIVATE --------------------

    private boolean isNotTemplateField(DatasetField field) {
        return field.getTopParentDatasetField().getTemplate() == null;
    }

    private FieldValidationResult validateField(DatasetField field) {
        DatasetFieldType fieldType = field.getDatasetFieldType();
        if (isBlank(field.getValue()) && fieldType.isPrimitive() 
                && isRequiredInDataverse(field)) {
            return invalid(field, "isrequired", fieldType.getDisplayName());
        }
        final boolean effectivelyEmptyValue = isBlank(field.getValue())
                || DatasetField.NA_VALUE.equals(field.getValue());
        for (final ValidationDescriptor descriptor : retrieveDescriptors(field)) {
            final Map<String, Object> parameters = descriptor.getParameters();
            @SuppressWarnings("unchecked") final List<String> contexts = 
                    (List<String>) parameters.get(ValidationDescriptor.CONTEXT_PARAM);
            final boolean properContext = contexts == null 
                    || contexts.contains(ValidationDescriptor.DATASET_CONTEXT);
            if (!properContext || 
                    (effectivelyEmptyValue 
                            && !parameters.containsKey(ValidationDescriptor.RUN_ON_EMPTY_PARAM))) {
                continue;
            }
            final FieldValidator validator = registry.getOrThrow(descriptor.getName());
            final FieldValidationResult result = validator.validate(field, parameters, fieldIndex);
            if (!result.isOk()) {
                return result;
            }
        }
        return FieldValidationResult.ok();
    }

    private  boolean isRequiredInDataverse(final DatasetField field) {
        final DatasetFieldType fieldType = field.getDatasetFieldType();
        if (fieldType.isRequired()) {
            return isFieldRendered(field);
        }

        final Dataverse dataverse = field.getTopParentDatasetField()
                .getDatasetVersion()
                .getDataset()
                .getOwner().getMetadataBlockRootDataverse();
        final boolean inputLevelRequired =  dataverse.getDataverseFieldTypeInputLevels().stream()
                .filter(inputLevel -> inputLevel.getDatasetFieldType().equals(field.getDatasetFieldType()))
                .map(DataverseFieldTypeInputLevel::isRequired)
                .findFirst()
                .orElse(false);

        return inputLevelRequired && isFieldRendered(field);
    }

    private boolean isFieldRendered(DatasetField field) {
        final InputFieldRenderer renderer = this.inputFieldRendererManager.
                obtainRenderer(field.getDatasetFieldType());
        if (renderer != null && renderer.getConditionalRendering().isDefined()) {
            final ConditionalRendering conditionalRendering = renderer.getConditionalRendering().get();
            final List<DatasetField> subfields = field.getParent()
                    .map(DatasetField::getDatasetFieldsChildren)
                    .getOrElse(Collections.emptyList());
            return conditionalRendering.shouldRender(subfields);
        }

        // default case no conditional rendering
        return true;
    }

    private List<ValidationDescriptor> retrieveDescriptors(final DatasetField field) {
        final String configJson = field.getDatasetFieldType().getValidation();
        final List<ValidationDescriptor> existing = descriptorsCache.get(configJson);
        if (existing != null) {
            return existing;
        }
        try {
            final List<ValidationDescriptor> descriptors = objectMapper.readValue(configJson,
                    objectMapper.getTypeFactory().
                    constructCollectionType(List.class, ValidationDescriptor.class));
            descriptorsCache.put(configJson, descriptors);
            return descriptors;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
