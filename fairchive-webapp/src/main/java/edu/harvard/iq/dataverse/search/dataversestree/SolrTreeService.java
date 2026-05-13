package edu.harvard.iq.dataverse.search.dataversestree;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;

import edu.harvard.iq.dataverse.engine.command.DataverseRequest;
import edu.harvard.iq.dataverse.persistence.dataverse.DataverseRepository;
import edu.harvard.iq.dataverse.search.SearchFields;
import edu.harvard.iq.dataverse.search.query.PermissionFilterQueryBuilder;
import edu.harvard.iq.dataverse.search.query.SearchObjectType;

@Stateless
public class SolrTreeService {
    private static final Logger logger = getLogger(SolrTreeService.class);

    private SolrClient solrClient;
    private PermissionFilterQueryBuilder permissionFilterQueryBuilder;
    private DataverseRepository dataverseRepo;

    // -------------------- CONSTRUCTORS --------------------

    public SolrTreeService() { }

    @Inject
    public SolrTreeService(final SolrClient solrClient, 
    		final PermissionFilterQueryBuilder permissionFilterQueryBuilder,
            final DataverseRepository dataverseRepo) {
        this.solrClient = solrClient;
        this.permissionFilterQueryBuilder = permissionFilterQueryBuilder;
        this.dataverseRepo = dataverseRepo;
    }

    // -------------------- LOGIC --------------------

    public NodesInfo fetchNodesInfo(final DataverseRequest dataverseRequest) {
        try {
            return createNodesInfo(executeSolrQueryForNodeInfo(dataverseRequest));
        } catch (final IOException | SolrServerException e) {
            logger.warn("Error during permissions fetching: ", e);
            return new NodesInfo(emptyMap(), emptySet());
        }
    }

    public List<NodeData> fetchNodes(final Long nodeId, final NodesInfo nodesInfo) {
        if (nodeId == null || nodesInfo == null || nodesInfo.getPermissions().isEmpty()) {
            return emptyList();
        }
        try {
            return createNodeData(nodesInfo, executeSolrQueryForNodes(nodeId));
        } catch (final SolrServerException | IOException e) {
            logger.warn("Error during node fetching: ", e);
            return emptyList();
        }
    }

    // -------------------- PRIVATE --------------------

    private QueryResponse executeSolrQueryForNodeInfo(final DataverseRequest dataverseRequest) 
    		throws IOException, SolrServerException {
        final Integer dataversesCount = this.dataverseRepo.countAll().intValue();
        final String permissionQuery = this.permissionFilterQueryBuilder.
        		buildPermissionFilterQueryForAddDataset(dataverseRequest);
        final SolrQuery query = new SolrQuery()
                .setRows(dataversesCount)
                .setQuery(format("%s:%s", SearchFields.TYPE, SearchObjectType.DATAVERSES.getSolrValue()))
                .setFields(SearchFields.ENTITY_ID, SearchFields.PARENT_ID, SearchFields.SUBTREE)
                .setFilterQueries(permissionQuery);
        return solrClient.query(query);
    }

    private NodesInfo createNodesInfo(final QueryResponse queryResponse) {
        final Set<Long> allowedToSelect = new HashSet<>();
        final Set<Long> allowedToView = new HashSet<>();
        for (final SolrDocument solrDocument : queryResponse.getResults()) {
            allowedToSelect.add((Long) solrDocument.getFieldValue(SearchFields.ENTITY_ID));
            final String parentId = (String) solrDocument.getFieldValue(SearchFields.PARENT_ID);
            @SuppressWarnings("unchecked")
            List<String> paths = (List<String>) solrDocument.getFieldValue(SearchFields.SUBTREE);
            paths = paths != null ? paths : emptyList();
            final Set<Long> intermediatePaths = paths.stream()
                    .filter(p -> p.endsWith("/" + parentId))
                    .flatMap(p -> stream(p.split("/"))
                            .filter(StringUtils::isNotBlank)
                            .map(Long::valueOf))
                    .collect(toSet());
            allowedToView.addAll(intermediatePaths);
        }
        final Set<Long> expandableNodes = new HashSet<>(allowedToView); // if node is expandable it's listed in subtreePath field
        allowedToView.removeAll(allowedToSelect); // SELECT node is more than only VIEW, so we remove selectable nodes
        final Map<Long, NodePermission> result = new HashMap<>();
        for (final Long id : allowedToSelect) {
            result.put(id, NodePermission.SELECT);
        }
        for (final Long id : allowedToView) {
            result.put(id, NodePermission.VIEW);
        }
        return new NodesInfo(result, expandableNodes);
    }

    private QueryResponse executeSolrQueryForNodes(final Long nodeId) 
    		throws IOException, SolrServerException {
        final Integer rows = this.dataverseRepo.countDataversesWithParent(nodeId).intValue();
        final SolrQuery query = new SolrQuery()
                .setRows(rows)
                .setQuery(format("%s:%s AND %s:%d",
                        SearchFields.TYPE, SearchObjectType.DATAVERSES.getSolrValue(),
                        SearchFields.PARENT_ID, nodeId))
                .setSort(SearchFields.NAME_SORT, SolrQuery.ORDER.asc)
                .setFields(SearchFields.ENTITY_ID, SearchFields.NAME);
        return solrClient.query(query);
    }

    private List<NodeData> createNodeData(final NodesInfo nodesInfo, 
    		final QueryResponse queryResponse) {
        final List<NodeData> result = new ArrayList<>();
        for (final SolrDocument solrDocument : queryResponse.getResults()) {
            final Long id = (Long) solrDocument.getFieldValue(SearchFields.ENTITY_ID);
            final String name = (String) solrDocument.getFieldValue(SearchFields.NAME);
            if (nodesInfo.isViewable(id)) {
                result.add(new NodeData(id, name, nodesInfo.isExpandable(id),
                		nodesInfo.isSelectable(id)));
            }
        }
        return result;
    }
}