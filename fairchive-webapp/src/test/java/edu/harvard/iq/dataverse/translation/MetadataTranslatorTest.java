package edu.harvard.iq.dataverse.translation;

import static edu.harvard.iq.dataverse.persistence.dataset.FieldType.NONE;
import static edu.harvard.iq.dataverse.persistence.dataset.FieldType.PERIODO;
import static edu.harvard.iq.dataverse.persistence.dataset.FieldType.TEXT;
import static edu.harvard.iq.dataverse.persistence.dataset.FieldType.TEXTBOX;
import static edu.harvard.iq.dataverse.persistence.dataset.FieldType.URL;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldsOfType;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.translation.Translator.Format;

@ExtendWith(MockitoExtension.class)
public class MetadataTranslatorTest {

    private static final String LANGUAGE = "pl";

    /**
     * Answers of the mocked translation service. A text missing here was not
     * meant to be sent, and fails the test.
     */
    private static final Map<String, String> POLISH = new HashMap<>();

    static {
        POLISH.put("Archives", "Archiwa");
        POLISH.put("Egypt", "Egipt");
        POLISH.put("Law", "Prawo");
        POLISH.put("Engineering", "Inżynieria");
        POLISH.put("Subject headings", "Hasła przedmiotowe");
        POLISH.put("R&D <archives>", "B+R <archiwa>");
        POLISH.put("<p>The <b>archives</b></p>", "<p><b>Archiwa</b></p>");
    }

    @Mock
    private Translator translator;

    @Captor
    private ArgumentCaptor<List<String>> sentTexts;

    private MetadataTranslator metadataTranslator;

    private long nextId = 1;

    @BeforeEach
    public void setUp() {
        this.metadataTranslator = new MetadataTranslator(this.translator);
    }

    // -------------------- TESTS --------------------

    @Test
    public void isEnabled__translatorEnabled_returnsTrue() {
        // given
        when(this.translator.isEnabled()).thenReturn(true);

        // when
        boolean enabled = this.metadataTranslator.isEnabled();

        // then
        assertThat(enabled).isTrue();
    }

    @Test
    public void translate__compoundField_eachChildOnce() {
        // given
        DatasetFieldType value = type("keywordValue", TEXT);
        DatasetFieldType vocabulary = type("keywordVocabulary", TEXT);
        DatasetFieldType keyword = compoundType("keyword", value, vocabulary);
        DatasetField field = compound(keyword, primitive(value, "Archives"), primitive(vocabulary, "Subject headings"));
        Map<MetadataBlock, List<DatasetFieldsOfType>> blocks = blocks(keyword, field);
        mockTranslateToPolish();

        // when
        Map<MetadataBlock, List<DatasetFieldsOfType>> translated = this.metadataTranslator.translate(blocks, LANGUAGE);

        // then
        assertThat(singleField(translated).getDatasetFieldsChildren())
                .extracting(child -> child.getFieldValue().get())
                .containsExactly("Archiwa", "Hasła przedmiotowe");
    }

    @Test
    public void translate__displayFormat_sendsRawValue() {
        // given
        DatasetFieldType title = type("title", TEXT);
        title.setDisplayFormat("– <b>#VALUE</b>");
        Map<MetadataBlock, List<DatasetFieldsOfType>> blocks = blocks(title, primitive(title, "Archives"));
        mockTranslateToPolish();

        // when
        this.metadataTranslator.translate(blocks, LANGUAGE);

        // then
        verify(this.translator).translate(this.sentTexts.capture(), eq(LANGUAGE), eq(Format.TEXT));
        assertThat(this.sentTexts.getValue()).containsExactly("Archives");
    }

    @Test
    public void translate__nonTextFields_notSent() {
        // given
        DatasetFieldType period = type("period", PERIODO);
        DatasetFieldType link = type("link", URL);
        DatasetFieldType title = type("title", TEXT);
        Map<MetadataBlock, List<DatasetFieldsOfType>> blocks = blocks(
                fieldsOf(period, primitive(period, "periodo period")),
                fieldsOf(link, primitive(link, "see the site")),
                fieldsOf(title, primitive(title, "Archives")));
        mockTranslateToPolish();

        // when
        this.metadataTranslator.translate(blocks, LANGUAGE);

        // then
        verify(this.translator).translate(this.sentTexts.capture(), eq(LANGUAGE), eq(Format.TEXT));
        assertThat(this.sentTexts.getValue()).containsExactly("Archives");
    }

