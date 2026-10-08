package edu.harvard.iq.dataverse.engine.command.impl;

import static edu.harvard.iq.dataverse.persistence.user.DataverseRole.BuiltInRole.ADMIN;
import static edu.harvard.iq.dataverse.persistence.user.DataverseRole.BuiltInRole.COLLECTION_CUSTODIAN;
import static edu.harvard.iq.dataverse.persistence.user.DataverseRole.BuiltInRole.DEPOSITOR;
import static edu.harvard.iq.dataverse.persistence.user.DataverseRole.BuiltInRole.DS_CONTRIBUTOR;
import static edu.harvard.iq.dataverse.persistence.user.DataverseRole.BuiltInRole.EDITOR;
import static edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key.InheritParentRoleAssignments;
import static java.time.Instant.now;
import static java.util.Collections.emptyList;

import java.sql.Timestamp;
import java.util.List;
import java.util.function.Predicate;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFacet;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFieldTypeInputLevel;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseVersion;
import edu.harvard.iq.dataverse.persistence.group.AuthenticatedUsers;
import edu.harvard.iq.dataverse.persistence.group.Group;
import edu.harvard.iq.dataverse.persistence.user.AuthenticatedUser;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import edu.harvard.iq.dataverse.persistence.user.User;

@SuppressWarnings("serial")
@RequiredPermissions(Permission.AddDataverse)
public class CreateDataverseCommand extends AbstractCommand<Dataverse> {

    private final Dataverse created;
    private final List<DataverseFieldTypeInputLevel> inputLevels;
    private final List<DatasetFieldType> facets;

    public CreateDataverseCommand(final Dataverse created, 
    		final DataverseRequest request, final List<DatasetFieldType> facets,
            final List<DataverseFieldTypeInputLevel> inputLevels) {
    	
        super(request, created.getOwner());
        this.created = created;
        this.facets = facets != null ? facets : emptyList();
        this.inputLevels = inputLevels != null ? inputLevels : emptyList();
    }

    @Override
    public Dataverse execute(final CommandContext context)  {

        if (this.created.isRoot() && context.dataverses().isRootDataverseExists()) {
            throw new IllegalCommandException(
            		"Root Dataverse already exists. Cannot create another one", this);
        }
        
        final Timestamp now = Timestamp.from(now());

        if (this.created.getCreateDate() == null) {
            this.created.setCreateDate(now);
        }

        if (this.created.getCreator() == null) {
            final User user = getRequest().getUser();
            if (user.isAuthenticated()) {
                this.created.setCreator((AuthenticatedUser) user);
            } else {
                throw new IllegalCommandException("Guest users cannot create a Dataverse.", this);
            }
        }
        
        setDefaultContributorRoles(context);

        // @todo for now we are saying all dataverses are permission root
        this.created.setPermissionRoot(true);

        if (context.dataverses().findByAlias(this.created.getAlias()) != null) {
            throw new IllegalCommandException("A dataverse with alias " + 
            		this.created.getAlias() + " already exists", this);
        }

        // Save the dataverse
        final Dataverse managedDv = context.dataverses().save(this.created);
        final String privateUrlToken = null;

        context.roles().save(new RoleAssignment(this.created.getDefaultDataverseContributorRole(), 
        		managedDv.getCreator() , managedDv, privateUrlToken));
        
        if(this.created.getDefaultDataverseContributorRole().is(COLLECTION_CUSTODIAN)) {
        	final boolean skipPermissions = skipPermissionsCheck();
        	context.getManagePermissionsService().assignRoleWithNotification(
        			role(context, DS_CONTRIBUTOR), AuthenticatedUsers.get(), 
        			managedDv, skipPermissions);
        }
        
        // Add additional role assignments if inheritance is set
        final List<String> rolesToInherit = context.settings().
        		getValueForKeyAsList(InheritParentRoleAssignments);
        if (!rolesToInherit.isEmpty()) {
        	final boolean inheritAllRoles = rolesToInherit.contains("*");
            for (final RoleAssignment role : context.roles().directRoleAssignments(this.created.getOwner())) {
                //Only supporting built-in/non-dataverse-specific custom roles. Custom roles all have an owner.
                if (role.getRole().getOwner() == null) {
                    // And... If all roles are to be inherited, or this role is in the list, and, in both
                    // cases, this is not an admin role for the current user which was just created
                    // above...
                    if ((inheritAllRoles || rolesToInherit.contains(role.getRole().getAlias()))
                            && !(role.getAssigneeIdentifier().equals(getRequest().getUser().getIdentifier())
                            && role.getRole().is(ADMIN))) {
                        String identifier = role.getAssigneeIdentifier();
                        if (identifier.startsWith(AuthenticatedUser.IDENTIFIER_PREFIX)) {
                            identifier = identifier.substring(AuthenticatedUser.IDENTIFIER_PREFIX.length());
                            context.roles().save(new RoleAssignment(role.getRole(),
                                                                 context.authentication().getAuthenticatedUser(identifier), managedDv, privateUrlToken));
                        } else if (identifier.startsWith(Group.IDENTIFIER_PREFIX)) {
                            identifier = identifier.substring(Group.IDENTIFIER_PREFIX.length());
                            Group roleGroup = context.groups().getGroup(identifier);
                            if (roleGroup != null) {
                                context.roles().save(new RoleAssignment(role.getRole(),
                                                                     roleGroup, managedDv, privateUrlToken));
                            }
                        }
                    }
                }
            }
        }
        
        if(this.created.getDefaultDataverseContributorRole().is(COLLECTION_CUSTODIAN)) {
	        final List<RoleAssignment> alreadyAssigned =  context.roles().
	        		findByDefinitionPointIdAndRoleAlias(managedDv.getId(), ADMIN.getAlias());
	        
	        final Predicate<RoleAssignment> notAssigned = 
	        		assignment -> ! alreadyAssigned.stream().anyMatch(assignment::isEquivalentTo);
	        
	        context.roles().findByDefinitionPointIdAndRoleAlias(this.created.getOwner().getId(), 
	        		ADMIN.getAlias())
	        	.stream()
	        	.filter(notAssigned)
	        	.map(assignment -> assignment.cloneForDefinitionPoint(managedDv))
	        	.forEach(context.roles()::save);
        }

        managedDv.setPermissionModificationTime(now);
        final Dataverse result = context.dataverses().save(managedDv);

        context.index().indexDataverse(result);
        addFacets(context);
        addDataverseFieldTypeInputLevel(context);
        return result;
    }

