package edu.harvard.iq.dataverse.persistence.dataverse;

import javax.ejb.Stateless;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

@Stateless
public class DataverseFacetRepository extends JpaRepository<Long, DataverseFacet>{
	
    public DataverseFacetRepository() {
        super(DataverseFacet.class);
    }
}
