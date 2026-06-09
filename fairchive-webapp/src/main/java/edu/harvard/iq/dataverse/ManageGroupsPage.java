package edu.harvard.iq.dataverse;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static java.util.logging.Logger.getLogger;
import static javax.faces.application.FacesMessage.SEVERITY_ERROR;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;

import edu.harvard.iq.dataverse.authorization.groups.impl.explicit.ExplicitGroupServiceBean;
import edu.harvard.iq.dataverse.engine.command.impl.CreateExplicitGroupCommand;
import edu.harvard.iq.dataverse.engine.command.impl.DeleteExplicitGroupCommand;
import edu.harvard.iq.dataverse.engine.command.impl.UpdateExplicitGroupCommand;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.group.ExplicitGroup;
import edu.harvard.iq.dataverse.persistence.group.Group;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignee;
import edu.harvard.iq.dataverse.persistence.user.User;
import edu.harvard.iq.dataverse.util.JsfHelper;
import io.vavr.control.Try;

/**
 * @author michaelsuo
 */
@SuppressWarnings("serial")
@ViewScoped
@Named
public class ManageGroupsPage implements java.io.Serializable {

    private static final Logger logger = getLogger(ManageGroupsPage.class.getCanonicalName());

    private DataverseDao dataverseDao;
    private ExplicitGroupServiceBean explicitGroupService;
    private RoleAssigneeServiceBean roleAssigneeService;
    private PermissionsWrapper permissionsWrapper;
    private ManageGroupsCRUDService mgCrudService;


    private List<ExplicitGroup> explicitGroups;
    private Dataverse dataverse;
    private Long dataverseId;
    private ExplicitGroup selectedGroup;
    private List<RoleAssignee> selectedGroupRoleAssignees = new ArrayList<>();
    private List<RoleAssignee> selectedGroupAddRoleAssignees;

    /*
    ============================================================================
    Explicit Group dialogs
    ============================================================================
    */
    private String explicitGroupIdentifier = "";
    private String explicitGroupName = "";
    private String newExplicitGroupDescription = "";
    private List<RoleAssignee> newExplicitGroupRoleAssignees = new LinkedList<>();

    // -------------------- CONSTRUCTORS --------------------
    @Deprecated
    public ManageGroupsPage() {
    }

    @Inject
    public ManageGroupsPage(final DataverseDao dataverseDao, 
    						final ExplicitGroupServiceBean explicitGroupService,
                            final RoleAssigneeServiceBean roleAssigneeService, 
                            final PermissionsWrapper permissionsWrapper,
                            final ManageGroupsCRUDService mgCrudService) {
        this.dataverseDao = dataverseDao;
        this.explicitGroupService = explicitGroupService;
        this.roleAssigneeService = roleAssigneeService;
        this.permissionsWrapper = permissionsWrapper;
        this.mgCrudService = mgCrudService;
    }

    // -------------------- GETTERS --------------------
    public List<ExplicitGroup> getExplicitGroups() {
        return this.explicitGroups;
    }

    public Dataverse getDataverse() {
        return this.dataverse;
    }

    public Long getDataverseId() {
        return this.dataverseId;
    }

    public Group getSelectedGroup() {
        return this.selectedGroup;
    }

    public List<RoleAssignee> getSelectedGroupRoleAssignees() {
        return this.selectedGroupRoleAssignees;
    }

    public List<RoleAssignee> getSelectedGroupAddRoleAssignees() {
        return this.selectedGroupAddRoleAssignees;
    }

    public String getExplicitGroupName() {
        return this.explicitGroupName;
    }

    public String getExplicitGroupIdentifier() {
        return this.explicitGroupIdentifier;
    }

    public List<RoleAssignee> getNewExplicitGroupRoleAssignees() {
        return this.newExplicitGroupRoleAssignees;
    }

    public String getNewExplicitGroupDescription() {
        return this.newExplicitGroupDescription;
    }