    @Test
    public void translate__controlledVocabulary_allValuesJoined() {
        // given
        DatasetFieldType subject = type("subject", TEXT);
        ControlledVocabularyValue law = new ControlledVocabularyValue(1L, "Law", subject);
        ControlledVocabularyValue engineering = new ControlledVocabularyValue(2L, "Engineering", subject);
        Map<MetadataBlock, List<DatasetFieldsOfType>> blocks = blocks(subject, vocabularyField(subject, law, engineering));
        mockTranslateToPolish();

        // when
        Map<MetadataBlock, List<DatasetFieldsOfType>> translated = this.metadataTranslator.translate(blocks, LANGUAGE);

        // then
        assertThat(singleField(translated).getFieldValue().get()).isEqualTo("Prawo; Inżynieria");
    }

    @Test
    public void translate__controlledVocabulary_identifierLikeValueKept() {
        // given
        DatasetFieldType subject = type("subject", TEXT);
        ControlledVocabularyValue law = new ControlledVocabularyValue(1L, "Law", subject);
        ControlledVocabularyValue radiocarbon = new ControlledVocabularyValue(2L, "C14", subject);
        Map<MetadataBlock, List<DatasetFieldsOfType>> blocks = blocks(subject, vocabularyField(subject, law, radiocarbon));
        mockTranslateToPolish();

        // when
        Map<MetadataBlock, List<DatasetFieldsOfType>> translated = this.metadataTranslator.translate(blocks, LANGUAGE);

        // then
        assertThat(singleField(translated).getFieldValue().get()).isEqualTo("Prawo; C14");
    }

    @Test
    public void translate__identifierLikeValues_keptUntranslated() {
        // given
        DatasetFieldType keyword = type("keywordValue", TEXT);
        Map<MetadataBlock, List<DatasetFieldsOfType>> blocks = blocks(keyword,
                primitive(keyword, "0000-0001-9572-9029"),
                primitive(keyword, "https://ror.org/00cm8fy76"),
                primitive(keyword, "COVID-19"),
                primitive(keyword, "Archives"));
        mockTranslateToPolish();

        // when
        Map<MetadataBlock, List<DatasetFieldsOfType>> translated = this.metadataTranslator.translate(blocks, LANGUAGE);

        // then
        assertThat(fieldsOf(translated))
                .extracting(field -> field.getFieldValue().get())
                .containsExactly("0000-0001-9572-9029", "https://ror.org/00cm8fy76", "COVID-19", "Archiwa");
    }

    @Test
    public void translate__repeatedTexts_sentOnceInOneRequest() {
        // given
        DatasetFieldType title = type("title", TEXT);
        DatasetFieldType keyword = type("keywordValue", TEXT);
        Map<MetadataBlock, List<DatasetFieldsOfType>> blocks = blocks(
                fieldsOf(title, primitive(title, "Archives")),
                fieldsOf(keyword, primitive(keyword, "Archives"), primitive(keyword, "Egypt")));
        mockTranslateToPolish();

        // when
        this.metadataTranslator.translate(blocks, LANGUAGE);

        // then
        verify(this.translator).translate(this.sentTexts.capture(), eq(LANGUAGE), eq(Format.TEXT));
        assertThat(this.sentTexts.getValue()).containsExactly("Archives", "Egypt");
    }

    @Test
    public void translate__textAndTextbox_sentAsTextAndHtml() {
        // given
        DatasetFieldType title = type("title", TEXT);
        DatasetFieldType description = type("dsDescriptionValue", TEXTBOX);
        Map<MetadataBlock, List<DatasetFieldsOfType>> blocks = blocks(
                fieldsOf(title, primitive(title, "R&D <archives>")),
                fieldsOf(description, primitive(description, "<p>The <b>archives</b></p>")));
        mockTranslateToPolish();

        // when
        Map<MetadataBlock, List<DatasetFieldsOfType>> translated = this.metadataTranslator.translate(blocks, LANGUAGE);

        // then
        verify(this.translator).translate(singletonList("R&D <archives>"), LANGUAGE, Format.TEXT);
        verify(this.translator).translate(singletonList("<p>The <b>archives</b></p>"), LANGUAGE, Format.HTML);
        assertThat(fieldsOf(translated))
                .extracting(field -> field.getFieldValue().get())
                .containsExactly("B+R <archiwa>", "<p><b>Archiwa</b></p>");
    }

    @Test
    public void translate__translatedCopy_sourceFieldsUntouched() {
        // given
        DatasetFieldType title = type("title", TEXT);
        DatasetField field = primitive(title, "Archives");
        Map<MetadataBlock, List<DatasetFieldsOfType>> blocks = blocks(title, field);
        mockTranslateToPolish();

        // when
        this.metadataTranslator.translate(blocks, LANGUAGE);

        // then
        assertThat(field.getFieldValue().get()).isEqualTo("Archives");
    }

