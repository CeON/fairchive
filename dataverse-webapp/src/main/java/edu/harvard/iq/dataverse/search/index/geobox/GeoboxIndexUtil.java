package edu.harvard.iq.dataverse.search.index.geobox;

import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.search.response.GeoPoint;
import edu.harvard.iq.dataverse.validation.field.validators.geobox.GeoboxFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.geographicCoordinates;

public class GeoboxIndexUtil {
    private static final Logger logger = LoggerFactory.getLogger(GeoboxIndexUtil.class);

    private RectangleToSolrConverter converter = new RectangleToSolrConverter();
    private PolygonToSolrConverter polygonToSolrConverter = new PolygonToSolrConverter();

    // -------------------- LOGIC --------------------

    public List<String> geoboxFieldToSolr(DatasetField field) {
        Map<String, String> values = new HashMap<>();
        for (DatasetField subfield : field.getDatasetFieldsChildren()) {
             String label = (String) subfield.getDatasetFieldType().getMetadata("geoboxCoord");
             values.put(label, subfield.getValue());
        }
        Rectangle rectangle = new Rectangle(
                values.get(GeoboxFields.X1.fieldType()),
                values.get(GeoboxFields.Y1.fieldType()),
                values.get(GeoboxFields.X2.fieldType()),
                values.get(GeoboxFields.Y2.fieldType()));
        return rectangle.cutIfNeeded().stream()
                .map(converter::toSolrPolygon)
                .collect(Collectors.toList());
    }

    public List<String> geoboxPolygonFieldToSolr(DatasetField field) {
        Optional<DatasetField> polygonGeo = field.getDatasetFieldsChildren().stream()
                .filter(f -> DatasetFieldConstant.geographicCoordinates.equals(f.getTypeName())).findFirst();

        if (!polygonGeo.isPresent()) {
            throw new IllegalStateException("Missing polygon dataset field");
        }

        List<GeoPoint> geoPoints = Arrays.stream(polygonGeo.get().getValue().split("\n"))
                .map(line -> {
                    String[] coords = line.trim().split("\\s+");
                    double longitude = Double.parseDouble(coords[0]);
                    double latitude = Double.parseDouble(coords[1]);
                    return new GeoPoint(latitude, longitude);
                })
                .collect(Collectors.toList());

        return new ArrayList<>(Collections.singletonList(polygonToSolrConverter.toSolrPolygon(geoPoints)));
    }

    public boolean isIndexablePolygon(DatasetField field) {
        return field.getDatasetFieldsChildren()
                        .stream()
                        .anyMatch(f -> geographicCoordinates.equals(f.getTypeName()));
    }
}
