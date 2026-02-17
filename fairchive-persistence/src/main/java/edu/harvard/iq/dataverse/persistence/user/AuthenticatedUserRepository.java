package edu.harvard.iq.dataverse.persistence.user;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

import javax.ejb.Stateless;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;

@Stateless
public class AuthenticatedUserRepository extends JpaRepository<Long, AuthenticatedUser> {

    // -------------------- CONSTRUCTORS --------------------

    public AuthenticatedUserRepository() {
        super(AuthenticatedUser.class);
    }

    // -------------------- LOGIC --------------------

    /**
     * Results of this query are used to build Authenticated User records.
     */
    public List<AuthenticatedUser> find(final SortKey sortKey, 
    		final int resultLimit, final int offset, final String searchTerm, 
    		final boolean isSortAscending) {

        final CriteriaBuilder builder = this.em.getCriteriaBuilder();
        final CriteriaQuery<AuthenticatedUser> query = builder.createQuery(AuthenticatedUser.class);
        final Root<AuthenticatedUser> root = query.from(AuthenticatedUser.class);
        root.fetch("authenticatedUserLookup");
        query.select(root)
                .where(prepareSearchPredicates(searchTerm, root, builder))
                .orderBy(isSortAscending 
                		? builder.asc(root.get(sortKey.text)) 
                		: builder.desc(root.get(sortKey.text)));
        
        return this.em.createQuery(query)
                .setFirstResult(offset)
                .setMaxResults(resultLimit)
                .getResultList();
    }


    /**
     * Retrieves number of authenticatedUsers for a search term.
     *
     * @return number of results for given search term
     */
    public Long countUsers(final String searchTerm) {

        final CriteriaBuilder builder = this.em.getCriteriaBuilder();
        final CriteriaQuery<Long> query = builder.createQuery(Long.class);
        final Root<AuthenticatedUser> root = query.from(AuthenticatedUser.class);
        query.select(builder.count(root))
                .where(prepareSearchPredicates(searchTerm, root, builder));

        return this.em.createQuery(query).getSingleResult();
    }

    // -------------------- PRIVATE --------------------

    private Predicate prepareSearchPredicates(String searchTerm, 
    		final Root<AuthenticatedUser> root, final CriteriaBuilder builder) {
        
        final Predicate notErased = builder.notLike(builder.upper(root.get("userIdentifier")), "ERASED%");
        if (searchTerm.isEmpty()) {
            return notErased;
        } else {
            searchTerm = searchTerm.toLowerCase().concat("%");
            return builder.and(
                builder.or(
                        builder.like(builder.lower(root.get("userIdentifier")), searchTerm),
                        builder.like(builder.lower(root.get("affiliation")), searchTerm),
                        builder.like(builder.lower(root.get("lastName")), searchTerm),
                        builder.like(builder.lower(root.get("email")), searchTerm)
                        ),
                notErased);
        }
    }
    
    public Long countSuperUsers() {
        return this.em.createQuery(
                "SELECT count(au) FROM AuthenticatedUser au WHERE au.superuser = true",
                Long.class).getSingleResult();
    }

    // -------------------- INNER CLASSES --------------------

    public enum SortKey {

        ID("id"),
        USER_IDENTIFIER("userIdentifier"),
        AFFILIATION("affiliation"),
        LASTNAME("lastName"),
        EMAIL("email"),
        SUPERUSER("superuser");

        private final String text;

        SortKey(final String text) {
            this.text = text;
        }

        public static SortKey fromString(final String text) {
            for (SortKey sortKey : SortKey.values()) {
                if (sortKey.text.equals(text)) {
                    return sortKey;
                }
            }
            return SortKey.ID;
        }

        @Override
        public String toString() {
            return this.text;
        }
    }
}
