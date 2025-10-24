package edu.harvard.iq.dataverse.dataset.metadata.valueRenderer;

import javax.ejb.Stateless;
import javax.inject.Inject;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.search.geonames.GeoNameDataFinder;
import edu.harvard.iq.dataverse.search.periodo.PeriodoDataFinder;

@Stateless
public class ValueRendererRepository {

    @Inject
    private GeoNameDataFinder geoNames;
    
    @Inject
    private PeriodoDataFinder periods;

    public ValueRenderer getRendererFor(final DatasetField field) {
        if (field.isGeoName()) {
            return value -> this.geoNames.findById(value)
                    .map(gn -> gn.getDetails("<b>", "</b>", "<br/>")).orElse("");
        } else if(field.isPeriodo()) {
            return value -> this.periods.getByUrl(value)
                    .map(gn -> gn.getDetails("<b>", "</b>", "<br/>")).orElse("");
        }else {
            return value -> value;
        }
    }
}
