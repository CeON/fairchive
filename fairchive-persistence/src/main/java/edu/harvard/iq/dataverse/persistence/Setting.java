package edu.harvard.iq.dataverse.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;
import java.util.Objects;

/**
 * A single value in the config of dataverse.
 *
 * @author michael
 */
@SuppressWarnings("serial")
@Entity
public class Setting implements Serializable {

    @Id
    private String name;

    @Column(columnDefinition = "TEXT")
    private String content;

    // -------------------- CONSTRUCTORS --------------------

    public Setting() {
    }

    public Setting(final String name, final String content) {
        this.name = name;
        this.content = content;
    }

    // -------------------- GETTERS --------------------

    public String getName() {
        return this.name;
    }

    public String getContent() {
        return this.content;
    }

    // -------------------- SETTERS --------------------

    public void setName(final String name) {
        this.name = name;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    // -------------------- hashCode & equals --------------------

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Setting)) {
            return false;
        }
        final Setting other = (Setting) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        return Objects.equals(this.content, other.content);
    }

    // -------------------- toString --------------------

    @Override
    public String toString() {
        return "[Setting name:" + getName() + " value:" + getContent() + "]";
    }


}
