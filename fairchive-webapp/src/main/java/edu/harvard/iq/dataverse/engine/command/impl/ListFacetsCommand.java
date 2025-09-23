package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFacet;
import edu.harvard.iq.dataverse.persistence.user.Permission;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * List the search facets {@link DataverseFacet} of a {@link Dataverse}.
 *
 * @author michaelsuo
 */
// no annotations here, since permissions are dynamically decided
@SuppressWarnings("serial")
public class ListFacetsCommand extends AbstractCommand<List<DataverseFacet>> {

    private final Dataverse dv;

    public ListFacetsCommand(DataverseRequest aRequest, Dataverse aDataverse) {
        super(aRequest, aDataverse);
        dv = aDataverse;
    }

    @Override
    public List<DataverseFacet> execute(CommandContext ctxt)  {
        return dv.getDataverseFacets();
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        return singletonMap("", dv.isReleased() 
                                ? emptySet()
                                : singleton(Permission.ViewUnpublishedDataverse));
    }
}
