package edu.harvard.iq.dataverse.validation.field.validators.geobox;

import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataset.FieldType;
import edu.harvard.iq.dataverse.search.advanced.field.GeoboxCoordSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.GroupingSearchField;
import edu.harvard.iq.dataverse.search.advanced.field.SearchField;

import java.util.Collections;
import java.util.List;

import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.geographicCoordinates;

public class GeoboxTestUtil {

    // -------------------- LOGIC --------------------

    public DatasetField buildPolygonGeobox(String coordinates) {
        DatasetField geobox = buildSingle(null, null);
        List<DatasetField> children = geobox.getDatasetFieldsChildren();
        DatasetField field = new DatasetField();
        DatasetFieldType type = new DatasetFieldType(geographicCoordinates, FieldType.TEXT, true);
        field.setDatasetFieldType(type);
        field.setValue(coordinates);
        field.setDatasetFieldParent(geobox);
        children.add(field);

        return geobox;
    }

    public DatasetField buildSingle(GeoboxFields geoboxField, String value) {
        DatasetField field = new DatasetField();
        DatasetFieldType type = new DatasetFieldType();
        if (geoboxField != null) {
            type.setMetadata(Collections.singletonMap("geoboxCoord", geoboxField.fieldType()));
        }
        field.setDatasetFieldType(type);
        field.setValue(value);
        return field;
    }

    public SearchField buildGeoboxSearchField(String coordinates) {
        DatasetFieldType parentType = new DatasetFieldType();
        parentType.setName("GoespatialBox");
        parentType.setFieldType(FieldType.GEOBOX);
        GroupingSearchField parent = new GroupingSearchField("Geobox", "Geobox Field", "Description", null, parentType);
        List<SearchField> children = parent.getChildren();
        DatasetFieldType fieldType = new DatasetFieldType();
        fieldType.setName(DatasetFieldConstant.geographicCoordinates);
        fieldType.setDescription("Coord descr.");
        GeoboxCoordSearchField coordField = new GeoboxCoordSearchField(fieldType);
        coordField.setFieldValue(coordinates);
        coordField.setParent(parent);
        children.add(coordField);
        return parent;
    }
}