    // -------------------- LOGIC --------------------
    public String init() {
        setDataverse(this.dataverseDao.find(getDataverseId()));
        Dataverse editDv = getDataverse();

        if (editDv == null) {
            return this.permissionsWrapper.notFound();
        }

        if (!this.permissionsWrapper.canIssueAnyOf(editDv, 
        		CreateExplicitGroupCommand.class, 
        		DeleteExplicitGroupCommand.class,
        		UpdateExplicitGroupCommand.class)) {
            return this.permissionsWrapper.notAuthorized();
        }
        
        this.explicitGroups = new LinkedList<>(this.explicitGroupService.findByOwnerId(getDataverseId()));
        this.selectedGroup = null;

        return null;
    }

    public void deleteGroup() {
        if (this.selectedGroup != null) {
            Try
                    .run(() -> this.mgCrudService.delete(this.selectedGroup))
                    .andThen(() -> {
                        this.explicitGroups.remove(this.selectedGroup);
                        JsfHelper.addSuccessMessage(getStringFromBundle("dataverse.manageGroups.delete"));
                    })
                    .onFailure(throwable -> {
                        String failMessage = getStringFromBundle("dataverse.manageGroups.nodelete");
                        JsfHelper.addErrorMessage(failMessage);
                    })
            ;
        } else {
            logger.info("Selected group is null");
        }
    }

    public void viewSelectedGroup(final ExplicitGroup selectedGroup) {
        this.selectedGroup = this.explicitGroupService.findByAlias(selectedGroup.getAlias());

        // initialize member list for autocomplete interface
        setSelectedGroupAddRoleAssignees(new LinkedList<>());
        setSelectedGroupRoleAssignees(getExplicitGroupMembers(selectedGroup));
    }

    /**
     * Return the set of all role assignees for an explicit group.
     * Does not traverse subgroups.
     *
     * @param eg The explicit group to check.
     * @return The set of role assignees belonging to explicit group.
     */
    public List<RoleAssignee> getExplicitGroupMembers(final ExplicitGroup eg) {
        return (eg != null) ?
                new ArrayList<>(this.explicitGroupService.getDirectMembers(eg)) : null;
    }

    /**
     * Return a string describing the type of a role assignee
     * TODO reference the bundle for localization
     *
     * @param ra The role assignee
     * @return A {@code String} representing the role assignee's type.
     */
    public String getRoleAssigneeTypeString(final RoleAssignee ra) {
        if (ra instanceof User) {
            return getStringFromBundle("dataverse.manageGroups.User");
        } else if (ra instanceof Group) {
            return getStringFromBundle("dataverse.manageGroups.Group");
        } else {
            return getStringFromBundle("dataverse.manageGroups.unknown");
        }
    }

    public String getMembershipString(final ExplicitGroup eg) {
        long userCount = 0;
        long groupCount = 0;
        for (RoleAssignee ra : this.explicitGroupService.getDirectMembers(eg)) {
            if (ra instanceof User) {
                userCount++;
            } else {
                groupCount++;
            }
        }

        if (userCount == 0 && groupCount == 0) {
            return getStringFromBundle("dataverse.manageGroups.nomembers");
        }

        String memberString = "";
        if (userCount == 1) {
            memberString = "1 " + getStringFromBundle("dataverse.manageGroups.user");
        } else if (userCount != 1) {
            memberString = userCount + " " + getStringFromBundle("dataverse.manageGroups.users");
        }

        if (groupCount == 1) {
            memberString = memberString + ", 1 " + getStringFromBundle("dataverse.manageGroups.group");
        } else if (groupCount != 1) {
            memberString = memberString + ", " + groupCount + " " + getStringFromBundle("dataverse.manageGroups.groups");
        }

        return memberString;
    }

    public void removeMemberFromSelectedGroup(final RoleAssignee ra) {
        this.selectedGroup.remove(ra);
        this.selectedGroupRoleAssignees.remove(ra);
    }

    public List<RoleAssignee> completeRoleAssignee(final String query) {

        final List<RoleAssignee> alreadyAssignedRoleAssignees = new ArrayList<>();

        if (this.getNewExplicitGroupRoleAssignees() != null) {
            alreadyAssignedRoleAssignees.addAll(this.getNewExplicitGroupRoleAssignees());
        }
        if (this.getSelectedGroupRoleAssignees() != null) {
            alreadyAssignedRoleAssignees.addAll(this.getSelectedGroupRoleAssignees());
        }
        if (this.getSelectedGroupAddRoleAssignees() != null) {
            alreadyAssignedRoleAssignees.addAll(this.getSelectedGroupAddRoleAssignees());
        }

        return this.roleAssigneeService.filterRoleAssignees(query, this.dataverse, 
        		alreadyAssignedRoleAssignees);

    }

