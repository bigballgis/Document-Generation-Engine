package com.bank.docgen.authorization.management.api;

import java.util.List;

public record PageView<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PageView<T> of(List<T> all, int page, int size) {
        int safeSize = size <= 0 ? 20 : size;
        int safePage = Math.max(page, 0);
        int from = Math.min(safePage * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        List<T> slice = all.subList(from, to);
        int totalPages = (int) Math.ceil((double) all.size() / safeSize);
        return new PageView<>(List.copyOf(slice), safePage, safeSize, all.size(), totalPages);
    }
}
