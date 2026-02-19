package edu.harvard.iq.dataverse;

import static java.time.Instant.now;
import static java.util.Collections.emptyList;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;

import edu.harvard.iq.dataverse.common.RoleTranslationUtil;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserRepository;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserRepository.SortKey;

@Stateless
public class UserServiceBean {

    @PersistenceContext
    private EntityManager em;

    @Inject
    private AuthenticatedUserRepository repo;

    // -------------------- LOGIC --------------------

    public AuthenticatedUser getById(long id) {
        return this.repo.getById(id);
    }

    public AuthenticatedUser save(final AuthenticatedUser user) {
        if (user.getCreatedTime() == null) {
            user.setCreatedTime(Timestamp.from(now()));
        } 
        if(user.getLastLoginTime() == null) {
        	 user.setLastLoginTime(user.getCreatedTime());
        }
        return this.repo.save(user);
    }

    /**
     * Return the user information as a List of AuthenticatedUser objects -- easier to work with in the UI
     * - With Role added as a transient field
     */
    public List<AuthenticatedUser> find(final String searchTerm, 
    			final String sortKey, final  boolean isSortAscending,
    			final Integer resultLimit, final Integer offset) {

        final List<AuthenticatedUser> users = this.repo.find(searchTerm, 
        		SortKey.fromString(sortKey), isSortAscending,
        		sanitize(resultLimit, 1), sanitize(offset, 0));

        Map<String, List<String>> roleLookup = retrieveRolesForUsers(users);

        List<AuthenticatedUser> viewObjects = new ArrayList<>();
        for (AuthenticatedUser user : users) {
            List<String> roleList = roleLookup.getOrDefault("@" + user.getUserIdentifier(), emptyList());
            user.setRoles(String.join(", ", roleList));
            viewObjects.add(user);
        }

        return viewObjects;
    }


    public Long countSuperUsers() {
        return this.repo.countSuperUsers();
    }

    public Long countUsers() {
        return countUsers("");
    }

    public Long countUsers(final String searchTerm) {
        return this.repo.countUsers(searchTerm);
    }

    public AuthenticatedUser updateLastLogin(final AuthenticatedUser user) {
        //assumes that AuthenticatedUser user already exists
        user.setLastLoginTime(Timestamp.from(now()));
        return save(user);
    }

    public AuthenticatedUser updateLastApiUseTime(final AuthenticatedUser user) {
        //assumes that AuthenticatedUser user already exists
        user.setLastApiUseTime(Timestamp.from(now()));
        return save(user);
    }
    
    private static Integer sanitize(final Integer value, final int minimum) {
    	return value == null || value < minimum
    			? Integer.valueOf(minimum) 
    			: value;
    }
    

    // -------------------- PRIVATE --------------------

