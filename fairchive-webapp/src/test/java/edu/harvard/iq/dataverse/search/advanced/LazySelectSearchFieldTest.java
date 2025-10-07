package edu.harvard.iq.dataverse.search.advanced;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.harvard.iq.dataverse.persistence.dataset.ControlledVocabularyValue;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.search.advanced.field.LazySelectSearchField;
import edu.harvard.iq.dataverse.search.advanced.query.QueryPart;
import edu.harvard.iq.dataverse.search.advanced.query.QueryPartType;

public class LazySelectSearchFieldTest {

    private LazySelectSearchField searchField;


    @BeforeEach
    void beforeEach() {
        DatasetFieldType fieldType = new DatasetFieldType();
        fieldType.setName("title");
        List<ControlledVocabularyValue> controlledVocabularyValues = new ArrayList<>();
        controlledVocabularyValues.add(new ControlledVocabularyValue(1L, "abcdef", fieldType));
        controlledVocabularyValues.add(new ControlledVocabularyValue(2L, "abCDef", fieldType));
        controlledVocabularyValues.add(new ControlledVocabularyValue(3L, "abc def", fieldType));
        controlledVocabularyValues.add(new ControlledVocabularyValue(4L, "abc a def", fieldType));
        controlledVocabularyValues.add(new ControlledVocabularyValue(5L, "abdef", fieldType));
        fieldType.setControlledVocabularyValues(controlledVocabularyValues);

        searchField = new LazySelectSearchField(fieldType, true);
    }

    @Test
    void getQueryPart() {
        // given
        List<String> selected = new ArrayList<>();
        selected.add("abcdef");
        selected.add("abCDef");
        searchField.setSelected(selected);

        // when
        QueryPart queryPart = searchField.getQueryPart();

        // then
        assertThat(queryPart.queryPartType).isEqualTo(QueryPartType.QUERY);
        assertThat(queryPart.queryFragment).isEqualTo("title:\"abcdef\" AND title:\"abCDef\"");
    }

    @Test
    void complete() {
        // when
        List<String> suggestions = searchField.complete("abcd");
        // then
        assertThat(suggestions).containsExactly("abcdef", "abCDef");
    }

    @Test
    void complete__one_already_selected() {
        // given
        searchField.setSelected(Collections.singletonList("abcdef"));
        // when
        List<String> suggestions = searchField.complete("abcd");
        // then
        assertThat(suggestions).containsExactly("abCDef");
    }

    @Test
    void complete__query_with_uppercase_characters() {
        // when
        List<String> suggestions = searchField.complete("aBcD");
        // then
        assertThat(suggestions).containsExactly("abcdef", "abCDef");
    }

    @Test
    void complete__find_starting_from_the_middle_of_the_word() {
        // when
        List<String> suggestions = searchField.complete("bcde");
        // then
        assertThat(suggestions).containsExactly("abcdef", "abCDef");
    }

    @Test
    void complete__query_with_space() {
        // when
        List<String> suggestions = searchField.complete("abc d");
        // then
        assertThat(suggestions).containsExactly("abc def");
    }
}
