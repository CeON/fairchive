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
    public List<AuthenticatedUser> findSearchedAuthenticatedUsers(SortKey sortKey, int resultLimit, int offset, String searchTerm, boolean isSortAscending) {

        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();

        CriteriaQuery<AuthenticatedUser> query = criteriaBuilder.createQuery(AuthenticatedUser.class);
        Root<AuthenticatedUser> root = query.from(AuthenticatedUser.class);
        root.fetch("authenticatedUserLookup");

        query.select(root)
                .where(getSearchPredicates(searchTerm, root, criteriaBuilder))
                .orderBy(isSortAscending ? criteriaBuilder.asc(root.get(sortKey.text)) : criteriaBuilder.desc(root.get(sortKey.text)));

        return em.createQuery(query)
                .setFirstResult(offset)
                .setMaxResults(resultLimit)
                .getResultList();
    }


    /**
     * Retrieves number of authenticatedUsers for a search term.
     *
     * @return number of results for given search term
     */
    public Long countSearchedAuthenticatedUsers(String searchTerm) {

        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();

        CriteriaQuery<Long> query = criteriaBuilder.createQuery(Long.class);
        Root<AuthenticatedUser> root = query.from(AuthenticatedUser.class);

        query.select(criteriaBuilder.count(root))
                .where(getSearchPredicates(searchTerm, root, criteriaBuilder));

        return em.createQuery(query).getSingleResult();
    }

    // -------------------- PRIVATE --------------------

    private Predicate getSearchPredicates(String searchTerm, Root<AuthenticatedUser> root, CriteriaBuilder criteriaBuilder) {
        
        final Predicate notErased = criteriaBuilder.notLike(criteriaBuilder.upper(root.get("userIdentifier")), "ERASED%");
        if (searchTerm.isEmpty()) {
            return notErased;
        } else {
            searchTerm = searchTerm.toLowerCase().concat("%");
            return criteriaBuilder.and(
                criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("userIdentifier")), searchTerm),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("affiliation")), searchTerm),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), searchTerm),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), searchTerm)
                        ),
                notErased);
        }
    }
    
    public Long getSuperUserCount() {
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
