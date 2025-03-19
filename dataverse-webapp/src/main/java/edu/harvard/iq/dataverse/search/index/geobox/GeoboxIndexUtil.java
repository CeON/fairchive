package edu.harvard.iq.dataverse.search.index.geobox;

import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.search.response.GeoPoint;
import edu.harvard.iq.dataverse.validation.field.validators.geobox.GeoboxComponentValidator;
import edu.harvard.iq.dataverse.validation.field.validators.geobox.GeoboxFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.geographicCoordinates;

public class GeoboxIndexUtil {
    private static final Logger logger = LoggerFactory.getLogger(GeoboxIndexUtil.class);
    private static final Set<String> COORD_FIELDS = Initializer.initializeCoordFields();

    private RectangleToSolrConverter converter = new RectangleToSolrConverter();
    private PolygonToSolrConverter polygonToSolrConverter = new PolygonToSolrConverter();
    private GeoboxComponentValidator componentValidator = new GeoboxComponentValidator();

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

    public boolean isIndexable(DatasetField field) {
        if (field == null) {
            return false;
        }
        Set<String> availableCoords = field.getDatasetFieldsChildren().stream()
                .map(f -> (String) f.getDatasetFieldType().getMetadata("geoboxCoord"))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return availableCoords.containsAll(COORD_FIELDS)
                && componentValidator.validate(field, Collections.emptyMap(), Collections.emptyMap()).isOk();
    }

    public boolean isIndexablePolygon(DatasetField field) {
        if (field == null) {
            return false;
        }

        return field.getDatasetFieldsChildren()
                        .stream()
                        .anyMatch(f -> geographicCoordinates.equals(f.getTypeName()));
    }

    // -------------------- INNER CLASSES --------------------

    private static class Initializer {
        public static Set<String> initializeCoordFields() {
            return Arrays.stream(GeoboxFields.values())
                    .map(GeoboxFields::fieldType)
                    .collect(Collectors.toSet());
        }
    }
}
