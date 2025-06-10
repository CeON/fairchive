package edu.harvard.iq.dataverse.dataset.metadata.valueRenderer;

import javax.ejb.Stateless;
import javax.inject.Inject;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.search.geonames.GeoNameDataFinder;

@Stateless
public class ValueRendererRepository {

    @Inject
    private GeoNameDataFinder geoNames;

    public ValueRenderer getRendererFor(final DatasetField field) {
        if (field.isGeoName()) {
            return value -> this.geoNames.findById(value)
                    .map(gn -> gn.getDetails("<b>", "</b>", "<br/>")).orElse("");
        } else {
            return value -> value;
        }
    }
}
