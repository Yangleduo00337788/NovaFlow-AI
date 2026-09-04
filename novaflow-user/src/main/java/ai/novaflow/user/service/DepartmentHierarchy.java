package ai.novaflow.user.service;

import java.util.Map;

final class DepartmentHierarchy {

    static final int MAX_DEPTH = 8;

    private DepartmentHierarchy() {
    }

    static boolean isSelfOrDescendant(Long nodeId, Long candidateParentId, Map<Long, Long> parentById) {
        if (nodeId == null || candidateParentId == null) {
            return false;
        }
        if (nodeId.equals(candidateParentId)) {
            return true;
        }
        Long current = candidateParentId;
        int guard = 0;
        while (current != null && guard++ < 64) {
            if (nodeId.equals(current)) {
                return true;
            }
            current = parentById.get(current);
        }
        return false;
    }

    static int depthOf(Long nodeId, Map<Long, Long> parentById) {
        int depth = 1;
        Long current = nodeId;
        int guard = 0;
        while (current != null && parentById.get(current) != null && guard++ < 64) {
            current = parentById.get(current);
            depth++;
        }
        return depth;
    }

    static int subtreeHeight(Long rootId, Map<Long, Long> parentById) {
        if (rootId == null) {
            return 0;
        }
        int rootDepth = depthOf(rootId, parentById);
        int maxDepth = rootDepth;
        for (Long nodeId : parentById.keySet()) {
            if (isSelfOrDescendant(rootId, nodeId, parentById)) {
                maxDepth = Math.max(maxDepth, depthOf(nodeId, parentById));
            }
        }
        return maxDepth - rootDepth + 1;
    }
}
