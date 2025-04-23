package edu.harvard.iq.dataverse.validation.field.validators.geobox;

import edu.harvard.iq.dataverse.common.BundleUtil;
import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.search.advanced.field.GeoboxCoordSearchField;
import edu.harvard.iq.dataverse.search.response.GeoPoint;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;
import edu.harvard.iq.dataverse.validation.field.FieldValidator;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * In advance search we can only select rectangles so we must validate this special case
 * Valid coordinates have 4 or 5 point. For 5 points first and last point must be equal
 * This method works only for axis-aligned rectangles (rectangles that are not rotated)
 */
class GeoboxRectangleValueValidator implements FieldValidator {

    @Override
    public String getName() {
        return StringUtils.EMPTY;
    }


    @Override
    public FieldValidationResult validate(ValidatableField field, Map<String, Object> params, Map<String, ? extends List<? extends ValidatableField>> fieldIndex) {
        String value = field.getSingleValue();
        if (StringUtils.isBlank(value)) {
            return FieldValidationResult.ok();
        }

        if (!(field instanceof GeoboxCoordSearchField)) {
            return FieldValidationResult.ok();
        }

        List<GeoPoint> coordinates = GeoPoint.fromCoordinateString(value);
        coordinates = coordinates.stream().distinct().collect(Collectors.toList());

        if (coordinates.size() != 4) {
            return FieldValidationResult.invalid(field, BundleUtil.getStringFromBundle("geobox.polygon.invalid.coordiantes.length"));
        }

        // Sort to match: Top-left, Top-right, Bottom-left, Bottom-right
        coordinates.sort((a, b) -> {
            int compareY = Double.compare(b.getLatitude(), a.getLatitude()); // descending Y (top first)
            return (compareY != 0) ? compareY : Double.compare(a.getLongitude(), b.getLongitude()); // ascending X
        });

        GeoPoint topLeft = coordinates.get(0);
        GeoPoint topRight = coordinates.get(1);
        GeoPoint bottomLeft = coordinates.get(2);
        GeoPoint bottomRight = coordinates.get(3);

        boolean topSide = topRight.getLatitude() == topLeft.getLatitude();
        boolean bottomSide = bottomRight.getLatitude() == bottomLeft.getLatitude();
        boolean leftSide = topLeft.getLongitude() == bottomLeft.getLongitude();
        boolean rightSide = topRight.getLongitude() == bottomRight.getLongitude();

        if (topSide && bottomSide && leftSide && rightSide) {
            return FieldValidationResult.ok();
        } else {
            return FieldValidationResult.invalid(field, BundleUtil.getStringFromBundle("geobox.polygon.invalid.coordiantes.rectangle"));
        }
    }
}
