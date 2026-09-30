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

    private final Dataverse dataverse;

    public ListFacetsCommand(final DataverseRequest request, final Dataverse dataverse) {
        super(request, dataverse);
        this.dataverse = dataverse;
    }

    @Override
    public List<DataverseFacet> execute(final CommandContext context)  {
        return this.dataverse.getLatestVersion().getFacets();
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        return singletonMap("", this.dataverse.isReleased() 
                                ? emptySet()
                                : singleton(Permission.ViewUnpublishedDataverse));
    }
}
