package edu.harvard.iq.dataverse.engine.command.impl;

import java.util.UUID;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.engine.command.exception.IllegalCommandException;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole;
import edu.harvard.iq.dataverse.persistence.user.DataverseRole.BuiltInRole;
import edu.harvard.iq.dataverse.persistence.user.Permission;
import edu.harvard.iq.dataverse.persistence.user.PrivateUrlUser;
import edu.harvard.iq.dataverse.persistence.user.RoleAssignment;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;

@SuppressWarnings("serial")
@RequiredPermissions(value = {Permission.ManageDatasetPermissions, Permission.ManageMinorDatasetPermissions}, isAllPermissionsRequired = false)
public class CreatePrivateUrlCommand extends AbstractCommand<PrivateUrl> {

    final Dataset dataset;
    private final boolean anonymized;
    
    public CreatePrivateUrlCommand(DataverseRequest dataverseRequest, Dataset theDataset, boolean anonymized) {
        super(dataverseRequest, theDataset);
        this.dataset = theDataset;
        this.anonymized = anonymized;
    }

    @Override
    public PrivateUrl execute(CommandContext ctxt) {
        if (dataset == null) {
            /**
             * @todo Internationalize this.
             */
            String message = "Can't create Private URL. Dataset is null.";
            throw new IllegalCommandException(message, this);
        }
        PrivateUrl existing = ctxt.privateUrl().getPrivateUrlFromDatasetId(dataset.getId(), this.anonymized);
        if (existing != null) {
            /**
             * @todo Internationalize this.
             */
            String message = "Private URL already exists for dataset id " + dataset.getId() + ".";
            throw new IllegalCommandException(message, this);
        }
        PrivateUrlUser privateUrlUser = new PrivateUrlUser(dataset.getId());
        DataverseRole memberRole = ctxt.roles().findBuiltinRoleByAlias(BuiltInRole.MEMBER);
        final String privateUrlToken = UUID.randomUUID().toString();
        RoleAssignment roleAssignment = ctxt.engine().submit(new AssignRoleCommand(privateUrlUser, memberRole, dataset, getRequest(), privateUrlToken, this.anonymized));
        PrivateUrl privateUrl = new PrivateUrl(roleAssignment, dataset, ctxt.systemConfig().getDataverseSiteUrl());
        return privateUrl;
    }

}
