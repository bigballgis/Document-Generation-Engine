package com.bank.docgen.library.service;

/**
 * Catalog gate for template resolve — ACTIVE membership under a template owning group (ALGI-C5).
 */
public interface LibraryAssetActiveLookup {

    boolean isActive(String groupCode, String assetKey);
}
