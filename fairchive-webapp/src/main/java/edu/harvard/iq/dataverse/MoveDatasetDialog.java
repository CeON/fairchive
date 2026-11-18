package edu.harvard.iq.dataverse;

import static edu.harvard.iq.dataverse.common.BundleUtil.getStringFromBundle;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;
import org.primefaces.event.NodeExpandEvent;
import org.primefaces.model.TreeNode;

import edu.harvard.iq.dataverse.dataset.DatasetService;
import edu.harvard.iq.dataverse.persistence.dataset.Dataset;
import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseRepository;
import edu.harvard.iq.dataverse.search.dataverselookup.DataverseLookupService;
import edu.harvard.iq.dataverse.search.dataverselookup.LookupData;
import edu.harvard.iq.dataverse.search.dataversestree.NodeData;
import edu.harvard.iq.dataverse.search.dataversestree.NodesInfo;
import edu.harvard.iq.dataverse.search.dataversestree.SolrTreeService;
import edu.harvard.iq.dataverse.search.dataversestree.TreeNodeBrowser;
import edu.harvard.iq.dataverse.util.UIMessages;

@SuppressWarnings("serial")
@ViewScoped
@Named("MoveDatasetDialog")
public class MoveDatasetDialog implements Serializable {
    private SolrTreeService solrTreeService;
    private DataverseRequestServiceBean dataverseRequestService;
    private DataverseLookupService dataverseLookupService;
    private DataverseRepository dataverseRepo;
    private DatasetService datasetService;

    private NodesInfo nodesInfo = new NodesInfo(emptyMap(), emptySet());
    protected TreeNode selectedNode;

    private String treeFilter;
    private String prevTreeFilter;
    private TreeNodeBrowser treeNodeBrowser;
    private DataverseSession session;
    private UIMessages ui;
    
    private Dataset dataset;

    // -------------------- CONSTRUCTORS --------------------

    public MoveDatasetDialog() { }

    @Inject
    public MoveDatasetDialog(final SolrTreeService solrTreeService, 
    		final DataverseRequestServiceBean dataverseRequestService,
            final DataverseLookupService dataverseLookupService, 
            final DataverseRepository dataverseRepo,
            final DataverseSession session,
            final DatasetService datasetService,
            UIMessages ui) {
    	
        this.solrTreeService = solrTreeService;
        this.dataverseRequestService = dataverseRequestService;
        this.dataverseLookupService = dataverseLookupService;
        this.dataverseRepo = dataverseRepo;
        this.session = session;
        this.datasetService = datasetService;
        this.ui = ui;
    }

    public TreeNode getRootNode() {
        return this.treeNodeBrowser != null ? this.treeNodeBrowser.getRootNode() : null;
    }

    public TreeNode getSelectedNode() {
        return this.selectedNode;
    }
    
    public void setSelectedNode(final TreeNode node) {
        this.selectedNode = node;
    }

    public String getTreeFilter() {
        return this.treeFilter;
    }

    public void setTreeFilter(final String filter) {
        this.treeFilter = filter;
    }

    @PostConstruct
    public void init() {
        final Dataverse rootDataverse = this.dataverseRepo.findRoot();
        this.nodesInfo = this.solrTreeService.fetchNodesInfo(
        		this.dataverseRequestService.getDataverseRequest());
        this.treeNodeBrowser = new TreeNodeBrowser(rootDataverse, 
        		this.nodesInfo, this::loadParentDataverseId, this::fetchChildren);
    }
    
    public void setDataset(final Dataset dataset) {
    	this.dataset = dataset;
    }

    public void onNodeExpand(final NodeExpandEvent event) {
        this.treeNodeBrowser.fetchChildNodes(event.getTreeNode());
    }
    
    public void executeTreeFilter() {
        if (this.prevTreeFilter != null && this.prevTreeFilter.equals(this.treeFilter)) {
            return;
        }

        this.treeNodeBrowser.resetRoot();

        if (this.treeFilter == null || this.treeFilter.length() < 3) {
            return;
        }

        final List<LookupData> results = this.dataverseLookupService.
        		fetchLookupDataByNameAndExtraDescription(this.treeFilter, EMPTY);
        if (results.isEmpty()) {
            return;
        }

        final Map<Long, Long> parentIdsCache = this.treeNodeBrowser.expandTreeTo(results.stream()
                .collect(toMap(LookupData::getId, LookupData::getParentId)));
        this.treeNodeBrowser.trimTree(parentIdsCache.keySet());

        this.prevTreeFilter = this.treeFilter;
    }

    public void moveDataset() {
    	if(this.session.isSuperUserLoggedIn()) {
            try {
            	final Long id = ((NodeData) this.selectedNode.getData()).getId();
            	final Dataverse target = this.dataverseRepo.getById(id);
            	this.datasetService.moveDataset(this.dataset, target);
            } catch (Exception ce) {
                this.ui.addErrorMessage(getStringFromBundle(
                        "dashboard.datamove.dataset.message.failure.summary"));
            }
    	} 
    }

    private Optional<Long> loadParentDataverseId(final Long id) {
        return this.dataverseRepo.findById(id)
                .map(Dataverse::getOwner)
                .map(Dataverse::getId);
    }

    private List<NodeData> fetchChildren(final Long id) {
        return this.solrTreeService.fetchNodes(id, this.nodesInfo);
    }
}