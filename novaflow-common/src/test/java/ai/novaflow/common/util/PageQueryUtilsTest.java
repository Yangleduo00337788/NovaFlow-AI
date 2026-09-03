package ai.novaflow.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageQueryUtilsTest {

    @Test
    void normalizePage_clampsToAtLeastOne() {
        assertEquals(1, PageQueryUtils.normalizePage(0));
        assertEquals(1, PageQueryUtils.normalizePage(-5));
        assertEquals(3, PageQueryUtils.normalizePage(3));
    }

    @Test
    void normalizePageSize_clampsBetweenOneAndMax() {
        assertEquals(1, PageQueryUtils.normalizePageSize(0));
        assertEquals(100, PageQueryUtils.normalizePageSize(99999));
        assertEquals(20, PageQueryUtils.normalizePageSize(20));
    }

    @Test
    void normalizePageSize_respectsCustomMax() {
        assertEquals(50, PageQueryUtils.normalizePageSize(999, 50));
    }
}
