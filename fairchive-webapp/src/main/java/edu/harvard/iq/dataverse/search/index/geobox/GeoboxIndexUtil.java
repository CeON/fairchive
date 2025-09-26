package edu.harvard.iq.dataverse.search.index.geobox;

import edu.harvard.iq.dataverse.common.DatasetFieldConstant;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetField;
import edu.harvard.iq.dataverse.search.response.GeoPoint;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static edu.harvard.iq.dataverse.common.DatasetFieldConstant.geographicCoordinates;

public class GeoboxIndexUtil {

    private PolygonToSolrConverter polygonToSolrConverter = new PolygonToSolrConverter();

    // -------------------- LOGIC --------------------

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
