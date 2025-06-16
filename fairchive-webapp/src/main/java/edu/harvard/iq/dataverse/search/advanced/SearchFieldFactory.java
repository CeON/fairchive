package edu.harvard.iq.dataverse.search.advanced;

import javax.ejb.Stateless;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.search.advanced.field.CheckboxSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.DateSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.GeoNameSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.GeoboxCoordSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.NumberSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.PeriodoSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.SearchField;
import edu.harvard.iq.dataverse.search.advanced.field.SelectOneSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.TextSearchField;
import io.vavr.Tuple;

@Stateless
public class SearchFieldFactory {

    // -------------------- LOGIC --------------------

    public SearchField create(DatasetFieldType fieldType) {
        if (fieldType.containsControlledVocabularyValues()) {
            return fieldType.isThisOrParentAllowsMultipleValues()
                    ? mapCheckBoxValues(fieldType) : mapSelectOneValues(fieldType);
        } else if (fieldType.isTextual()) {
            return new TextSearchField(fieldType);
        } else if (fieldType.isDate()) {
            return new DateSearchField(fieldType);
        } else if (fieldType.isNumberic()) {
            return new NumberSearchField(fieldType);
        } else if (fieldType.isGeoboxField()) {
            return new GeoboxCoordSearchField(fieldType);
        } else if(fieldType.isGeoName()) {
            return new GeoNameSearchField(fieldType);
        } else if(fieldType.isPeriodo()) {
            return new PeriodoSearchField(fieldType);
        } else {
            return SearchField.EMPTY;
        }
    }

    // -------------------- PRIVATE --------------------

    private CheckboxSearchField mapCheckBoxValues(DatasetFieldType fieldType) {
        CheckboxSearchField checkboxSearchField = new CheckboxSearchField(fieldType);

        fieldType.getControlledVocabularyValues()
                .forEach(v -> checkboxSearchField.getCheckboxLabelAndValue()
                        .add(Tuple.of(v.getLocaleStrValue(), v.getStrValue())));
        return checkboxSearchField;
    }

    private SelectOneSearchField mapSelectOneValues(DatasetFieldType fieldType) {
        SelectOneSearchField selectOneSearchField = new SelectOneSearchField(fieldType);

        fieldType.getControlledVocabularyValues()
                .forEach(v -> selectOneSearchField.getListLabelAndValue()
                        .add(Tuple.of(v.getLocaleStrValue(), v.getStrValue())));
        return selectOneSearchField;
    }
}

