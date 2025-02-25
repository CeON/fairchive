package edu.harvard.iq.dataverse.persistence.guestbook;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static java.util.stream.Collectors.toList;
import static javax.persistence.CascadeType.MERGE;
import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.CascadeType.REMOVE;
import static javax.persistence.GenerationType.IDENTITY;
import static javax.persistence.TemporalType.TIMESTAMP;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Temporal;
import javax.persistence.Transient;

import edu.harvard.iq.dataverse.persistence.JpaEntity;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;

/**
 * @author skraffmiller
 */
@SuppressWarnings("serial")
@Entity
public class Guestbook implements Serializable, JpaEntity<Long> {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    /**
     * Holds value of the Dataverse
     */
    @ManyToOne
    @JoinColumn(nullable = true)
    private Dataverse dataverse;

    @OneToMany(mappedBy = "guestbook", cascade = {REMOVE, MERGE, PERSIST}, orphanRemoval = true)
    @OrderBy("displayOrder")
    private List<CustomQuestion> customQuestions;

    private String name;

    private boolean enabled;
    private boolean nameRequired;
    private boolean emailRequired;
    private boolean institutionRequired;
    private boolean positionRequired;
    @Temporal(value = TIMESTAMP)
    @Column(nullable = false)
    private Date createTime;
    
    @Override
    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Dataverse getDataverse() {
        return this.dataverse;
    }

    public void setDataverse(final Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public List<CustomQuestion> getCustomQuestions() {
        return this.customQuestions;
    }

    public void setCustomQuestions(final List<CustomQuestion> questions) {
        this.customQuestions = questions;
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isNameRequired() {
        return nameRequired;
    }

    public void setNameRequired(boolean nameRequired) {
        this.nameRequired = nameRequired;
    }

    public boolean isEmailRequired() {
        return this.emailRequired;
    }

    public void setEmailRequired(final boolean emailRequired) {
        this.emailRequired = emailRequired;
    }

    public boolean isInstitutionRequired() {
        return this.institutionRequired;
    }

    public void setInstitutionRequired(final boolean institutionRequired) {
        this.institutionRequired = institutionRequired;
    }

    public boolean isPositionRequired() {
        return this.positionRequired;
    }

    public void setPositionRequired(final boolean positionRequired) {
        this.positionRequired = positionRequired;
    }

    public Date getCreateTime() {
        return this.createTime;
    }

    public void setCreateTime(final Date createTime) {
        this.createTime = createTime;
    }

    public Guestbook copyGuestbook(final Guestbook source, final Dataverse dataverse) {
        final Guestbook newGuestbook = new Guestbook();
        newGuestbook.setDataverse(dataverse);
        newGuestbook.setEmailRequired(source.isEmailRequired());
        newGuestbook.setNameRequired(source.isNameRequired());
        newGuestbook.setPositionRequired(source.isPositionRequired());
        newGuestbook.setInstitutionRequired(source.isInstitutionRequired());
        newGuestbook.setCustomQuestions(new ArrayList<>());
        if (!source.getCustomQuestions().isEmpty()) {
            for (final CustomQuestion sq : source.getCustomQuestions()) {
                final CustomQuestion target = new CustomQuestion();
                target.setQuestionType(sq.getQuestionType());
                target.setGuestbook(newGuestbook);
                target.setHidden(sq.isHidden());
                target.setRequired(sq.isRequired());
                target.setDisplayOrder(sq.getDisplayOrder());
                target.setQuestionString(sq.getQuestionString());
                if (!sq.getCustomQuestionValues().isEmpty()) {
                    target.setCustomQuestionValues(new ArrayList<>());
                    for (final CustomQuestionValue scqv : sq.getCustomQuestionValues()) {
                        final CustomQuestionValue newVal = new CustomQuestionValue();
                        newVal.setValueString(scqv.getValueString());
                        newVal.setCustomQuestion(target);
                        target.getCustomQuestionValues().add(newVal);
                    }
                }
                newGuestbook.getCustomQuestions().add(target);
            }
        }
        return newGuestbook;
    }

    @Transient
    private boolean deletable;

    public boolean isDeletable() {
        return this.deletable;
    }

    public void setDeletable(final boolean deletable) {
        this.deletable = deletable;
    }

    public List<String> getRequiredAccountInformation() {
        final List<String> result = new ArrayList<>();
        if (this.nameRequired) {
            result.add(getStringFromBundle("name"));
        }
        if (this.emailRequired) {
            result.add(getStringFromBundle("email"));
        }
        if (this.institutionRequired) {
            result.add(getStringFromBundle("institution"));
        }
        if (this.positionRequired) {
            result.add(getStringFromBundle("position"));
        }
        return result;
    }

    public List<String> getOptionalAccountInformation() {
        final List<String> result = new ArrayList<>();
        if (!this.nameRequired) {
            result.add(getStringFromBundle("name"));
        }
        if (!this.emailRequired) {
            result.add(getStringFromBundle("email"));
        }
        if (!this.institutionRequired) {
            result.add(getStringFromBundle("institution"));
        }
        if (!this.positionRequired) {
            result.add(getStringFromBundle("position"));
        }
        return result;

    }

    public List<String> getRequiredQuestionsList() {
        return getCustomQuestions().stream()
                .filter(CustomQuestion::isRequired)
                .map(CustomQuestion::getQuestionString)
                .collect(toList());
    }

    public List<String> getOptionalQuestionsList() {    
        return getCustomQuestions().stream()
                .filter(q -> !q.isRequired())
                .map(CustomQuestion::getQuestionString)
                .collect(toList());
    }

    public void removeCustomQuestion(final int index) {
        this.customQuestions.remove(index);
    }

    public void addCustomQuestion(final int index, final CustomQuestion cq) {
        this.customQuestions.add(index, cq);
    }

    @Transient
    private Long usageCount;

    public Long getUsageCount() {
        return this.usageCount;
    }

    public void setUsageCount(final Long count) {
        this.usageCount = count;
    }

    @Transient
    private Long usageCountDataverse;

    public Long getUsageCountDataverse() {
        return this.usageCountDataverse;
    }

    public void setUsageCountDataverse(final Long usageCountDataverse) {
        this.usageCountDataverse = usageCountDataverse;
    }

    @Transient
    private Long responseCount;

    public Long getResponseCount() {
        return this.responseCount;
    }

    public void setResponseCount(final Long responseCount) {
        this.responseCount = responseCount;
    }

    @Transient
    private Long responseCountDataverse;

    public Long getResponseCountDataverse() {
        return this.responseCountDataverse;
    }

    public void setResponseCountDataverse(final Long responseCountDataverse) {
        this.responseCountDataverse = responseCountDataverse;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Guestbook)) {
            return false;
        }
        Guestbook other = (Guestbook) object;
        return Objects.equals(getId(), other.getId());
    }

    @Override
    public String toString() {
        return "Guestbook [id=" + id + "]";
    }

}