    @Test
    public void translate__manyBlocks_blockOrderKept() {
        // given
        DatasetFieldType title = type("title", TEXT);
        Map<MetadataBlock, List<DatasetFieldsOfType>> blocks = new LinkedHashMap<>();
        for (long id = 10; id > 0; id--) {
            blocks.put(block(id), singletonList(fieldsOf(title, primitive(title, "Archives"))));
        }
        mockTranslateToPolish();

        // when
        Map<MetadataBlock, List<DatasetFieldsOfType>> translated = this.metadataTranslator.translate(blocks, LANGUAGE);

        // then
        assertThat(translated.keySet()).containsExactlyElementsOf(blocks.keySet());
    }

    @Test
    public void translate__untranslatedText_keepsOriginal() {
        // given
        DatasetFieldType title = type("title", TEXT);
        Map<MetadataBlock, List<DatasetFieldsOfType>> blocks = blocks(title, primitive(title, "Archives"));
        when(this.translator.translate(anyList(), eq(LANGUAGE), any())).thenReturn(emptyList());

        // when
        Map<MetadataBlock, List<DatasetFieldsOfType>> translated = this.metadataTranslator.translate(blocks, LANGUAGE);

        // then
        assertThat(singleField(translated).getFieldValue().get()).isEqualTo("Archives");
    }

    @Test
    public void translate__serviceFailure_propagates() {
        // given
        DatasetFieldType title = type("title", TEXT);
        UncheckedIOException failure = new UncheckedIOException(new IOException("service down"));
        when(this.translator.translate(anyList(), eq(LANGUAGE), any())).thenThrow(failure);
        Map<MetadataBlock, List<DatasetFieldsOfType>> blocks = blocks(title, primitive(title, "Archives"));

        // when & then
        assertThatThrownBy(() -> this.metadataTranslator.translate(blocks, LANGUAGE)).isSameAs(failure);
    }

    // -------------------- PRIVATE --------------------

    private void mockTranslateToPolish() {
        when(this.translator.translate(anyList(), eq(LANGUAGE), any())).thenAnswer(invocation ->
                invocation.<List<String>>getArgument(0).stream()
                        .map(text -> Optional.ofNullable(POLISH.get(text))
                                .orElseThrow(() -> new AssertionError("Not expected to be translated: " + text)))
                        .collect(toList()));
    }

    private DatasetFieldType type(final String name, final FieldType fieldType) {
        DatasetFieldType type = new DatasetFieldType();
        type.setId(this.nextId++);
        type.setName(name);
        type.setFieldType(fieldType);
        return type;
    }

    private DatasetFieldType compoundType(final String name, final DatasetFieldType... children) {
        DatasetFieldType type = type(name, NONE);
        type.setChildDatasetFieldTypes(asList(children));
        for (DatasetFieldType child : children) {
            child.setParentDatasetFieldType(type);
        }
        return type;
    }

    private static DatasetField primitive(final DatasetFieldType type, final String value) {
        DatasetField field = new DatasetField();
        field.setDatasetFieldType(type);
        field.setFieldValue(value);
        return field;
    }

    private static DatasetField vocabularyField(final DatasetFieldType type, final ControlledVocabularyValue... values) {
        type.setControlledVocabularyValues(asList(values));
        DatasetField field = new DatasetField();
        field.setDatasetFieldType(type);
        field.setControlledVocabularyValues(asList(values));
        return field;
    }

    private static DatasetField compound(final DatasetFieldType type, final DatasetField... children) {
        DatasetField field = new DatasetField();
        field.setDatasetFieldType(type);
        for (DatasetField child : children) {
            child.setDatasetFieldParent(field);
            field.getDatasetFieldsChildren().add(child);
        }
        return field;
    }

    private static DatasetFieldsOfType fieldsOf(final DatasetFieldType type, final DatasetField... fields) {
        return new DatasetFieldsOfType(type, asList(fields));
    }

    private static MetadataBlock block(final long id) {
        MetadataBlock block = new MetadataBlock();
        block.setId(id);
        block.setName("block" + id);
        return block;
    }

    private static Map<MetadataBlock, List<DatasetFieldsOfType>> blocks(final DatasetFieldsOfType... fieldsOfTypes) {
        Map<MetadataBlock, List<DatasetFieldsOfType>> blocks = new LinkedHashMap<>();
        blocks.put(block(100L), asList(fieldsOfTypes));
        return blocks;
    }

    private static Map<MetadataBlock, List<DatasetFieldsOfType>> blocks(final DatasetFieldType type,
            final DatasetField... fields) {
        return blocks(fieldsOf(type, fields));
    }

    private static List<DatasetField> fieldsOf(final Map<MetadataBlock, List<DatasetFieldsOfType>> blocks) {
        return blocks.values().stream()
                .flatMap(List::stream)
                .flatMap(fieldsOfType -> fieldsOfType.getFields().stream())
                .collect(toList());
    }

    private static DatasetField singleField(final Map<MetadataBlock, List<DatasetFieldsOfType>> blocks) {
        List<DatasetField> fields = fieldsOf(blocks);
        assertThat(fields).hasSize(1);
        return fields.get(0);
    }
}
