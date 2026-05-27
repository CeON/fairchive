package edu.harvard.iq.dataverse;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.join;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.GroupServiceBean;
import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupServiceBean;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.mydata.MyDataFilterParams;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.group.AllUsers;
import edu.harvard.iq.dataverse.persistence.group.AuthenticatedUsers;
import edu.harvard.iq.dataverse.persistence.group.ExplicitGroup;
import edu.harvard.iq.dataverse.persistence.group.Group;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUserRepository;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.GuestUser;
import edu.harvard.iq.dataverse.persistence.user.PrivateUrlUser;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignee;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignmentRepository;
import edu.harvard.iq.dataverse.privateurl.PrivateUrlUtil;

/**
 * The place to obtain {@link RoleAssignee}s, based on their identifiers.
 *
 * @author michael
 */
@Stateless
public class RoleAssigneeServiceBean {

    @PersistenceContext(unitName = "VDCNet-ejbPU")
    private EntityManager em;

    @EJB
    AuthenticationServiceBean authenticationService;

    @EJB
    GroupServiceBean groupService;

    @EJB
    ExplicitGroupServiceBean explicitGroupService;

    @EJB
    DataverseRoleServiceBean dataverseRoleService;
    
    @EJB
    RoleAssignmentRepository roleAssignmentRepository;
    
    @EJB
    AuthenticatedUserRepository authenticatedUserRepo;

    protected Map<String, RoleAssignee> predefinedRoleAssignees = new TreeMap<>();

    @PostConstruct
    protected void setup() {
        final GuestUser gu = GuestUser.get();
        this.predefinedRoleAssignees.put(gu.getIdentifier(), gu);
        this.predefinedRoleAssignees.put(AuthenticatedUsers.get().getIdentifier(), AuthenticatedUsers.get());
        this.predefinedRoleAssignees.put(AllUsers.get().getIdentifier(), AllUsers.get());
    }

    /**
     * @param identifier An identifier beginning with ":" (builtin), "@"
     *                   ({@link AuthenticatedUser}), "&" ({@link Group}), or "#"
     *                   ({@link PrivateUrlUser}).
     * @return A RoleAssignee (User or Group) or null.
     * @throws IllegalArgumentException if you pass null, empty string, or an
     *                                  identifier that doesn't start with one of the supported characters.
     */
    public RoleAssignee getRoleAssignee(final String identifier) {
        if (isEmpty(identifier)) {
            throw new IllegalArgumentException("Identifier cannot be null or empty string.");
        }
        switch (identifier.charAt(0)) {
            case ':':
                return this.predefinedRoleAssignees.get(identifier);
            case '@':
                return this.authenticationService.getAuthenticatedUser(identifier.substring(1));
            case '&':
                return this.groupService.getGroup(identifier.substring(1));
            case '#':
                return PrivateUrlUtil.identifier2roleAssignee(identifier);
            default:
                throw new IllegalArgumentException("Unsupported assignee identifier '"
                                                    + identifier + "'");
        }
    }

    public List<RoleAssignment> getAssignmentsFor(final String roleAssigneeIdentifier) {
    	return this.roleAssignmentRepository.findByAssigneeIdentifier(roleAssigneeIdentifier);
    }

    public Optional<RoleAssignment> findAssignmentFor(final String roleAssigneeIdentifier, 
            final Long definitionPointId, final Long roleId) {
        return this.roleAssignmentRepository.findAssignmentFor(roleAssigneeIdentifier, 
        		definitionPointId, roleId);
    }

    public List<AuthenticatedUser> getExplicitUsers(final RoleAssignee ra) {
        final List<AuthenticatedUser> explicitUsers = new ArrayList<>();
        if (ra instanceof AuthenticatedUser) {
            explicitUsers.add((AuthenticatedUser) ra);
        } else if (ra instanceof ExplicitGroup) {
            final ExplicitGroup group = (ExplicitGroup) ra;
            for (String raIdentifier : group.getContainedRoleAssgineeIdentifiers()) {
                explicitUsers.addAll(getExplicitUsers(getRoleAssignee(raIdentifier)));
            }
        }

        return explicitUsers;
    }

    private String getRoleIdListClause(final List<Long> roleIdList) {
        if (roleIdList == null) {
            return "";
        }
        final List<String> outputList = new ArrayList<>();

        for (final Long r : roleIdList) {
            if (r != null) {
                outputList.add(r.toString());
            }
        }
        if (outputList.isEmpty()) {
            return "";
        }
        return " AND r.role_id IN (" + join(outputList, ",") + ")";
    }

