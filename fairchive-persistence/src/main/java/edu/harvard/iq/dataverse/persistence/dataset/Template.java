package edu.harvard.iq.dataverse.persistence.dataset;

import static javax.persistence.CascadeType.MERGE;
import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.CascadeType.REMOVE;
import static javax.persistence.GenerationType.IDENTITY;
import static javax.persistence.TemporalType.TIMESTAMP;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.validation.constraints.Size;

import org.eclipse.persistence.annotations.Customizer;
import org.hibernate.validator.constraints.NotBlank;

import edu.harvard.iq.dataverse.persistence.JpaEntity;
import edu.harvard.iq.dataverse.persistence.config.EntityCustomizer;
import edu.harvard.iq.dataverse.persistence.config.annotations.CustomizeSelectionQuery;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;

/**
 * @author skraffmiller
 */
@SuppressWarnings("serial")
@Entity
@Table(indexes = {@Index(columnList = "dataverse_id")})
@Customizer(EntityCustomizer.class)
public class Template implements JpaEntity<Long>, Serializable {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @NotBlank(message = "{dataset.templatename}")
    @Size(max = 255, message = "{dataset.nameLength}")
    @Column(nullable = false)
    private String name;

    private Long usageCount;

    @Temporal(value = TIMESTAMP)
    @Column(nullable = false)
    private Date createTime;

    @OneToMany(mappedBy = "template", orphanRemoval = true,
            cascade = {REMOVE, MERGE, PERSIST})
    @CustomizeSelectionQuery(EntityCustomizer.Customizations.DATASET_FIELDS_WITH_PRIMARY_SOURCE)
    private List<DatasetField> datasetFields = new ArrayList<>();

    @ManyToOne
    @JoinColumn(nullable = true)
    private Dataverse dataverse;
    
    public final static Comparator<Template> comparator = (t1, t2) -> t1.name.compareToIgnoreCase(t2.name);

    // -------------------- CONSTRUCTORS --------------------

    public Template() { }

    public Template(final Dataverse dataverseIn) {
        this.dataverse = dataverseIn;
    }

    // -------------------- GETTERS --------------------
    @Override
    public Long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public Long getUsageCount() {
        return this.usageCount;
    }

    public Date getCreateTime() {
        return this.createTime;
    }

    public String getCreateDate() {
        return new SimpleDateFormat("MMMM d, yyyy").format(this.createTime);
    }

    public List<DatasetField> getDatasetFields() {
        return this.datasetFields;
    }

    public Dataverse getDataverse() {
        return this.dataverse;
    }

    // -------------------- LOGIC --------------------

    public Template cloneNewTemplate(final Template source) {
        final Template result = new Template(this.dataverse);
        // if the latest version has values get them copied over
        if (source.getDatasetFields() != null && !source.getDatasetFields().isEmpty()) {
            result.setDatasetFields(DatasetFieldUtil.copyDatasetFields(source.getDatasetFields()));
        }
        return result;
    }

    public Template cloneNewTemplate() {
        return cloneNewTemplate(this);
    }

    // -------------------- SETTERS --------------------

    public void setId(final Long id) {
        this.id = id;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setUsageCount(final Long usageCount) {
        this.usageCount = usageCount;
    }

    public void setCreateTime(final Date createTime) {
        this.createTime = createTime;
    }

    public void setDataverse(final Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public void setDatasetFields(final List<DatasetField> fields) {
    	fields.forEach(field -> field.setTemplate(this));
        this.datasetFields = fields;
    }

    // -------------------- hashCode & equals --------------------

    @Override
    public int hashCode() {
    	return Objects.hashCode(this.id);
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Template)) {
            return false;
        }
        Template other = (Template) object;
        return Objects.equals(this.id, other.id);
    }

}