    public void initExplicitGroupDialog(final ActionEvent ae) {
        setExplicitGroupName("");
        setExplicitGroupIdentifier("");
        setNewExplicitGroupDescription("");
        setNewExplicitGroupRoleAssignees(new LinkedList<>());
        setSelectedGroupRoleAssignees(null);
    }

    public void createExplicitGroup(ActionEvent ae) {
        Try.of(() -> this.mgCrudService.create(this.dataverse, this.explicitGroupName, 
        		this.explicitGroupIdentifier, this.newExplicitGroupDescription, 
        		this.newExplicitGroupRoleAssignees))
            .onSuccess((eg) -> {
                this.explicitGroups.add(eg.get());
                JsfHelper.addSuccessMessage(getStringFromBundle("dataverse.manageGroups.create.success", 
                		eg.get().getDisplayName()));
            })
            .onFailure(throwable -> {
                logger.log(Level.WARNING, "Group creation failed", throwable);
                JsfHelper.addErrorMessage(getStringFromBundle("dataverse.manageGroups.create.fail"), "");
            })
        ;
    }

    public void editExplicitGroup(final ActionEvent ae) {

        Try.of(() -> this.mgCrudService.update(this.selectedGroup, this.selectedGroupAddRoleAssignees))
            .onSuccess((eg) -> {
                JsfHelper.addFlashSuccessMessage(getStringFromBundle("dataverse.manageGroups.save.success", 
                		eg.get().getDisplayName()));
                this.explicitGroups.set(this.explicitGroups.indexOf(eg.get()), eg.get());
            })
            .onFailure(throwable -> JsfHelper.addErrorMessage(getStringFromBundle("dataverse.manageGroups.edit.fail"),
                    ""))
        ;
    }

    public void validateGroupIdentifier(final FacesContext context, 
    		final UIComponent toValidate, final Object rawValue) {
        final String value = (String) rawValue;
        final UIInput input = (UIInput) toValidate;
        input.setValid(true); // Optimistic approach

        if (!isEmpty(value)) {

            // cheap test - regex
            if (!Pattern.matches("^[a-zA-Z0-9\\_\\-]+$", value)) {
                input.setValid(false);
                context.addMessage(toValidate.getClientId(),
                        new FacesMessage(SEVERITY_ERROR, "", 
                        		getStringFromBundle("dataverse.permissions.explicitGroupEditDialog.groupIdentifier.invalid")));

            } else if (explicitGroupService.findByOwnerIdAndAlias(dataverse.getId(), value) != null) {
                // Ok, see that the alias is not taken
                input.setValid(false);
                context.addMessage(toValidate.getClientId(),
                        new FacesMessage(SEVERITY_ERROR, "", 
                        		getStringFromBundle("dataverse.permissions.explicitGroupEditDialog.groupIdentifier.taken")));
            }
        }
    }
    // -------------------- PRIVATE ---------------------

    // -------------------- SETTERS --------------------
    public void setSelectedGroup(final ExplicitGroup selectedGroup) {
        this.selectedGroup = selectedGroup;
    }

    public void setDataverse(final Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public void setDataverseId(final Long dataverseId) {
        this.dataverseId = dataverseId;
    }

    public void setSelectedGroupRoleAssignees(final List<RoleAssignee> assignees) {
        this.selectedGroupRoleAssignees = assignees;
    }

    public void setSelectedGroupAddRoleAssignees(final List<RoleAssignee> assignees) {
        this.selectedGroupAddRoleAssignees = assignees;
    }

    public void setExplicitGroupName(final String name) {
        this.explicitGroupName = name;
    }

    public void setExplicitGroupIdentifier(final String name) {
        this.explicitGroupIdentifier = name;
    }

    public void setNewExplicitGroupRoleAssignees(final List<RoleAssignee> assignees) {
        this.newExplicitGroupRoleAssignees = assignees;
    }

    public void setNewExplicitGroupDescription(final String description) {
        this.newExplicitGroupDescription = description;
    }
}
