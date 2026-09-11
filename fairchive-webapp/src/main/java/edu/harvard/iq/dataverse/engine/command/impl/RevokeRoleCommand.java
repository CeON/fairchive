package edu.harvard.iq.dataverse.engine.command.impl;

import com.google.common.collect.ImmutableSet;
import edu.harvard.iq.dataverse.authorization.DataverseRolePermissionHelper;
import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.datafile.DataFile;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;

import static edu.harvard.iq.dataverse.persistence.user.Permission.ManageDataset;
import static edu.harvard.iq.dataverse.persistence.user.Permission.ManageDataverse;
import static edu.harvard.iq.dataverse.persistence.user.Permission.ManageMinorDataset;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * Revokes a role for a user on a dataverse.
 *
 * @author michael
 */
// no annotations here, since permissions are dynamically decided
@SuppressWarnings("serial")
public class RevokeRoleCommand extends AbstractVoidCommand implements Serializable {

    private final RoleAssignment toBeRevoked;

    public RevokeRoleCommand(RoleAssignment toBeRevoked, DataverseRequest aRequest) {
        // for data file check permission on owning dataset
        super(aRequest, toBeRevoked.getDefinitionPoint() instanceof DataFile 
                            ? toBeRevoked.getDefinitionPoint().getOwner() 
                            : toBeRevoked.getDefinitionPoint());
        this.toBeRevoked = toBeRevoked;
    }

    @Override
    protected void executeImpl(CommandContext ctxt)  {
        ctxt.roles().revoke(toBeRevoked);
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        // for data file check permission on owning dataset

        if (toBeRevoked.getDefinitionPoint() instanceof Dataverse) {
            return singletonMap("", singleton(ManageDataverse));
        }
        if (DataverseRolePermissionHelper.getRolesAllowedToBeAssignedByManageMinorDatasetPermissions()
                .contains(toBeRevoked.getRole().getAlias())) {
            return singletonMap("", ImmutableSet.of(ManageDataset, ManageMinorDataset));
        }

        return singletonMap("", singleton(ManageDataset));
    }

    @Override
    public boolean isAllPermissionsRequired() {
        return false;
    }
}
