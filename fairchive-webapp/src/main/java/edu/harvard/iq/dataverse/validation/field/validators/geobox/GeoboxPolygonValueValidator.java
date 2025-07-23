package edu.harvard.iq.dataverse.validation.field.validators.geobox;

import edu.harvard.iq.dataverse.persistence.dataset.ValidatableField;
import edu.harvard.iq.dataverse.validation.field.FieldValidationResult;
import edu.harvard.iq.dataverse.validation.field.FieldValidator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

class GeoboxPolygonValueValidator implements FieldValidator {

    private static BigDecimal MAX_LONGITUDE = new BigDecimal("180");
    private static BigDecimal MAX_LATITUDE = new BigDecimal("90");

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

        BigDecimal maxLongitude = BigDecimal.ZERO;
        BigDecimal minLongitude = BigDecimal.ZERO;
        for (String line : value.split("\n")) {
            String[] coords = line.trim().split("\\s+");
            if (coords.length != 2) {
                return FieldValidationResult.invalid(field, "geobox.polygon.invalid.geo.point",
                        field.getDatasetFieldType().getDisplayName());
            }

            if (!NumberUtils.isParsable(coords[0])) {
                return FieldValidationResult.invalid(field, "isNotValidNumber", coords[0]);
            }

            if (!NumberUtils.isParsable(coords[1])) {
                return FieldValidationResult.invalid(field, "isNotValidNumber", coords[1]);
            }

            BigDecimal longitude = new BigDecimal(coords[0]);
            maxLongitude = maxLongitude.compareTo(longitude) > 0 ? maxLongitude : longitude;
            minLongitude = minLongitude.compareTo(longitude) > 0 ? longitude : minLongitude;
            BigDecimal latitude = new BigDecimal(coords[1]);
            if (longitude.abs().compareTo(MAX_LONGITUDE) > 0) {
                return FieldValidationResult.invalid(field, "geobox.invalid.longitude");
            }

            if (latitude.abs().compareTo(MAX_LATITUDE) > 0) {
                return FieldValidationResult.invalid(field, "geobox.invalid.latitude");
            }
        }

        BigDecimal span = maxLongitude.subtract(minLongitude).abs();
        if (span.compareTo(MAX_LONGITUDE) > 0) {
            return FieldValidationResult.invalid(field, "geobox.invalid.longitude.span");
        }

        return FieldValidationResult.ok();
    }
}
