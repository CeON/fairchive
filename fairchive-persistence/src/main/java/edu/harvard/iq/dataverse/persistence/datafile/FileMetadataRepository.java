package edu.harvard.iq.dataverse.persistence.datafile;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.ejb.Stateless;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

@Stateless
public class FileMetadataRepository extends JpaRepository<Long, FileMetadata> {

    public FileMetadataRepository() {
        super(FileMetadata.class);
    }

    /**
     * Retrieves fileMetadata with pagination.
     *
     * @param pageNumber page number that starts with 0 (important for calculation).
     * @return List of fileMetadata
     */
    public List<FileMetadata> findFileMetadataByDatasetVersionId(final long versionId, 
    		final int pageNumber, final int maxResults) {
        return createQuery(
        		 	"SELECT f FROM FileMetadata f JOIN f.datasetVersion v " +
        		 	"WHERE v.id = :dsvId ORDER BY f.displayOrder")
                 .setParameter("dsvId", versionId)
                 .setFirstResult(pageNumber * maxResults)
                 .setMaxResults(maxResults)
                 .getResultList();
    }

    /**
     * Retrieves fileMetadata with pagination and search term.
     *
     * @param pageNumber page number that starts with 0 (important for calculation).
     * @return List of fileMetadata
     */
    public List<FileMetadata> findSearchedFileMetadataByDatasetVersionId(
    		final long versionId, final int pageNumber, final int maxResults, 
    		final String searchTerm) {
        return createQuery(
        			"SELECT f FROM FileMetadata f JOIN f.datasetVersion v " +
                    "WHERE v.id = :dsvId AND (lower(f.label) LIKE :searchTerm OR lower(f.description) LIKE :searchTerm) " +
                    "ORDER BY f.displayOrder asc")
                 .setParameter("dsvId", versionId)
                 .setParameter("searchTerm", "%" + searchTerm.toLowerCase() + "%")
                 .setFirstResult(pageNumber * maxResults)
                 .setMaxResults(maxResults)
                 .getResultList();
    }

    /**
     * Finds fileMetadata ids attached to dataset version.
     */
    public List<Long> findFileMetadataIdsByDatasetVersionId(final long versionId) {
        return this.em.createQuery(
        			"SELECT f.id FROM FileMetadata f JOIN f.datasetVersion v " +
                    "WHERE v.id = :dsvId", Long.class)
                 .setParameter("dsvId", versionId)
                 .getResultList();
    }

    /**
     * Finds files with provided id's along with cache enabled.
     */
    public List<FileMetadata> findFileMetadata(final Collection<Long> ids) {
        return createQuery(
        			"SELECT f FROM FileMetadata f WHERE f.id IN :fileMetadatas")
                 .setParameter("fileMetadatas", ids)
                 .setHint("eclipselink.QUERY_RESULTS_CACHE", "TRUE")
                 .getResultList();
    }

    /**
     * Finds files which are restricted by license.
     */
    public List<FileMetadata> findRestrictedFileMetadata(final Collection<Long> ids) {
        return createQuery(
        			"SELECT f FROM FileMetadata f JOIN f.termsOfUse t " +
                    "WHERE f.id IN :fileIds AND  t.restrictType != null")
                 .setParameter("fileIds", ids)
                 .getResultList();
    }
    
	public Optional<FileMetadata> findByDatasetVersionIdAndDataFileId(
			final Long datasetVersionId, final Long dataFileId) {
		return createQuery(
				"select o from FileMetadata o  " + 
		        "where o.datasetVersion.id = :datasetVersionId  and o.dataFile.id = :dataFileId")
				.setParameter("datasetVersionId", datasetVersionId)
				.setParameter("dataFileId", dataFileId)
				.getResultList()
				.stream()
				.findFirst();
	}
}
