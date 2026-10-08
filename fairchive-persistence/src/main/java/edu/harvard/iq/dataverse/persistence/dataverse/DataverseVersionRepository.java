package edu.harvard.iq.dataverse.persistence.dataverse;

import javax.ejb.Stateless;

import edu.harvard.iq.dataverse.persistence.JpaRepository;
@Stateless
public class DataverseVersionRepository extends JpaRepository<Long, DataverseVersion> {

    public DataverseVersionRepository() {
        super(DataverseVersion.class);
    }

}
