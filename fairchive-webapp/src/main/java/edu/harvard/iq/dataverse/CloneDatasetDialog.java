package edu.harvard.iq.dataverse;

import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;

import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseRepository;
import edu.harvard.iq.dataverse.search.dataverselookup.DataverseLookupService;
import edu.harvard.iq.dataverse.search.dataversestree.NodeData;
import edu.harvard.iq.dataverse.search.dataversestree.SolrTreeService;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.util.SystemConfig;

@SuppressWarnings("serial")
@ViewScoped
@Named("CloneDatasetDialog")
public class CloneDatasetDialog extends CreateDatasetDialog {

    private Dataset dataset;

    public CloneDatasetDialog() { 
    	super();
    }

    @Inject
    public CloneDatasetDialog(final SolrTreeService solrTreeService, 
    		final DataverseRequestServiceBean dataverseRequestService,
            final DataverseLookupService dataverseLookupService, 
            final DataverseRepository dataverseRepo,
            final DataverseSession session, 
            final SystemConfig systemConfig, 
            final SettingsServiceBean settingsService) {
    	
        super(solrTreeService, dataverseRequestService, dataverseLookupService,  
        		dataverseRepo, session, systemConfig, settingsService);
    }

    public String cloneDataset() {
    	return "/createDataset.xhtml?faces-redirect=true&ownerId="
        	+ ((NodeData) this.selectedNode.getData()).getId()
    		+ "&sourceDatasetId=" + this.dataset.getId();
    }

    public void setDataset(final Dataset dataset) {
    	this.dataset = dataset;
    }
}