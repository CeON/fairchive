package edu.harvard.iq.dataverse.persistence.datafile;

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
}
