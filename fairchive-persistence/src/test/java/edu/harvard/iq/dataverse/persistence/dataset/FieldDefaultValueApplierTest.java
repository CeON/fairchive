package edu.harvard.iq.dataverse.persistence.dataset;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

public class FieldDefaultValueApplierTest {

    private FieldDefaultValueApplier applier = new FieldDefaultValueApplier();

    @Test
    void applyDefaultValue() {

        // given
        DatasetField df = new DatasetField();
        DatasetFieldType dft = new DatasetFieldType();
        dft.setDefaultValue("abc");
        df.setDatasetFieldType(dft);

        // when
        applier.applyDefaultValue(df);

        // then
        assertThat(df.getValue()).isEqualTo("abc");
    }

    @Test
    void applyDefaultValue__controlled_vocabulary() {

        // given
        DatasetField df = new DatasetField();
        DatasetFieldType dft = new DatasetFieldType();
        ControlledVocabularyValue cvv1 = new ControlledVocabularyValue(1L, "abc", dft);
        ControlledVocabularyValue cvv2 = new ControlledVocabularyValue(1L, "cba", dft);
        dft.setControlledVocabularyValues(ImmutableList.of(cvv1, cvv2));
        dft.setDefaultValue("abc");
        df.setDatasetFieldType(dft);

        // when
        applier.applyDefaultValue(df);

        // then
        assertThat(df.getControlledVocabularyValues()).containsExactly(cvv1);
    }

    @Test
    void applyDefaultValue__for_children() {

        // given
        DatasetFieldType dft = new DatasetFieldType();
        DatasetFieldType childDft1 = new DatasetFieldType();
        DatasetFieldType childDft2 = new DatasetFieldType();
        childDft2.setDefaultValue("abc");
        dft.setChildDatasetFieldTypes(ImmutableList.of(childDft1, childDft2));

        DatasetField df = DatasetField.createNewEmptyDatasetField(dft, null);

        // when
        applier.applyDefaultValue(df);

        // then
        assertThat(df.getChildren().get(0).getValue()).isNull();
        assertThat(df.getChildren().get(1).getValue()).isEqualTo("abc");
    }
}
