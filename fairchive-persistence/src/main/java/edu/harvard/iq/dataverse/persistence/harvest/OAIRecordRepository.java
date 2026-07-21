package edu.harvard.iq.dataverse.persistence.harvest;

import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.ejb.Stateless;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

@Stateless
public class OAIRecordRepository extends JpaRepository<Long, OAIRecord> {

    // -------------------- CONSTRUCTORS --------------------

    public OAIRecordRepository() {
        super(OAIRecord.class);
    }

    // -------------------- LOGIC --------------------

    public List<OAIRecord> findBySetName(final String name) {
    	
        return createQuery("SELECT r from OAIRecord r where r.setName = :name")
            .setParameter("name", name)
            .getResultList();
    }

    public List<OAIRecord> findBySetNameAndRemoved(final String name, 
    		final boolean removed) {
    	
        return createQuery(
        	"SELECT r from OAIRecord r where r.setName = :name and r.removed = :removed")
            .setParameter("name", name)
            .setParameter("removed", removed)
            .getResultList();
    }

    public List<OAIRecord> findByGlobalId(final String id) {
        return createQuery("SELECT r from OAIRecord r where r.globalId = :id")
                .setParameter("id", id)
                .getResultList();
    }

    public List<OAIRecord> findByGlobalIds(final List<String> ids) {
    	
    	if (ids.isEmpty()) {
    	    return emptyList();
    	} else {
	        return createQuery("SELECT r from OAIRecord r where r.globalId in :ids")
	                .setParameter("ids", ids)
	                .getResultList();
    	}
    }

    public Optional<Date> findEarliestDate() {
        final List<java.sql.Date> dates = this.em.createQuery(
        		"SELECT min(r.lastUpdateTime) FROM OAIRecord r", java.sql.Date.class)
                .getResultList();
        return dates.isEmpty() ? Optional.empty() : Optional.ofNullable(dates.get(0));
    }

    public List<OAIRecord> findBySetNameAndLastUpdateBetween(final String setName, 
    		final Date from, final Date until) {
    	
        final CriteriaBuilder builder = this.em.getCriteriaBuilder();

        final CriteriaQuery<OAIRecord> query = builder.createQuery(OAIRecord.class);
        final Root<OAIRecord> root = query.from(OAIRecord.class);

        final List<Predicate> predicates = new ArrayList<>();
        predicates.add(builder.equal(root.get("setName"), setName));

        if (from != null) {
            predicates.add(builder.greaterThanOrEqualTo(root.get("lastUpdateTime"), from));
        }
        if (until != null) {
            predicates.add(builder.lessThanOrEqualTo(root.get("lastUpdateTime"), until));
        }
        query.select(root)
            .where(predicates.toArray(new Predicate[]{}))
            .orderBy(builder.asc(root.get("globalId")));

        return this.em.createQuery(query).getResultList();
    }

    public void deleteBySetName(final String name) {
        createQuery("delete from OAIRecord hs where hs.setName = :name")
            .setParameter("name", name)
            .executeUpdate();
    }
}
