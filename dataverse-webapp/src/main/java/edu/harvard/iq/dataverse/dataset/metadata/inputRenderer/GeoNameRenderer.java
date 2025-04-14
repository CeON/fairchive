package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import static edu.harvard.iq.dataverse.persistence.dataset.InputRendererType.GEONAME;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.event.ValueChangeEvent;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;
import edu.harvard.iq.dataverse.persistence.geonames.GeoName;
import edu.harvard.iq.dataverse.persistence.geonames.GeoNameRepository;

public class GeoNameRenderer implements InputFieldRenderer {

    private Optional<GeoName> selectedGeoName = Optional.empty();
    private final GeoNameRepository geoNameRepo;
    private final CapturingConverter converter = new CapturingConverter();

    public GeoNameRenderer(final GeoNameRepository geoNameRepo) {
        this.geoNameRepo = geoNameRepo;
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
     * Workaround for p:autocomplete in order to use function with 2 arguments in 'completeMethod'.
     * Since value is bound to {@link DatasetField} and binded only after executing 'completeMethod'
     * the query will not work since it will take previously binded value, so we are taking it from {@link FacesContext} directly.
     */
    public List<GeoName> processSuggestionQuery(final DatasetField datasetField) {
        final String query = SuggestionAutocompleteHelper.processSuggestionQuery("geoname")
                .orElseThrow(() -> new IllegalStateException("Autocomplete query was not found."));

        return this.geoNameRepo.find(query);
    }

    public boolean displayDetails() {
        return this.selectedGeoName.isPresent();
    }

    public String getDetails() {
        return this.selectedGeoName.map(gn -> gn.getDetails("<br/>")).orElse("");
    }

    public Converter getConverter() {
        return this.converter;
    }

    public void processValueChange(final ValueChangeEvent event) {
        final int id = Integer.parseInt(event.getNewValue().toString());
        this.selectedGeoName = geoNameRepo.findById(id);
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
            final int id = Integer.parseInt(value.toString());
            selectedGeoName = geoNameRepo.findById(id);
            return value.toString();
        }
    }
}