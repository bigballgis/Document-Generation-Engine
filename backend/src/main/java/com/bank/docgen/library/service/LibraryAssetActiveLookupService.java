package com.bank.docgen.library.service;

import com.bank.docgen.library.domain.AssetLibraryAssetStatus;
import com.bank.docgen.library.persistence.LibraryAssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LibraryAssetActiveLookupService implements LibraryAssetActiveLookup {

    private final LibraryAssetRepository repository;

    public LibraryAssetActiveLookupService(LibraryAssetRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActive(String groupCode, String assetKey) {
        if (groupCode == null || groupCode.isBlank() || assetKey == null || assetKey.isBlank()) {
            return false;
        }
        return repository
                .findByGroupCodeAndAssetKeyAndDeletedAtIsNull(groupCode.trim(), assetKey.trim())
                .filter(entity -> entity.getStatus() == AssetLibraryAssetStatus.ACTIVE)
                .isPresent();
    }
}
