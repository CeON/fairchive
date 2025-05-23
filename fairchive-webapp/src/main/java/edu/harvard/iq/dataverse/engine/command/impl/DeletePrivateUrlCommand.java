package edu.harvard.iq.dataverse.engine.command.impl;

import static edu.harvard.iq.dataverse.persistence.user.Permission.ManageDatasetPermissions;
import static edu.harvard.iq.dataverse.persistence.user.Permission.ManageMinorDatasetPermissions;

import java.util.List;

import edu.harvard.iq.dataverse.engine.command.AbstractVoidCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.user.PrivateUrlUser;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;

@SuppressWarnings("serial")
@RequiredPermissions(value = {ManageDatasetPermissions, ManageMinorDatasetPermissions}, isAllPermissionsRequired = false)
public class DeletePrivateUrlCommand extends AbstractVoidCommand {

    private final Dataset dataset;
    private final boolean anonymized;

    
    public DeletePrivateUrlCommand(DataverseRequest aRequest, Dataset theDataset, boolean anonymized) {
        super(aRequest, theDataset);
        this.dataset = theDataset;
        this.anonymized = anonymized;
    }

    @Override
    protected void executeImpl(CommandContext ctxt)  {
        if (dataset == null) {
            /**
             * @todo Internationalize this.
             */
            String message = "Can't delete Private URL. Dataset is null.";
            throw new IllegalCommandException(message, this);
        }
        PrivateUrlUser privateUrlUser = new PrivateUrlUser(dataset.getId());
        List<RoleAssignment> roleAssignments = ctxt.roles().directRoleAssignments(privateUrlUser, dataset);
        for (RoleAssignment roleAssignment : roleAssignments) {
            if(roleAssignment.isAnonymized() == this.anonymized) {
                ctxt.engine().submit(new RevokeRoleCommand(roleAssignment, getRequest()));
            }
        }
    }

}
