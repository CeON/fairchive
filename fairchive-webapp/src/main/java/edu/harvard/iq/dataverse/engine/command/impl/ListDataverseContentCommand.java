package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.DvObject;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.Permission;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lists the content of a dataverse - both datasets and dataverses.
 *
 * @author michael
 */
// no annotations here, since permissions are dynamically decided
@SuppressWarnings("serial")
public class ListDataverseContentCommand extends AbstractCommand<List<DvObject>> {

    private final Dataverse dvToList;

    public ListDataverseContentCommand(DataverseRequest aRequest, 
            Dataverse anAffectedDataverse) {
        super(aRequest, anAffectedDataverse);
        dvToList = anAffectedDataverse;
    }

    @Override
    public List<DvObject> execute(CommandContext ctxt)  {
        if (getRequest().getUser().isSuperuser()) {
            return ctxt.dvObjects().findByOwnerId(dvToList.getId());
        } else {
            return ctxt.permissions().whichChildrenHasPermissionsForOrReleased(getRequest(), 
                    dvToList, EnumSet.of(Permission.ViewUnpublishedDataverse, 
                            Permission.ViewUnpublishedDataset));
        }
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        return singletonMap("", dvToList.isReleased() 
                                ? emptySet()
                                : singleton(Permission.ViewUnpublishedDataverse));
    }

}