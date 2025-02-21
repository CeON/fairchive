package edu.harvard.iq.dataverse.persistence.guestbook;

import static javax.persistence.CascadeType.MERGE;
import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.CascadeType.REMOVE;
import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import edu.harvard.iq.dataverse.persistence.JpaEntity;

/**
 * @author skraffmiller
 */
@Entity
@Table(indexes = {
        @Index(columnList = "guestbook_id")
})
public class CustomQuestion implements Serializable, JpaEntity<Long> {
    
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Guestbook guestbook;

    @OneToMany(mappedBy = "customQuestion", cascade = {REMOVE, MERGE, PERSIST}, orphanRemoval = true)
    private List<CustomQuestionResponse> customQuestionResponses;

    @OneToMany(mappedBy = "customQuestion", cascade = {REMOVE, MERGE, PERSIST}, orphanRemoval = true)
    @OrderBy("displayOrder")
    private List<CustomQuestionValue> customQuestionValues;

    @Column(nullable = false)
    private String questionType;

    @Column(nullable = false)
    private String questionString;
    private boolean required;

    private boolean hidden;  //when a question is marked for removal, but it has data it is set to hidden

    private int displayOrder;
    
    @Override  
    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public int getDisplayOrder() {
        return this.displayOrder;
    }

    public void setDisplayOrder(final int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public boolean isHidden() {
        return this.hidden;
    }

    public void setHidden(final boolean hidden) {
        this.hidden = hidden;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(final boolean required) {
        this.required = required;
    }

    public Guestbook getGuestbook() {
        return this.guestbook;
    }

    public void setGuestbook(final Guestbook guestbook) {
        this.guestbook = guestbook;
    }

    public String getQuestionString() {
        return this.questionString;
    }

    public void setQuestionString(final String questionString) {
        this.questionString = questionString;
    }

    public List<CustomQuestionValue> getCustomQuestionValues() {
        return this.customQuestionValues;
    }

    public void setCustomQuestionValues(final List<CustomQuestionValue> values) {
        this.customQuestionValues = values;
    }

    public String getQuestionType() {
        return this.questionType;
    }

    public void setQuestionType(final String questionType) {
        this.questionType = questionType;
    }

    public List<CustomQuestionResponse> getCustomQuestionResponses() {
        return this.customQuestionResponses;
    }

    public void setCustomQuestionResponses(final List<CustomQuestionResponse> responses) {
        this.customQuestionResponses = responses;
    }

    public void removeCustomQuestionValue(final int index) {
        this.customQuestionValues.remove(index);
    }

    public void addCustomQuestionValue(final int index, final CustomQuestionValue cq) {
        this.customQuestionValues.add(index, cq);
    }


    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof CustomQuestion)) {
            return false;
        }
        CustomQuestion other = (CustomQuestion) object;
        return (this.id != null || other.id == null) && (this.id == null || this.id.equals(other.id));
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dvn.core.vdc.CustomQuestion[ id=" + id + " ]";
    }

}

