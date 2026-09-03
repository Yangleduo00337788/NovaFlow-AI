package ai.novaflow.common.util;

public final class PageQueryUtils {

    public static final int DEFAULT_MAX_PAGE_SIZE = 100;

    private PageQueryUtils() {
    }

    public static int normalizePage(int page) {
        return Math.max(page, 1);
    }

    public static int normalizePageSize(int pageSize) {
        return normalizePageSize(pageSize, DEFAULT_MAX_PAGE_SIZE);
    }

    public static int normalizePageSize(int pageSize, int maxPageSize) {
        int cap = Math.max(maxPageSize, 1);
        return Math.min(Math.max(pageSize, 1), cap);
    }
}
