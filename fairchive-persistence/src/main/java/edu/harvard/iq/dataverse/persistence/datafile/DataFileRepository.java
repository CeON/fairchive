package edu.harvard.iq.dataverse.persistence.datafile;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.ejb.Stateless;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

@Stateless
public class DataFileRepository extends JpaRepository<Long, DataFile> {

    // -------------------- CONSTRUCTORS --------------------

    public DataFileRepository() {
        super(DataFile.class);
    }
    
	public Optional<DataFile> findReplacementFile(final Long previousFileId) {
		return createQuery(
				"select f from DataFile f where f.previousDataFileId = :id")
				.setParameter("id", previousFileId)
				.getResultList()
				.stream()
				.findFirst();
	}
	
    public List<DataFile> findAllRelatedByRootDataFileId(final Long id) {
        return createQuery(
        		"select f from DataFile f where f.rootDataFileId = :id order by f.createDate")
                .setParameter("id", id)
                .getResultList();
    }
    
    public List<DataFile> findByFileMetadataIds(final Collection<Long> ids) {
        return createQuery(
        		"SELECT d FROM FileMetadata f JOIN f.dataFile d WHERE f.id IN :ids")
                .setParameter("ids", ids)
                .setHint("eclipselink.QUERY_RESULTS_CACHE", "TRUE")
                .getResultList();
    }
}
