package edu.harvard.iq.dataverse.persistence.dataverse;

import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;

import java.util.Comparator;
import java.util.List;

import javax.ejb.Stateless;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

@Stateless
public class DataverseFeaturedDataverseRepository extends JpaRepository<Long, DataverseFeaturedDataverse> {

	private final static Comparator<DataverseFeaturedDataverse> byDisplayOrder  =
			comparingInt(DataverseFeaturedDataverse::getDisplayOrder);
	private final static Comparator<DataverseFeaturedDataverse> byName = 
			comparing(dfd -> dfd.getFeaturedDataverse().getName(), 
					  String.CASE_INSENSITIVE_ORDER);
	
    // -------------------- CONSTRUCTORS --------------------

    public DataverseFeaturedDataverseRepository() {
        super(DataverseFeaturedDataverse.class);
    }

    // -------------------- LOGIC --------------------

    public List<Dataverse> findByDataverseIdOrderByDisplayOrder(final Long dataverseId) {
    	 return findByDataverseId(dataverseId)
    			 .stream()
    			 .sorted(byDisplayOrder)
    			 .map(DataverseFeaturedDataverse::getFeaturedDataverse)
    			 .collect(toList());
    }

    public List<Dataverse> findByDataverseIdOrderByNameAsc(final Long dataverseId) {	
    	return findByDataverseId(dataverseId)
			 .stream()
			 .sorted(byName)
			 .map(DataverseFeaturedDataverse::getFeaturedDataverse)
			 .collect(toList());
    }

    public List<Dataverse> findByDataverseIdOrderByNameDesc(final Long dataverseId) {
    	return findByDataverseId(dataverseId)
			 .stream()
			 .sorted(byName.reversed())
			 .map(DataverseFeaturedDataverse::getFeaturedDataverse)
			 .collect(toList());
    }

    public List<Dataverse> findByDataversesBySorting(
    		final List<Dataverse.FeaturedDataversesSorting> sorting) {
        return this.em.createQuery("select distinct d.dataverse from DataverseFeaturedDataverse d " +
                        "where d.dataverse.featuredDataversesSorting in :sorting", Dataverse.class)
                .setParameter("sorting", sorting)
                .getResultList();
    }

    // -------------------- PRIVATE --------------------

    private List<DataverseFeaturedDataverse> findByDataverseId(final Long id) {
        return createQuery(
        		"select d from DataverseFeaturedDataverse d" + 
        		" where d.dataverse.id = :id")
                .setParameter("id", id)
                .getResultList();
    }
}
