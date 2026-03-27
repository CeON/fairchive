package edu.harvard.iq.dataverse.persistence.datafile;

import java.util.Optional;

import javax.ejb.Singleton;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

@Singleton
public class DataFileRepository extends JpaRepository<Long, DataFile> {

    // -------------------- CONSTRUCTORS --------------------

    public DataFileRepository() {
        super(DataFile.class);
    }
    
	public Optional<DataFile> findReplacementFile(final Long previousFileId) {
		return getSingleResult(this.em
				.createQuery("select f from DataFile as f where f.previousDataFileId = :id", 
						DataFile.class)
				.setParameter("id", previousFileId));
	}
}
