package edu.harvard.iq.dataverse.engine.command.impl;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.dataset.MetadataBlock;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.user.Permission;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lists the metadata blocks of a {@link Dataverse}.
 *
 * @author michael
 */
// no annotations here, since permissions are dynamically decided
@SuppressWarnings("serial")
public class ListMetadataBlocksCommand extends AbstractCommand<List<MetadataBlock>> {

    private final Dataverse dv;

    public ListMetadataBlocksCommand(DataverseRequest aRequest, Dataverse aDataverse) {
        super(aRequest, aDataverse);
        dv = aDataverse;
    }

    @Override
    public List<MetadataBlock> execute(CommandContext ctxt) {
        return dv.getRootMetadataBlocks();
    }

    @Override
    public Map<String, Set<Permission>> getRequiredPermissions() {
        return singletonMap("", dv.isReleased() 
                                ? emptySet()
                                : singleton(Permission.ViewUnpublishedDataverse));
    }

}
