package com.bank.docgen.audit.api;

import java.util.List;

public record AuditPagedResult<T>(
        List<T> events,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static final int DEFAULT_PAGE_SIZE = 20;

    public static int normalizePage(Integer page) {
        return page == null || page < 0 ? 0 : page;
    }

    public static int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, 100);
    }
}
