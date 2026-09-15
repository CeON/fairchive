/*
 *  (C) Michael Bar-Sinai
 */
package edu.harvard.iq.dataverse.engine.command.impl;

import static edu.harvard.iq.dataverse.authorization.DataverseRolePermissionHelper.getRolesAllowedToBeAssignedByManageMinorDatasetPermissions;
import static edu.harvard.iq.dataverse.persistence.user.Permission.ManageDataset;
import static edu.harvard.iq.dataverse.persistence.user.Permission.ManageDataverse;
import static edu.harvard.iq.dataverse.persistence.user.Permission.ManageMinorDataset;
import static edu.harvard.iq.dataverse.persistence.user.Permission.MatchStrategy.atLeastOneRequired;
import static java.util.Collections.singletonMap;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignee;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;

/**
 * Assign a in a dataverse to a user.
 *
 * @author michael
 */
@SuppressWarnings("serial")
@RequiredPermissions(strategy = atLeastOneRequired) //permissions are dynamically decided
public class AssignRoleCommand extends AbstractCommand<RoleAssignment> implements Serializable {

    private final DataverseRole role;
    private final RoleAssignee grantee;
    private final DvObject defPoint;
    private final String privateUrlToken;
    private final boolean anonymized;
    private final boolean skipPermissionsCheck;

    /**
     * @param assignee      The user being granted the role
     * @param role           the role being granted to the user
     * @param assignmentPoint the dataverse on which the role is granted.
     * @param request
     * @param privateUrlToken An optional token used by the Private Url feature.
     */
    public AssignRoleCommand(final RoleAssignee assignee, 
    		final DataverseRole role, final DvObject assignmentPoint, 
    		final DataverseRequest request, final String privateUrlToken) {
    	
        this(assignee, role, assignmentPoint, request, privateUrlToken, false, false);
    }
    
    public AssignRoleCommand(final RoleAssignee assignee, final DataverseRole role, 
    		final DvObject assignmentPoint, final DataverseRequest request, 
    		final String privateUrlToken, final boolean anonymized, 
    		final  boolean skipPermissionsCheck) {
    	
        // for data file check permission on owning dataset
        super(request, assignmentPoint instanceof DataFile 
        		? assignmentPoint.getOwner() : assignmentPoint);
        this.role = role;
        grantee = assignee;
        defPoint = assignmentPoint;
        this.privateUrlToken = privateUrlToken;
        this.anonymized = anonymized;
        this.skipPermissionsCheck = skipPermissionsCheck;
    }

    @Override
    public RoleAssignment execute(final CommandContext ctxt) {
        // TODO make sure the role is defined on the dataverse.
        final RoleAssignment roleAssignment = new RoleAssignment(this.role, 
        		this.grantee, this.defPoint, this.privateUrlToken, this.anonymized);
        return ctxt.roles().save(roleAssignment);
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        // for data file check permission on owning dataset

        if (this.defPoint instanceof Dataverse) {
        	return singletonMap("", this.skipPermissionsCheck 
        								? Permission.none()	
        								: Permission.setOf(ManageDataverse));
        }

        if (getRolesAllowedToBeAssignedByManageMinorDatasetPermissions().contains(this.role.getAlias())) {
            return singletonMap("", Permission.setOf(ManageDataset, ManageMinorDataset));
        }

        return singletonMap("", Permission.setOf(ManageDataset));

    }

    @Override
    public String describe() {
        return this.grantee + " has been given " + this.role + " on " + 
        		this.defPoint.accept(DvObject.NameIdPrinter);
    }
}
