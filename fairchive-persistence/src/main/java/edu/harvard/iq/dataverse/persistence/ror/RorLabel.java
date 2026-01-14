package edu.harvard.iq.dataverse.persistence.ror;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

@SuppressWarnings("serial")
@Embeddable
public class RorLabel implements Serializable {

    @Column
    private String label;

    @Column
    private String code;

    // -------------------- CONSTRUCTORS --------------------

    public RorLabel() { }

    public RorLabel(final String label, final String code) {
        this.label = label;
        this.code = code;
    }

    // -------------------- GETTERS --------------------

    public String getLabel() {
        return this.label;
    }

    public String getCode() {
        return this.code;
    }

    // -------------------- SETTERS --------------------

    public void setLabel(final String label) {
        this.label = label;
    }

    public void setCode(final String code) {
        this.code = code;
    }
}
