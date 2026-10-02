package edu.harvard.iq.dataverse.engine.command.impl;

import static java.util.Collections.emptyList;

import java.util.List;

import edu.harvard.iq.dataverse.engine.command.AbstractCommand;
import edu.harvard.iq.dataverse.engine.command.CommandContext;
import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.engine.command.RequiredPermissions;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataset.DatasetFieldType;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFacet;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse.DataverseType;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseFieldTypeInputLevel;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseVersion;
import edu.harvard.iq.dataverse.persistence.user.Permission;

/**
 * Update an existing dataverse.
 *
 * @author michael
 */
@SuppressWarnings("serial")
@RequiredPermissions(Permission.EditDataverse)
public class UpdateDataverseCommand extends AbstractCommand<Dataverse> {

    private final Dataverse dataverse;
    private final List<DatasetFieldType> facets;
    private final List<Dataverse> featuredDataverses;
    private final List<DataverseFieldTypeInputLevel> inputLevels;

    public UpdateDataverseCommand(Dataverse dataverse, List<DatasetFieldType> facets, 
            List<Dataverse> featuredDataverses, DataverseRequest request, 
            List<DataverseFieldTypeInputLevel> inputLevels) {
        super(request, dataverse);
        this.dataverse = dataverse;
        // add update template uses this command but does not
        // update facet list or featured dataverses
        this.facets = facets != null ? facets: emptyList();
        this.featuredDataverses = featuredDataverses != null ? featuredDataverses : emptyList();
        this.inputLevels = inputLevels != null ?inputLevels : emptyList();
    }

    @Override
    public Dataverse execute(final CommandContext context)  {
    	
        DataverseType oldDvType = context.dataverses().find(this.dataverse.getId()).getDataverseType();
        String oldDvAlias = context.dataverses().find(this.dataverse.getId()).getAlias();
        String oldDvName = context.dataverses().find(this.dataverse.getId()).getName();
        Dataverse result = context.dataverses().save(this.dataverse);
        
        addFacets(context);
        addDataverseFieldTypeInputLevels(context);
        
        if (featuredDataverses != null) {
            context.featuredDataverses().deleteFeaturedDataversesFor(result);
            int i = 0;
            for (Dataverse dv : featuredDataverses) {
                context.featuredDataverses().create(i++, dv.getId(), result.getId());
            }
        }

        context.index().indexDataverse(result);

        List<Dataverse> dvChildrenToUpdate = context.dataverses().findByOwnerId(result.getId());
        dvChildrenToUpdate.forEach(dvForUpdate -> context.index().indexDataverse(dvForUpdate));

        //When these values are changed we need to reindex all children datasets
        //This check is not recursive as all the values just report the immediate parent
        //
        //This runs async to not slow down editing --MAD 4.9.4
        if (!oldDvType.equals(this.dataverse.getDataverseType())
                || !oldDvName.equals(this.dataverse.getName())
                || !oldDvAlias.equals(this.dataverse.getAlias())) {
            List<Dataset> datasets = context.datasetService().findByOwnerId(this.dataverse.getId());
            context.index().asyncIndexDatasetList(datasets, true);
        }
        return result;
    }
    
	private void addFacets(final CommandContext context) {
        final DataverseVersion version = this.dataverse.getLatestVersion();
        for(int index = 0; index < this.facets.size(); ++index) {
        	final DataverseFacet facet = version.addFacet(this.facets.get(index));
        	facet.setDisplayOrder(index);
        	context.facets().save(facet);
        }
	}
	
	private void addDataverseFieldTypeInputLevels(final CommandContext context) {
		final DataverseVersion version = this.dataverse.getLatestVersion();
		for(final DataverseFieldTypeInputLevel level : this.inputLevels) {
			version.addDataverseFieldTypeInputLevel(level);
			context.inputLevels().save(level);
		}
	}
}
