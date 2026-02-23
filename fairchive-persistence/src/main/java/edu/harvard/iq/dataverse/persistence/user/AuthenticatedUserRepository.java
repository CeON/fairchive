package edu.harvard.iq.dataverse.persistence.user;

import static java.util.stream.Collectors.joining;

import java.util.List;
import java.util.stream.Stream;

import javax.ejb.Stateless;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import edu.harvard.iq.dataverse.persistence.JpaRepository;

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
    public List<AuthenticatedUser> find(final String searchTerm, final SortKey sortKey, 
    		final boolean isSortAscending, final int resultLimit, final int offset) {

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
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
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
    
	@SuppressWarnings("unchecked")
	public List<Object[]> getDirectRolesOf(final Stream<String> userIdentifiers) {
		final String identifiers = userIdentifiers
                .map(i -> "'@" + i + '\'')
                .collect(joining(","));

        return this.em.createNativeQuery(
        		"SELECT distinct a.assigneeidentifier, d.alias, d.name" +
                " FROM roleassignment a, dataverserole d" +
                " WHERE d.id = a.role_id" +
                " AND a.assigneeidentifier IN (" +
                identifiers +
                ") ORDER by a.assigneeidentifier, d.alias;")
        		.getResultList();
	}
	
	@SuppressWarnings("unchecked")
	public List<Object[]> getGroupsOf(final Stream<Long> userIds) {
		final String identifiers = userIds.
				map(String::valueOf).
				collect(joining(","));

        // A *RECURSIVE* native query, that finds all the groups that the specified
        // users are part of, BOTH by direct inclusion, AND via parent groups:
        return this.em.createNativeQuery("WITH RECURSIVE group_user AS ((" +
                " SELECT distinct g.groupalias, g.id, u.useridentifier" +
                "  FROM explicitgroup g, explicitgroup_authenticateduser e, authenticateduser u" +
                "  WHERE e.explicitgroup_id = g.id " +
                "   AND u.id IN (" + 
                identifiers + 
                "  ) AND u.id = e.containedauthenticatedusers_id)" +
                "  UNION\n" +
                "   SELECT p.groupalias, p.id, c.useridentifier" +
                "    FROM group_user c, explicitgroup p, explicitgroup_explicitgroup e" +
                "    WHERE e.explicitgroup_id = p.id" +
                "     AND e.containedexplicitgroups_id = c.id)" +
                "SELECT distinct groupalias, useridentifier FROM group_user;").
        		getResultList();
	}
	
	@SuppressWarnings("unchecked")
	public List<Object[]> getRolesOf(final Stream<String> groupIdentifiers) {
		final String identifiers = groupIdentifiers.collect(joining(","));
	
        return this.em.createNativeQuery(
        		"SELECT distinct a.assigneeidentifier, d.alias, d.name" +
	             " FROM roleassignment a, dataverserole d" +
	             " WHERE d.id = a.role_id" +
	             " AND a.assigneeidentifier IN (" +
	             identifiers +
	             " ) ORDER by a.assigneeidentifier, d.alias;").
        		getResultList();
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
