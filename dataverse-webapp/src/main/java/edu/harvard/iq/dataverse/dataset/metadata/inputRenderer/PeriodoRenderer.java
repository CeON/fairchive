package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import static edu.harvard.iq.dataverse.persistence.dataset.InputRendererType.PERIODO;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.event.ValueChangeEvent;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;
import edu.harvard.iq.dataverse.persistence.dataset.PeriodoDictionary;
import edu.harvard.iq.dataverse.persistence.dataset.PeriodoDictionary.Period;

public class PeriodoRenderer implements InputFieldRenderer {

    private Optional<Period> selectedPeriod = Optional.empty();
    private final CapturingConverter converter = new CapturingConverter();

    // -------------------- GETTERS --------------------

    @Override
    public InputRendererType getType() {
        return PERIODO;
    }

    @Override
    public boolean renderInTwoColumns() {
        return true;
    }

    @Override
    public boolean isHidden() {
        return false;
    }
    
    // -------------------- LOGIC --------------------

    /**
     * Workaround for p:autocomplete in order to use function with 2 arguments in 'completeMethod'.
     * Since value is bound to {@link DatasetField} and binded only after executing 'completeMethod'
     * the query will not work since it will take previously binded value, so we are taking it from {@link FacesContext} directly.
     */
    public List<Period> processSuggestionQuery(final DatasetField datasetField) {
        final String query = SuggestionAutocompleteHelper.processSuggestionQuery("periodo")
                .orElseThrow(() -> new IllegalStateException("Autocomplete query was not found."));
        
        return PeriodoDictionary.find(query);
    }

    public boolean displayDetails() {
        return this.selectedPeriod.isPresent();
    }
    
    public String getDetails() {
        return this.selectedPeriod.map(p -> p.getDatails("<br>")).orElse("");
    }
    
    public Converter getConverter() {
        return this.converter;
    }
    
    public void processValueChange(final ValueChangeEvent event) {
        final String url = Objects.toString(event.getNewValue(), "");
        this.selectedPeriod = PeriodoDictionary.getByUrl(url);
    }
    
    private class CapturingConverter implements Converter {

        @Override
        public Object getAsObject(final FacesContext context,
                final UIComponent component,
                final String value) {
            return value;
        }

        @Override
        public String getAsString(final FacesContext context,
                final UIComponent component,
                final Object value) {
            selectedPeriod = PeriodoDictionary.getByUrl(value.toString());
            return value.toString();
        }
    }
}
