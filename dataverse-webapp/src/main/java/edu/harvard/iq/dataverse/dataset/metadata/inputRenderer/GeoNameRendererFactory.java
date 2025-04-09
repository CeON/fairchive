package edu.harvard.iq.dataverse.dataset.metadata.inputRenderer;

import javax.ejb.Stateless;
import javax.inject.Inject;

import com.google.gson.JsonObject;

import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.InputRendererType;
import edu.harvard.iq.dataverse.persistence.geonames.GeoNameRepository;

@Stateless
public class GeoNameRendererFactory implements InputFieldRendererFactory<GeoNameRenderer> {

    private final GeoNameRepository geoNameRepo;
    
    @Inject
    public GeoNameRendererFactory(final GeoNameRepository geoNameRepo) {
        this.geoNameRepo = geoNameRepo;
    }

    @Override
    public InputRendererType isFactoryForType() {
        return InputRendererType.GEONAME;
    }

    @Override
    public GeoNameRenderer createRenderer(DatasetFieldType fieldType, JsonObject jsonOptions) {
        return new GeoNameRenderer(this.geoNameRepo);
    }

}
