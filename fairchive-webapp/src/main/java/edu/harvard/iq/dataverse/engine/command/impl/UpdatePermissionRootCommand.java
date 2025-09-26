package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.Permission;

import static edu.harvard.iq.dataverse.persistence.user.Permission.ManageDatasetPermissions;
import static edu.harvard.iq.dataverse.persistence.user.Permission.ManageDataversePermissions;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;

import java.util.Map;
import java.util.Set;

/**
 * Updates the permission root-ness of a DvObjectContainer.
 *
 * @author michael
 */
// no annotations here, since permissions are dynamically decided
@SuppressWarnings("serial")
public class UpdatePermissionRootCommand extends AbstractCommand<Dataverse> {

    private final boolean newValue;
    private final Dataverse dvoc;

    public UpdatePermissionRootCommand(boolean newValue, DataverseRequest aRequest, 
            Dataverse anAffectedDataverse) {
        super(aRequest, anAffectedDataverse);
        this.newValue = newValue;
        dvoc = anAffectedDataverse;
    }

    @Override
    public Dataverse execute(final CommandContext ctxt) {
        if (dvoc.isPermissionRoot() == newValue) {
            return dvoc;

        } else {
            dvoc.setPermissionRoot(newValue);
            return ctxt.dataverses().save(dvoc);
        }
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        // for data file check permission on owning dataset
        return singletonMap("", singleton(dvoc instanceof Dataverse 
                                    ? ManageDataversePermissions
                                    : ManageDatasetPermissions));
    }

}
