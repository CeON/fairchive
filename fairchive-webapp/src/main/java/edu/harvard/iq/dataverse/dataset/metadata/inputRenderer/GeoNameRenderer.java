package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import static edu.harvard.iq.dataverse.persistence.dataset.InputRendererType.GEONAME;
import static org.apache.commons.lang.StringUtils.EMPTY;

import java.util.List;

import javax.faces.context.FacesContext;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;
import edu.harvard.iq.dataverse.search.geonames.GeoName;
import edu.harvard.iq.dataverse.search.geonames.GeoNameDataFinder;

public class GeoNameRenderer implements InputFieldRenderer {

    private final GeoNameDataFinder geoNames;

    public GeoNameRenderer(final GeoNameDataFinder geoNames) {
        this.geoNames = geoNames;
    }

    // -------------------- GETTERS --------------------

    @Override
    public InputRendererType getType() {
        return GEONAME;
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
     * Workaround for p:autocomplete in order to use function with 2 arguments in
     * 'completeMethod'. Since value is bound to {@link DatasetField} and binded
     * only after executing 'completeMethod' the query will not work since it will
     * take previously binded value, so we are taking it from {@link FacesContext}
     * directly.
     */
    public List<GeoName> processSuggestionQuery(final DatasetField datasetField) {
        final String query = SuggestionAutocompleteHelper
                .processSuggestionQuery("geoname")
                .orElseThrow(() -> new IllegalStateException(
                        "Autocomplete query was not found."));
        List<GeoName> result = this.geoNames.find(query, 50);
        return result;
    }
    
    
    public String getDetailsOf(final DatasetField field) {
        return this.geoNames.findById(field.getFieldValue().getOrElse(EMPTY))
                .map(gn -> gn.getDetails("<b>", "</b>", "<br/>"))
                .orElse(EMPTY);
    }

}