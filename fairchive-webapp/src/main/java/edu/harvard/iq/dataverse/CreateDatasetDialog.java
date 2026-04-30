package edu.harvard.iq.dataverse;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toMap;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;
import org.primefaces.event.NodeExpandEvent;
import org.primefaces.model.TreeNode;

import edu.harvard.iq.dataverse.persistence.dataverse.Dataverse;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseRepository;
import edu.harvard.iq.dataverse.search.dataverselookup.DataverseLookupService;
import edu.harvard.iq.dataverse.search.dataverselookup.LookupData;
import edu.harvard.iq.dataverse.search.dataversestree.NodeData;
import edu.harvard.iq.dataverse.search.dataversestree.NodesInfo;
import edu.harvard.iq.dataverse.search.dataversestree.SolrTreeService;
import edu.harvard.iq.dataverse.search.dataversestree.TreeNodeBrowser;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean;
import edu.harvard.iq.dataverse.settings.SettingsServiceBean.Key;
import edu.harvard.iq.dataverse.util.SystemConfig;

@SuppressWarnings("serial")
@ViewScoped
@Named("CreateDatasetDialog")
public class CreateDatasetDialog implements Serializable {
    private SolrTreeService solrTreeService;
    private DataverseRequestServiceBean dataverseRequestService;
    private DataverseLookupService dataverseLookupService;
    private DataverseRepository dataverseRepo;

    private boolean initialized = false;

    private NodesInfo nodesInfo = new NodesInfo(emptyMap(), emptySet());
    private TreeNode selectedNode;

    private String permissionFilterQuery;
    private String treeFilter;
    private String prevTreeFilter;
    private TreeNodeBrowser treeNodeBrowser;
    private DataverseSession session;
    private SystemConfig systemConfig;
    private SettingsServiceBean settingsService;

    // -------------------- CONSTRUCTORS --------------------

    public CreateDatasetDialog() { }

    @Inject
    public CreateDatasetDialog(final SolrTreeService solrTreeService, 
    		final DataverseRequestServiceBean dataverseRequestService,
            final DataverseLookupService dataverseLookupService, 
            final DataverseRepository dataverseRepo,
            final DataverseSession session, 
            final SystemConfig systemConfig, 
            final SettingsServiceBean settingsService) {
    	
        this.solrTreeService = solrTreeService;
        this.dataverseRequestService = dataverseRequestService;
        this.dataverseLookupService = dataverseLookupService;
        this.dataverseRepo = dataverseRepo;
        this.session = session;
        this.systemConfig = systemConfig;
        this.settingsService = settingsService;
    }

    // -------------------- GETTERS --------------------

    public TreeNode getRootNode() {
        return this.treeNodeBrowser != null ? this.treeNodeBrowser.getRootNode() : null;
    }

    public TreeNode getSelectedNode() {
        return this.selectedNode;
    }

    public String getTreeFilter() {
        return this.treeFilter;
    }

    // -------------------- LOGIC --------------------

    public void init() {
        if (this.initialized) {
            return;
        }
        this.permissionFilterQuery = this.dataverseLookupService.
        		buildFilterQuery(this.dataverseRequestService.getDataverseRequest());

        final Dataverse rootDataverse = this.dataverseRepo.findRoot();
        this.nodesInfo = this.solrTreeService.fetchNodesInfo(
        		this.dataverseRequestService.getDataverseRequest());
        this.treeNodeBrowser = new TreeNodeBrowser(rootDataverse, 
        		this.nodesInfo, this::loadParentDataverseId, this::fetchChildren);
        this.initialized = true;
    }

    public void onNodeExpand(final NodeExpandEvent event) {
        final TreeNode selectedNode = event.getTreeNode();
        this.treeNodeBrowser.fetchChildNodes(selectedNode);
    }

    public void executeTreeFilter() {
        if (this.prevTreeFilter != null && this.prevTreeFilter.equals(this.treeFilter)) {
            return;
        }

        this.treeNodeBrowser.resetRoot();

        if (this.treeFilter == null || this.treeFilter.length() < 3) {
            return;
        }

        final List<LookupData> results = this.dataverseLookupService.fetchLookupDataByNameAndExtraDescription(
        		this.treeFilter, this.permissionFilterQuery);
        if (results.isEmpty()) {
            return;
        }

        final Map<Long, Long> parentIdsCache = this.treeNodeBrowser.expandTreeTo(results.stream()
                .collect(toMap(LookupData::getId, LookupData::getParentId)));
        this.treeNodeBrowser.trimTree(parentIdsCache.keySet());

        this.prevTreeFilter = this.treeFilter;
    }

    public boolean showContactToCreateDataverseTip() {
        return settingsService.isTrueForKey(Key.ShowContactToCreateDataverseTip);
    }

    public String createDataset() {
        return "/createDataset.xhtml?faces-redirect=true&ownerId=" +
        		((NodeData) this.selectedNode.getData()).getId();
    }

    public String getSelectDataverseInfo() {
        return this.systemConfig.getSelectDataverseInfo(this.session.getLocale());
    }

    // -------------------- PRIVATE --------------------

    private Optional<Long> loadParentDataverseId(final Long id) {
        return this.dataverseRepo.findById(id)
                .map(Dataverse::getOwner)
                .map(Dataverse::getId);
    }

    private List<NodeData> fetchChildren(final Long id) {
        return this.solrTreeService.fetchNodes(id, this.nodesInfo);
    }

    // -------------------- SETTERS --------------------

    public void setSelectedNode(final TreeNode node) {
        this.selectedNode = node;
    }

    public void setTreeFilter(final String filter) {
        this.treeFilter = filter;
    }
}