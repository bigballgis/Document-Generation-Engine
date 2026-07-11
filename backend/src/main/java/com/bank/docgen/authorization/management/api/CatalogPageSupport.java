package com.bank.docgen.authorization.management.api;

/**
 * Shared normalization for management catalog list endpoints (LR-C5).
 */
public final class CatalogPageSupport {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public static final String SORT_GROUP_CODE_ASC = "groupCodeAsc";
    public static final String SORT_GROUP_ASC = "groupAsc";
    public static final String SORT_UPDATED_AT_DESC = "updatedAtDesc";
    public static final String SORT_UPDATED_AT_ASC = "updatedAtAsc";
    public static final String SORT_NAME_ASC = "nameAsc";
    public static final String SORT_EXTERNAL_ID_ASC = "externalIdAsc";
    public static final String SORT_MODULE_CODE_ASC = "moduleCodeAsc";

    private CatalogPageSupport() {
    }

    public static int normalizePage(Integer page) {
        if (page == null) {
            return DEFAULT_PAGE;
        }
        return Math.max(page, DEFAULT_PAGE);
    }

    /**
     * Missing or {@code size <= 0} → default 20; {@code size > 100} → clamp to 100.
     */
    public static int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    public static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static int totalPages(long totalElements, int size) {
        if (size <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalElements / size);
    }
}
