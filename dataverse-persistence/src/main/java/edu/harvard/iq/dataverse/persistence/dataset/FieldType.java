package edu.harvard.iq.dataverse.persistence.dataset;

/**
 * The set of possible metatypes of the field. Used for validation and layout.
 */
public enum FieldType {
    TEXT, TEXTBOX, DATE, EMAIL, URL, FLOAT, INT, CHECKBOX, NONE, GEOBOX, PERIODO;

    public boolean sanitizeHtml() {
        return this.equals(URL) | this.equals(TEXTBOX);
    }

    public boolean isNumberic() {
        return this.equals(INT) | this.equals(FLOAT);
    }

    public boolean isTextual() {
        return this.equals(TEXT) | this.equals(TEXTBOX) | this.equals(EMAIL)
                | this.equals(URL);
    }
    
    public boolean isGeospatial() {
        return this.equals(GEOBOX);
    }
    
    public boolean isDate() {
        return this.equals(DATE);
    }
}
