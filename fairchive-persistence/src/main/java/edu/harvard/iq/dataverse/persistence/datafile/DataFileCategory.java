package edu.harvard.iq.dataverse.persistence.datafile;

import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
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

    public void setName(String name) {
        this.name = name;
    }

    public Collection<FileMetadata> getFileMetadatas() {
        if (this.fileMetadatas == null) {
            this.fileMetadatas = new ArrayList<>();
        }
        return this.fileMetadatas;
    }

    public void setFileMetadatas(final Collection<FileMetadata> fileMetadatas) {
        this.fileMetadatas = fileMetadatas;
    }

    public void addFileMetadata(final FileMetadata fileMetadata) {
        getFileMetadatas().add(fileMetadata);
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
