package edu.harvard.iq.dataverse;

import static edu.harvard.iq.dataverse.common.RoleTranslationUtil.getLocaleNameFromAlias;
import static java.lang.String.join;
import static java.time.Instant.now;
import static java.util.Collections.emptyList;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserRepository;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserRepository.SortKey;

@Stateless
public class UserServiceBean {

    @Inject
    private AuthenticatedUserRepository repo;

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
        setRoles(users);
        return users;
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
    
    public List<AuthenticatedUser> findSuperUsers() {
        return this.repo.findSuperUsers();
    }
    
    public Optional<AuthenticatedUser> findByEmail(final String email) {
    	return this.repo.findByEmail(email);
    }
    
    public Optional<AuthenticatedUser> findByIdentifier(final String identifier) {
    	return this.repo.findByIdentifier(identifier);
    }
    
    public long countByIdentifier(final String identifier) {
    	return this.countByIdentifier(identifier);
    }
    
	public List<AuthenticatedUser> findAll() {
		return this.repo.findAll();
	}
	
	public AuthenticatedUser getAdmin() {
		return this.repo.getAdmin();
	}
    
    private static int sanitize(final Integer value, final int minimum) {
    	return value == null || value < minimum
    			? minimum
    			: value;
    }

    private List<Object[]> getDirectRolesOf(final Collection<AuthenticatedUser> users) {
    	return this.repo.getDirectRolesOf(
    			users.stream().
                map(AuthenticatedUser::getUserIdentifier).
                filter(StringUtils::isNotEmpty));
    }
    
    private void setRoles(final Collection<AuthenticatedUser> users) {
    	
    	final RoleNames roleLookup = retrieveRolesForUsers(users);
        for (final AuthenticatedUser user : users) {
            user.setRoles(roleLookup.get(user.getUserIdentifier()));
        }
    }
    
    private List<Object[]> getGroupsOf(final Collection<AuthenticatedUser> users) {
    	return this.repo.getGroupsOf(
    			users.stream().
                map(AuthenticatedUser::getId));
    }
    
    private List<Object[]> getRolesOf(final Collection<String> groupIdentifiers) {
    	return this.repo.getRolesOf(groupIdentifiers.stream());
    }

    /**
     * Attempt to retrieve all the user roles in 1 query
     * Consider putting limits on this -- e.g. no more than 1,000 user identifiers or something similar
     */
    private RoleNames retrieveRolesForUsers(Collection<AuthenticatedUser> users) {
        final RoleNames result = new RoleNames();
    	
        if (!users.isEmpty()) {
        	
	        for (final Object[] row : getDirectRolesOf(users)) {
	            final String userIdentifier = (String) row[0];
	            final String userRole = getLocaleNameFromAlias((String) row[1], (String) row[2]);
	            
	            result.add(userIdentifier, userRole);
	        }
	
	        // And now the roles assigned via groups:
	        // 1. One query for selecting all the groups to which these users may belong:
	
	        final Map<String, List<String>> groupsLookup = new HashMap<>();
	        final Set<String> groupIdentifiers = new HashSet<>();
	
	        for (final Object[] group : getGroupsOf(users)) {
	            String alias = (String) group[0];
	            final String user = (String) group[1];
	            if (alias != null) {
		            alias = "&explicit/".concat(alias);
		            groupIdentifiers.add("'" + alias + "'");
		
		            final List<String> groupUserList = groupsLookup.
		            		getOrDefault(alias, new ArrayList<>());
		            if (!groupUserList.contains(user)) {
		                groupUserList.add(user);
		                groupsLookup.put(alias, groupUserList);
		            }
	            }
	        }
	
	        // 2. And now we can make another lookup on the roleassignment table, using the list
	        // of the explicit group aliases we have just generated:
	
	        if (!groupIdentifiers.isEmpty()) {
		        for (final Object[] row : getRolesOf(groupIdentifiers)) {
		            final String groupIdentifier = (String) row[0];
		            final String groupRole = getLocaleNameFromAlias((String) row[1], (String) row[2]);
		            
		            for (final String groupUserIdentifier : groupsLookup.
		            		getOrDefault(groupIdentifier, emptyList())) {
		                result.add("@".concat(groupUserIdentifier), groupRole);
		            }
		        }
	        }
        }
        return result;
    }
    
    @SuppressWarnings("serial")
	private final static class RoleNames extends HashMap<String, List<String>> {
    	
    	void add(final String userIdentifier, final String roleName) {
    		final List<String> userRoleList = getOrDefault(userIdentifier, new ArrayList<>());
            if (!userRoleList.contains(roleName)) {
                userRoleList.add(roleName);
                put(userIdentifier, userRoleList);
            }
    	}
    	
    	String get(final String userIdentifier) {
    		final List<String> roleList = getOrDefault("@".concat(userIdentifier), emptyList());
            return join(", ", roleList);
    	}
    }
}
