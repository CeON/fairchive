package edu.harvard.iq.dataverse.translation;

import static edu.harvard.iq.dataverse.persistence.dataset.FieldType.TEXT;
import static edu.harvard.iq.dataverse.persistence.dataset.FieldType.TEXTBOX;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.containsWhitespace;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsOfType;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.translation.Translator.Format;

/**
 * Machine-translates the metadata of a dataset version for display.
 * <p>
 * Only free text goes to the translation service: values of primitive fields
 * of type {@link FieldType#TEXT} or {@link FieldType#TEXTBOX} (raw, before the
 * field's display format is applied, or the localized labels of their
 * controlled vocabulary values) not looking like an identifier or a link.
 * Everything else (dates, numbers, URLs, e-mails, PeriodO and GeoNames
 * references, ORCID or ROR identifiers) is shown as it is. Texts of
 * {@link FieldType#TEXTBOX} fields are sent as HTML, as they are displayed;
 * the others as plain text. Each kind goes to the service in a single request.
 */
@ApplicationScoped
public class MetadataTranslator {

    private static final List<FieldType> FREE_TEXT_TYPES = asList(TEXT, TEXTBOX);
    private static final String VALUE_SEPARATOR = "; ";

    private Translator translator;

    // -------------------- CONSTRUCTORS --------------------

    @Deprecated /* JEE requirement */
    MetadataTranslator() {
    }

    @Inject
    public MetadataTranslator(final Translator translator) {
        this.translator = translator;
    }

    // -------------------- LOGIC --------------------

    /**
     * Returns true if a translation service is configured.
     */
    public boolean isEnabled() {
        return this.translator.isEnabled();
    }

    /**
     * Returns a translated copy of the given metadata, leaving the given
     * fields untouched. Blocks and field types keep their order. A value the
     * service did not translate is shown in its original form.
     *
     * @param blocks metadata to translate, grouped by block and field type
     * @param languageCode code of the target language, e.g. "pl"
     * @throws UncheckedIOException if the translation service failed
     */
    public Map<MetadataBlock, List<DatasetFieldsOfType>> translate(
            final Map<MetadataBlock, List<DatasetFieldsOfType>> blocks, final String languageCode) {

        final Map<MetadataBlock, List<DatasetFieldsOfType>> result = copy(blocks);
        final List<DatasetField> translatable = result.values().stream()
                .flatMap(List::stream)
                .flatMap(fieldsOfType -> fieldsOfType.getFields().stream())
                .flatMap(MetadataTranslator::withDescendants)
                .filter(MetadataTranslator::isTranslatable)
                .collect(toList());
        final Map<Format, Map<String, String>> translations = Stream.of(Format.values())
                .collect(toMap(identity(), format -> translate(translatable, format, languageCode)));

        for (final DatasetField field : translatable) {
            final Map<String, String> translationsOfFormat = translations.get(formatOf(field));
            field.setValue(textsOf(field).stream()
                    .map(text -> translationsOfFormat.getOrDefault(text, text))
                    .collect(joining(VALUE_SEPARATOR)));
        }
        return result;
    }

    // -------------------- PRIVATE --------------------

    private Map<String, String> translate(final List<DatasetField> fields, final Format format,
            final String languageCode) {

        final List<String> sourceTexts = fields.stream()
                .filter(field -> formatOf(field) == format)
                .flatMap(field -> sourceTextsOf(field).stream())
                .distinct()
                .collect(toList());
        return sourceTexts.isEmpty()
                ? emptyMap()
                : pair(sourceTexts, this.translator.translate(sourceTexts, languageCode, format));
    }

    private static Map<MetadataBlock, List<DatasetFieldsOfType>> copy(
            final Map<MetadataBlock, List<DatasetFieldsOfType>> blocks) {

        final Map<MetadataBlock, List<DatasetFieldsOfType>> result = new LinkedHashMap<>();
        blocks.forEach((block, fieldsOfTypes) -> result.put(block, fieldsOfTypes.stream()
                .map(fieldsOfType -> new DatasetFieldsOfType(fieldsOfType.getType(),
                        fieldsOfType.getFields().stream().map(DatasetField::copy).collect(toList())))
                .collect(toList())));
        return result;
    }

    private static Stream<DatasetField> withDescendants(final DatasetField field) {
        return Stream.concat(Stream.of(field),
                field.getDatasetFieldsChildren().stream().flatMap(MetadataTranslator::withDescendants));
    }

    private static boolean isTranslatable(final DatasetField field) {
        final DatasetFieldType type = field.getDatasetFieldType();
        return type.isPrimitive() && FREE_TEXT_TYPES.contains(type.getFieldType())
                && !sourceTextsOf(field).isEmpty();
    }

    /**
     * Texts of the field to translate: those of {@link #textsOf} that are free text.
     */
    private static List<String> sourceTextsOf(final DatasetField field) {
        return textsOf(field).stream()
                .filter(MetadataTranslator::isFreeText)
                .collect(toList());
    }

    /**
     * Texts the field shows: the localized labels of its controlled vocabulary
     * values, or its raw value.
     */
    private static List<String> textsOf(final DatasetField field) {
        if (field.getDatasetFieldType().isControlledVocabulary()) {
            return field.getControlledVocabularyValues().stream()
                    .filter(Objects::nonNull)
                    .map(ControlledVocabularyValue::getLocaleStrValue)
                    .filter(Objects::nonNull)
                    .collect(toList());
        }
        return field.getFieldValue()
                .map(Collections::singletonList)
                .getOrElse(emptyList());
    }

    private static boolean isFreeText(final String value) {
        return isNotBlank(value) && !DatasetField.NA_VALUE.equals(value) && !isIdentifierLike(value);
    }

    /**
     * A link, or a single word containing a digit, as an ORCID or a grant
     * number. Machine translation corrupts these, e.g. by dropping a character.
     */
    private static boolean isIdentifierLike(final String value) {
        return value.contains("://")
                || (!containsWhitespace(value) && value.chars().anyMatch(Character::isDigit));
    }

    private static Format formatOf(final DatasetField field) {
        return field.getDatasetFieldType().getFieldType() == TEXTBOX ? Format.HTML : Format.TEXT;
    }

    private static Map<String, String> pair(final List<String> sourceTexts, final List<String> translatedTexts) {
        final Map<String, String> result = new HashMap<>();
        for (int i = 0; i < Math.min(sourceTexts.size(), translatedTexts.size()); i++) {
            result.put(sourceTexts.get(i), translatedTexts.get(i));
        }
        return result;
    }
}
