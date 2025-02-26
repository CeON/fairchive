package edu.harvard.iq.dataverse.persistence.guestbook;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import edu.harvard.iq.dataverse.persistence.JpaEntity;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author skraffmiller
 */
@Entity
public class CustomQuestionValue implements Serializable, JpaEntity<Long> {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String valueString;
    
    @ManyToOne
    @JoinColumn(nullable = false)
    private CustomQuestion customQuestion;

    private int displayOrder;

    public int getDisplayOrder() {
        return this.displayOrder;
    }

    public void setDisplayOrder(final int order) {
        this.displayOrder = order;
    }

    public CustomQuestion getCustomQuestion() {
        return this.customQuestion;
    }

    public void setCustomQuestion(final CustomQuestion question) {
        this.customQuestion = question;
    }

    public String getValueString() {
        return this.valueString;
    }

    public void setValueString(final String value) {
        this.valueString = value;
    }
    
    @Override
    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id);
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof CustomQuestionValue)) {
            return false;
        }
        CustomQuestionValue other = (CustomQuestionValue) object;
        return (this.id != null || other.id == null) && (this.id == null || this.id.equals(other.id));
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dvn.core.vdc.CustomQuestionValue[ id=" + id + " ]";
    }

}