	private void setDefaultContributorRoles(final CommandContext context) {
		
		final Dataverse owner = this.created.getOwner();
        if(owner.getDefaultDataverseContributorRole() != null 
        		&& owner.getDefaultDataverseContributorRole().is(COLLECTION_CUSTODIAN)) {
        	this.created.setDefaultDatasetContributorRole(role(context, DEPOSITOR));
        }

        if (this.created.getDefaultDatasetContributorRole() == null) {
            this.created.setDefaultDatasetContributorRole(role(context, EDITOR));
        }
        
        if(this.created.getDefaultDataverseContributorRole() != null &&
        		this.created.getDefaultDataverseContributorRole().is(COLLECTION_CUSTODIAN)) {
        	this.created.setDefaultDataverseContributorRole(role(context, DEPOSITOR));
        } else {
        	this.created.setDefaultDataverseContributorRole(
        			owner.getDefaultDataverseContributorRole() != null 
        				? owner.getDefaultDataverseContributorRole()
        				: role(context, ADMIN));
        }
	}
	
	private static DataverseRole role(final CommandContext context, 
			final DataverseRole.BuiltInRole alias) {
		
		return context.roles().findBuiltinRoleByAlias(alias);
	}
	
	private boolean skipPermissionsCheck() {
		if (this.created.isRoot()) {
			return false;
		} 
		final Dataverse parent = (Dataverse) this.created.getOwner();
		if(parent.getDefaultDataverseContributorRole() == null) {
			return false;
		}
		return parent.getDefaultDataverseContributorRole().is(COLLECTION_CUSTODIAN);
		
	}
	
	private void addFacets(final CommandContext context) {
        final DataverseVersion version = this.created.getVersions().get(0);
        for(int index = 0; index < this.facets.size(); ++index) {
        	final DataverseFacet facet = version.addFacet(this.facets.get(index));
        	facet.setDisplayOrder(index);
        	context.facets().save(facet);
        }
	}
	
	private void addDataverseFieldTypeInputLevel(final CommandContext context) {
		final DataverseVersion version = this.created.getVersions().get(0);
		for(final DataverseFieldTypeInputLevel level : this.inputLevels) {
			version.addDataverseFieldTypeInputLevel(level);
			context.inputLevels().save(level);
		}
	}
}
