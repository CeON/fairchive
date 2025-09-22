package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import static edu.harvard.iq.dataverse.persistence.dataset.InputRendererType.PERIODO;
import static org.apache.commons.lang.StringUtils.EMPTY;

import java.util.List;

import javax.faces.context.FacesContext;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;
import edu.harvard.iq.dataverse.persistence.dataset.PeriodoDictionary;
import edu.harvard.iq.dataverse.persistence.dataset.PeriodoDictionary.Period;
import io.vavr.control.Option;

public class PeriodoRenderer implements InputFieldRenderer {

    private final ConditionalRendering conditionalRendering;

    // -------------------- CONSTRUCTORS --------------------

    public PeriodoRenderer(ConditionalRendering conditionalRendering) {
        this.conditionalRendering = conditionalRendering;
    }

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

    @Override
    public Option<ConditionalRendering> getConditionalRendering() {
        return Option.of(conditionalRendering);
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
    public String getDetailsOf(final DatasetField field) {
        return PeriodoDictionary.getByUrl(field.getFieldValue().getOrElse(EMPTY))
                .map(gn -> gn.getDetails("<b>", "</b>", "<br/>"))
                .orElse(EMPTY);
    }
}
