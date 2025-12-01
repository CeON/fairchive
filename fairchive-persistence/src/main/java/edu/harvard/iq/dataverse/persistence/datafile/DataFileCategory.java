package edu.harvard.iq.dataverse.persistence.datafile;

import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;

import edu.harvard.iq.dataverse.persistence.JpaEntity;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;

/**
 * @author Leonid Andreev
 */

@Entity
@Table(indexes = {@Index(columnList = "dataset_id")})
public class DataFileCategory implements Serializable, JpaEntity<Long> {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @ManyToOne
    @JoinColumn(nullable = false)
    private Dataset dataset;
    @ManyToMany(mappedBy = "fileCategories")
    private Collection<FileMetadata> fileMetadatas = new ArrayList<>();

    protected DataFileCategory() {}
    
    public DataFileCategory(String name) {
        Preconditions.checkArgument(StringUtils.isNotEmpty(name));
        this.name = name;
    }

    @Override
    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Dataset getDataset() {
        return this.dataset;
    }

    public void setDataset(final Dataset dataset) {
        this.dataset = dataset;
    }

    public String getName() {
        return this.name;
    }

    public Collection<FileMetadata> getFileMetadatas() {
        return this.fileMetadatas;
    }

    public void addFileMetadata(final FileMetadata fileMetadata) {
        getFileMetadatas().add(fileMetadata);
    }

    public void removeFileMetadatas(final List<FileMetadata> fileMetadatas) {
        this.fileMetadatas.removeAll(fileMetadatas);
    }


    @Override
    public int hashCode() {
        return Objects.hashCode(this.id);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof DataFileCategory)) {
            return false;
        }
        DataFileCategory other = (DataFileCategory) object;
        return (this.id != null || other.id == null) && (this.id == null || this.id.equals(other.id));
    }

    @Override
    public String toString() {
        return "DataFileCategory[ id=" + id + " ]";
    }
}
