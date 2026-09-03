package ai.novaflow.user.service;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepartmentHierarchyTest {

    @Test
    void detectsCycleWhenMovingUnderOwnChild() {
        Map<Long, Long> parents = new HashMap<>();
        parents.put(2L, 1L);
        parents.put(3L, 2L);

        assertTrue(DepartmentHierarchy.isSelfOrDescendant(1L, 3L, parents));
        assertTrue(DepartmentHierarchy.isSelfOrDescendant(1L, 1L, parents));
        assertFalse(DepartmentHierarchy.isSelfOrDescendant(3L, 1L, parents));
        assertFalse(DepartmentHierarchy.isSelfOrDescendant(1L, null, parents));
        assertEquals(1, DepartmentHierarchy.depthOf(1L, parents));
        assertEquals(3, DepartmentHierarchy.depthOf(3L, parents));
    }
}
