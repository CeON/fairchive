package edu.harvard.iq.dataverse.persistence.guestbook;

import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import javax.faces.model.SelectItem;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import edu.harvard.iq.dataverse.persistence.JpaEntity;

/**
 * @author skraffmiller
 */
@Entity
@Table(indexes = {
        @Index(columnList = "guestbookresponse_id")
})
public class CustomQuestionResponse implements Serializable, JpaEntity<Long> {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(nullable = false)
    private GuestbookResponse guestbookResponse;

    @ManyToOne
    @JoinColumn(nullable = false)
    private CustomQuestion customQuestion;

    @Column(name = "response", columnDefinition = "TEXT", nullable = true)
    private String response;
    
    @Transient
    private List<SelectItem> responseSelectItems;
    
    @Transient
    private String validationMessage;

    @Override
    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public GuestbookResponse getGuestbookResponse() {
        return this.guestbookResponse;
    }

    public void setGuestbookResponse(final GuestbookResponse response) {
        this.guestbookResponse = response;
    }

    public String getResponse() {
        return this.response;
    }

    public void setResponse(final String response) {
        this.response = response;
    }


    public CustomQuestion getCustomQuestion() {
        return this.customQuestion;
    }

    public void setCustomQuestion(final CustomQuestion question) {
        this.customQuestion = question;
    }

    public List<SelectItem> getResponseSelectItems() {
        return responseSelectItems;
    }

    public void setResponseSelectItems(final List<SelectItem> items) {
        this.responseSelectItems = items;
    }
    
    public String getValidationMessage() {
        return this.validationMessage;
    }

    public void setValidationMessage(final String message) {
        this.validationMessage = message;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id);
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof CustomQuestionResponse)) {
            return false;
        }
        CustomQuestionResponse other = (CustomQuestionResponse) object;
        return (this.id != null || other.id == null) && (this.id == null || this.id.equals(other.id));
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dvn.core.vdc.CustomQuestionResponse[ id=" + id + " ]";
    }
}

