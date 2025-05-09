package edu.harvard.iq.dataverse.engine.command.impl;

import static edu.harvard.iq.dataverse.persistence.user.Permission.ManageDatasetPermissions;
import static edu.harvard.iq.dataverse.persistence.user.Permission.ManageMinorDatasetPermissions;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.privateurl.PrivateUrl;

@SuppressWarnings("serial")
@RequiredPermissions(value = {ManageDatasetPermissions, ManageMinorDatasetPermissions}, 
                     isAllPermissionsRequired = false)
public class GetPrivateUrlCommand extends AbstractCommand<PrivateUrl> {

    private final Dataset dataset;
    private final boolean anonymized;

    public GetPrivateUrlCommand(final DataverseRequest request,
            final Dataset dataset, final boolean anonymized) {
        super(request, dataset);
        this.dataset = dataset;
        this.anonymized = anonymized;
    }

    @Override
    public PrivateUrl execute(final CommandContext ctxt) {
        final Long datasetId = this.dataset.getId();
        if (datasetId != null) {
            return ctxt.privateUrl().getPrivateUrlFromDatasetId(datasetId,
                    this.anonymized);
        } else {
            return null; // Perhaps a dataset is being created in the GUI.
        }
    }

}
