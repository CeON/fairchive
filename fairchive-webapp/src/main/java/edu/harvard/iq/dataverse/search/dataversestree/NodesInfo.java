package edu.harvard.iq.dataverse.search.dataversestree;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.Set;

public class NodesInfo {
    private final Map<Long, NodePermission> permissions;
    private final Set<Long> expandableNodes;

    // -------------------- CONSTRUCTORS --------------------

    public NodesInfo(final Map<Long, NodePermission> permissions, 
    		final Set<Long> expandableNodes) {
        this.permissions = requireNonNull(permissions);
        this.expandableNodes = requireNonNull(expandableNodes);
    }

    // -------------------- GETTERS --------------------

    public Map<Long, NodePermission> getPermissions() {
        return this.permissions;
    }

    public Set<Long> getExpandableNodes() {
        return this.expandableNodes;
    }

    // -------------------- LOGIC --------------------

    public boolean isViewable(final Long id) {
        return this.permissions.containsKey(id);
    }

    public boolean isSelectable(final Long id) {
        return this.permissions.get(id) == NodePermission.SELECT;
    }

    public boolean isExpandable(final Long id) {
        return this.expandableNodes.contains(id);
    }
}