    /**
     * Attempt to retrieve all the user roles in 1 query
     * Consider putting limits on this -- e.g. no more than 1,000 user identifiers or something similar
     */
    @SuppressWarnings("unchecked")
    private Map<String, List<String>> retrieveRolesForUsers(List<AuthenticatedUser> userObjectList) {
        // Iterate through results, retrieving only the assignee identifiers
        // Note: userInfo[1], the assigneeIdentifier, cannot be null in the database
        List<String> userIdentifierList = userObjectList.stream()
                .map(AuthenticatedUser::getUserIdentifier)
                .collect(Collectors.toList());

        if (userIdentifierList.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> databaseIds = userObjectList.stream()
                .map(AuthenticatedUser::getId)
                .collect(Collectors.toList());

        // -------------------------------------------------
        // Note: This is not ideal but in this case SQL
        // injection isn't possible b/c the list of assigneeidentifier
        // strings comes from a previous query
        // -------------------------------------------------
        String identifierListString = userIdentifierList.stream()
                .filter(StringUtils::isNotEmpty)
                .map(i -> "'@" + i + "'")
                .collect(Collectors.joining(", "));

        // Create/Run the query to find directly assigned roles
        String qstr = "SELECT distinct a.assigneeidentifier, d.alias, d.name"
              + " FROM roleassignment a, dataverserole d"
              + " WHERE d.id = a.role_id"
              + " AND a.assigneeidentifier IN (" + identifierListString + ")"
              + " ORDER by a.assigneeidentifier, d.alias;";

        Query nativeQuery = em.createNativeQuery(qstr);

        List<Object[]> dbRoleResults = nativeQuery.getResultList();

        Map<String, List<String>> userRoleLookup = new HashMap<>();

        String userIdentifier;
        String userRole;
        for (Object[] dbResultRow : dbRoleResults) {
            userIdentifier = (String) dbResultRow[0];
            userRole = RoleTranslationUtil.getLocaleNameFromAlias((String) dbResultRow[1], (String) dbResultRow[2]);
            List<String> userRoleList = userRoleLookup.getOrDefault(userIdentifier, new ArrayList<>());
            if (!userRoleList.contains(userRole)) {
                userRoleList.add(userRole);
                userRoleLookup.put(userIdentifier, userRoleList);
            }
        }

        // And now the roles assigned via groups:
        // 1. One query for selecting all the groups to which these users may belong:

        Map<String, List<String>> groupsLookup = new HashMap<>();
        String idListString = StringUtils.join(databaseIds, ",");

        // A *RECURSIVE* native query, that finds all the groups that the specified
        // users are part of, BOTH by direct inclusion, AND via parent groups:

        qstr = "WITH RECURSIVE group_user AS ((" +
                " SELECT distinct g.groupalias, g.id, u.useridentifier" +
                "  FROM explicitgroup g, explicitgroup_authenticateduser e, authenticateduser u" +
                "  WHERE e.explicitgroup_id = g.id " +
                "   AND u.id IN (" + idListString + ")" +
                "   AND u.id = e.containedauthenticatedusers_id)" +
                "  UNION\n" +
                "   SELECT p.groupalias, p.id, c.useridentifier" +
                "    FROM group_user c, explicitgroup p, explicitgroup_explicitgroup e" +
                "    WHERE e.explicitgroup_id = p.id" +
                "     AND e.containedexplicitgroups_id = c.id)" +
                "SELECT distinct groupalias, useridentifier FROM group_user;";

        nativeQuery = em.createNativeQuery(qstr);
        List<Object[]> groupResults = nativeQuery.getResultList();

        Set<String> groupIdentifiers = new HashSet<>();

        for (Object[] group : groupResults) {
            String alias = (String) group[0];
            String user = (String) group[1];
            if (alias == null) {
                continue;
            }

            alias = "&explicit/" + alias;
            groupIdentifiers.add("'" + alias + "'");

            List<String> groupUserList = groupsLookup.getOrDefault(alias, new ArrayList<>());
            if (!groupUserList.contains(user)) {
                groupUserList.add(user);
                groupsLookup.put(alias, groupUserList);
            }
        }

        // 2. And now we can make another lookup on the roleassignment table, using the list
        // of the explicit group aliases we have just generated:

        if (groupIdentifiers.isEmpty()) {
            return userRoleLookup;
        }

        qstr = "SELECT distinct a.assigneeidentifier, d.alias, d.name"
             + " FROM roleassignment a, dataverserole d"
             + " WHERE d.id = a.role_id"
             + " AND a.assigneeidentifier IN (" + String.join(", ", groupIdentifiers) + ")"
             + " ORDER by a.assigneeidentifier, d.alias;";

        nativeQuery = em.createNativeQuery(qstr);
        dbRoleResults = nativeQuery.getResultList();
        if (dbRoleResults == null) {
            return userRoleLookup;
        }

        for (Object[] dbResultRow : dbRoleResults) {
            String groupIdentifier = (String) dbResultRow[0];
            String groupRole = RoleTranslationUtil.getLocaleNameFromAlias((String) dbResultRow[1], (String) dbResultRow[2]);
            List<String> groupUserList = groupsLookup.getOrDefault(groupIdentifier, Collections.emptyList());
            for (String groupUserIdentifier : groupUserList) {
                groupUserIdentifier = "@" + groupUserIdentifier;
                List<String> userRoleList = userRoleLookup.getOrDefault(groupUserIdentifier, new ArrayList<>());
                if (!userRoleList.contains(groupRole)) {
                    userRoleList.add(groupRole);
                    userRoleLookup.put(groupUserIdentifier, userRoleList);
                }
            }
        }
        return userRoleLookup;
    }


}
