package edu.harvard.iq.dataverse.persistence.dataverse;

import java.util.List;

import javax.ejb.Stateless;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

@Stateless
public class DataverseFacetRepository extends JpaRepository<Long, DataverseFacet>{
	
    public DataverseFacetRepository() {
        super(DataverseFacet.class);
    }
    
	public List<DataverseFacet> findByDataverseId(final Long dataverseId) {
		return createQuery(
				"select f from DataverseFacet f " + 
				"where f.dataverse.id = :id order by f.displayOrder").
				setParameter("id", dataverseId).
				getResultList();
	}
    
	public void deleteFor(final Long dataverseId) {
		createQuery("DELETE FROM DataverseFacet f WHERE f.dataverse.id=:id").
			setParameter("id", dataverseId).
			executeUpdate();
	}
}