    public List<DataverseRole> getAssigneeDataverseRoleFor(final DataverseRequest dataverseRequest) {

        final AuthenticatedUser au = dataverseRequest.getAuthenticatedUser();
        if (au.getUserIdentifier() == null) {
            return null;
        }
        final List<DataverseRole> result = new ArrayList<>();
        final String roleAssigneeIdentifier = au.getIdentifier().replaceAll("\\s", "");   // remove spaces from string
        final List<String> userGroups = getUserExplicitGroups(au);
        final List<String> userRunTimeGroups = getUserRuntimeGroups(dataverseRequest);
        String identifierClause = " WHERE r.assigneeIdentifier= '" 
                                    + roleAssigneeIdentifier + "'";
        if (userGroups != null || userRunTimeGroups != null) {

            identifierClause = getGroupIdentifierClause(roleAssigneeIdentifier, 
                    userGroups, userRunTimeGroups);
        }

        final String qstr = "SELECT distinct r.role_id"
                + " FROM RoleAssignment r"
                + identifierClause
                + ";";

        for (Object o : this.em.createNativeQuery(qstr).getResultList()) {
            result.add(dataverseRoleService.find((Long) o));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getAssigneeAndRoleIdListFor(final MyDataFilterParams filterParams) {

        final AuthenticatedUser au = filterParams.getAuthenticatedUser();
        final List<Long> roleIdList = filterParams.getRoleIds();

        if (au.getUserIdentifier() == null) {
            return null;
        }
        final String roleAssigneeIdentifier = au.getIdentifier().replaceAll("\\s", "");   // remove spaces from string
        final List<String> userExplicitGroups = getUserExplicitGroups(au);
        final List<String> userRunTimeGroups = getUserRuntimeGroups(filterParams.getDataverseRequest());
        String identifierClause = " WHERE r.assigneeIdentifier= '" + roleAssigneeIdentifier + "'";
        if (userExplicitGroups != null || userRunTimeGroups != null) {
            identifierClause = getGroupIdentifierClause(roleAssigneeIdentifier, 
                    userExplicitGroups, userRunTimeGroups);
        }

        final String qstr = "SELECT r.definitionpoint_id, r.role_id"
                + " FROM RoleAssignment r"
                + identifierClause
                + getRoleIdListClause(roleIdList)
                + ";";
        return this.em.createNativeQuery(qstr).getResultList();

    }

    @SuppressWarnings("unchecked")
    public List<Long> getRoleIdListForGivenAssigneeDvObject(final DataverseRequest dataverseRequest, 
            final List<Long> roleIdList, final Long defPointId) {

        final AuthenticatedUser au = dataverseRequest.getAuthenticatedUser();
        if (au.getUserIdentifier() == null) {
            return null;
        }
        final String roleAssigneeIdentifier = au.getIdentifier().replaceAll("\\s", "");   // remove spaces from string
        final List<String> userGroups = getUserExplicitGroups(au);
        final List<String> userRunTimeGroups = getUserRuntimeGroups(dataverseRequest);
        String identifierClause = " WHERE r.assigneeIdentifier= '" + roleAssigneeIdentifier + "'";
        if (userGroups != null || userRunTimeGroups != null) {
            identifierClause = getGroupIdentifierClause(roleAssigneeIdentifier, userGroups, userRunTimeGroups);
        }

        final String qstr = "SELECT r.role_id"
                + " FROM RoleAssignment r"
                + identifierClause
                + getRoleIdListClause(roleIdList)
                + " and r.definitionpoint_id = " + defPointId
                + ";";

        return this.em.createNativeQuery(qstr).getResultList();

    }

    private String getGroupIdentifierClause(final String roleAssigneeIdentifier, 
            final List<String> userExplicitGroups, final List<String> userRunTimeGroups) {

        if (userExplicitGroups == null && userRunTimeGroups == null) {
            return "";
        }
        final List<String> outputExplicitList = new ArrayList<>();
        String explicitString = "";

        if (userExplicitGroups != null) {
            for (final String r : userExplicitGroups) {
                if (r != null) {
                    outputExplicitList.add(r);
                }
            }

            if (!outputExplicitList.isEmpty()) {
                explicitString = ",'&explicit/" + join(outputExplicitList, "','&explicit/") + "'";
            }

        }

        final List<String> outputRuntimeList = new ArrayList<>();
        String runTimeString = "";

        if (userRunTimeGroups != null) {
            for (String r : userRunTimeGroups) {
                if (r != null) {
                    outputRuntimeList.add(r);
                }
            }

            if (!outputRuntimeList.isEmpty()) {
                runTimeString = ",'" + join(outputRuntimeList, "','") + "'";
            }

        }
        return " WHERE r.assigneeIdentifier in ( '" + roleAssigneeIdentifier 
                + "'" + explicitString + runTimeString + ")";

    }

    @SuppressWarnings("unchecked")
    public List<Object[]> getRoleIdsFor(final DataverseRequest dataverseRequest, 
            final List<Long> dvObjectIdList) {
    	
        final AuthenticatedUser au = dataverseRequest.getAuthenticatedUser();
        if (au.getUserIdentifier() == null) {
            return null;
        }
        final String roleAssigneeIdentifier = au.getIdentifier().replaceAll("\\s", "");   // remove spaces from string
        final List<String> userGroups = getUserExplicitGroups(au);
        final List<String> userRunTimeGroups = getUserRuntimeGroups(dataverseRequest);
        String identifierClause = " WHERE r.assigneeIdentifier= '" + roleAssigneeIdentifier + "'";
        if (userGroups != null || userRunTimeGroups != null) {
            identifierClause = getGroupIdentifierClause(roleAssigneeIdentifier, 
                    userGroups, userRunTimeGroups);
        }

        final String qstr = "SELECT r.definitionpoint_id, r.role_id"
                + " FROM RoleAssignment r"
                + identifierClause
                + getDvObjectIdListClause(dvObjectIdList)
                + ";";

        return this.em.createNativeQuery(qstr).getResultList();

    }

    private String getDvObjectIdListClause(final List<Long> dvObjectIdList) {
        if (dvObjectIdList == null) {
            return "";
        }
        final List<String> outputList = new ArrayList<>();

        for (final Long r : dvObjectIdList) {
            if (r != null) {
                outputList.add(r.toString());
            }
        }
        if (outputList.isEmpty()) {
            return "";
        }
        return " AND r.definitionpoint_id IN (" + join(outputList, ",") + ")";
    }

    /**
     * @param ra
     * @return List of aliases of all explicit groups {@code ra} is in.
     * @todo Support groups within groups: https://github.com/IQSS/dataverse/issues/3056
     */
    public List<String> getUserExplicitGroups(final RoleAssignee ra) {
        return this.explicitGroupService.findGroups(ra).stream()
                .map(g -> g.getAlias())
                .collect(toList());
    }

    private List<String> getUserRuntimeGroups(final DataverseRequest dataverseRequest) {
        final List<String> result = new ArrayList<>();

        final Set<Group> groups = this.groupService.collectAncestors(this.groupService.groupsFor(dataverseRequest));
        for (final Group group : groups) {
            final String groupAlias = group.getAlias();
            if (groupAlias != null && !groupAlias.isEmpty()) {
            	final String prefix = group instanceof ExplicitGroup ? "&explicit/" : "&";
                result.add(prefix.concat(groupAlias));
            }
        }
        return result;
    }

    public List<RoleAssignee> filterRoleAssignees(final String query, final DvObject dvObject, 
            final List<RoleAssignee> roleAssignSelectedRoleAssignees) {
        final List<RoleAssignee> roleAssigneeList = new ArrayList<>();

        // we get the users through a query that does the filtering through the db,
        // so that we don't have to instantiate all of the RoleAssignee objects
        this.authenticatedUserRepo.findUsersByIdentifierOrName(query).stream()
                .filter(ra -> roleAssignSelectedRoleAssignees == null || !roleAssignSelectedRoleAssignees.contains(ra))
                .forEach(roleAssigneeList::add);

        // now we add groups to the list, both global and explicit
        final Set<Group> groups = this.groupService.findGlobalGroups();
        groups.addAll(this.explicitGroupService.findAvailableFor(dvObject));
        groups.stream()
                .filter(ra -> containsIgnoreCase(ra.getDisplayInfo().getTitle(), query)
                        || containsIgnoreCase(ra.getIdentifier(), query))
                .filter(ra -> roleAssignSelectedRoleAssignees == null || !roleAssignSelectedRoleAssignees.contains(ra))
                .forEach(roleAssigneeList::add);

        return roleAssigneeList;
    }

    public void removeAllRolesForUserByIdentifier(final String identifier) {
    	this.roleAssignmentRepository.deleteAllByAssigneeIdentifier(identifier);
    }
}
