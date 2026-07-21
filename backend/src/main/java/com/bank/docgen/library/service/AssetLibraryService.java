package com.bank.docgen.library.service;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.api.CatalogPageSupport;
import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.authorization.management.persistence.BusinessGroupRepository;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.infrastructure.storage.ObjectStorageException;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.library.api.AssetLibraryAssetView;
import com.bank.docgen.library.domain.AssetLibraryAssetClass;
import com.bank.docgen.library.domain.AssetLibraryAssetStatus;
import com.bank.docgen.library.persistence.LibraryAssetEntity;
import com.bank.docgen.library.persistence.LibraryAssetRepository;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AssetLibraryService {

    private static final Logger LOG = LoggerFactory.getLogger(AssetLibraryService.class);

    private final LibraryAssetRepository repository;
    private final ObjectStoragePort objectStoragePort;
    private final GroupAccessService groupAccessService;
    private final BusinessGroupRepository businessGroupRepository;
    private final ManagementAuditRecorder auditRecorder;
    private final Clock clock;

    public AssetLibraryService(
            LibraryAssetRepository repository,
            ObjectStoragePort objectStoragePort,
            GroupAccessService groupAccessService,
            BusinessGroupRepository businessGroupRepository,
            ManagementAuditRecorder auditRecorder,
            Clock clock
    ) {
        this.repository = repository;
        this.objectStoragePort = objectStoragePort;
        this.groupAccessService = groupAccessService;
        this.businessGroupRepository = businessGroupRepository;
        this.auditRecorder = auditRecorder;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageView<AssetLibraryAssetView> list(
            ManagementSessionClaims session,
            Integer page,
            Integer size,
            AssetLibraryAssetClass assetClass,
            AssetLibraryListStatusFilter statusFilter,
            String q,
            String groupCodeRaw
    ) {
        requireListAccess(session);
        int normalizedPage = CatalogPageSupport.normalizePage(page);
        int normalizedSize = CatalogPageSupport.normalizeSize(size);
        AssetLibraryAssetStatus status = resolveListStatus(session, statusFilter);
        String query = CatalogPageSupport.blankToNull(q);
        String groupFilter = CatalogPageSupport.blankToNull(groupCodeRaw == null ? null : groupCodeRaw.trim());
        Pageable pageable = PageRequest.of(
                normalizedPage,
                normalizedSize,
                Sort.by(Sort.Direction.DESC, "uploadedAt")
        );

        boolean global = session.roles().contains("GLOBAL_ADMIN");
        boolean restrictGroups = !global;
        Collection<String> accessibleGroups = global ? List.of("__unused__") : session.authorizedGroupCodes();
        if (restrictGroups) {
            if (accessibleGroups.isEmpty()) {
                return emptyPage(normalizedPage, normalizedSize);
            }
            if (groupFilter != null && !accessibleGroups.contains(groupFilter)) {
                return emptyPage(normalizedPage, normalizedSize);
            }
        }

        Page<LibraryAssetEntity> result = query == null
                ? repository.search(status, assetClass, groupFilter, restrictGroups, accessibleGroups, pageable)
                : repository.searchByQuery(
                        status, assetClass, groupFilter, restrictGroups, accessibleGroups, query, pageable
                );
        return new PageView<>(
                result.getContent().stream().map(this::toView).toList(),
                normalizedPage,
                normalizedSize,
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional
    public AssetLibraryAssetView upload(
            ManagementSessionClaims session,
            MultipartFile file,
            String assetKeyRaw,
            AssetLibraryAssetClass assetClass,
            String groupCodeRaw
    ) {
        requireUploadAccess(session, assetClass);
        String groupCode = requireExistingAuthorizedGroup(session, groupCodeRaw);
        String assetKey = AssetLibraryUploadValidator.normalizeAssetKey(assetKeyRaw);
        AssetLibraryUploadValidator.ValidatedPayload payload = AssetLibraryUploadValidator.validateFile(file);
        warnIfPrefixMismatch(assetKey, assetClass);

        LibraryAssetEntity existing = repository
                .findByGroupCodeAndAssetKeyAndDeletedAtIsNull(groupCode, assetKey)
                .orElse(null);
        boolean reupload = false;
        if (existing != null) {
            if (existing.getStatus() == AssetLibraryAssetStatus.ACTIVE) {
                throw new AssetLibraryConflictException(
                        ApiErrorCodes.ASSET_LIBRARY_ASSET_KEY_CONFLICT,
                        "api.error.assetLibrary.assetKeyConflict"
                );
            }
            reupload = true;
        }

        Instant now = clock.instant();
        String sha256 = sha256Hex(payload.bytes());
        String objectKey = AssetLibraryStorageKeys.namespacedKey(groupCode, assetKey);
        storeObject(objectKey, payload.bytes(), payload.contentType());

        LibraryAssetEntity saved;
        if (reupload) {
            existing.reactivate(
                    assetClass,
                    payload.contentType(),
                    payload.bytes().length,
                    sha256,
                    payload.originalFileName(),
                    session.username(),
                    now
            );
            saved = repository.save(existing);
            auditRecorder.recordAssetLibraryReupload(
                    groupCode,
                    assetKey,
                    assetClass.name(),
                    session.username(),
                    session.displayName(),
                    sha256
            );
        } else {
            saved = repository.save(new LibraryAssetEntity(
                    groupCode,
                    assetKey,
                    assetClass,
                    AssetLibraryAssetStatus.ACTIVE,
                    payload.contentType(),
                    payload.bytes().length,
                    sha256,
                    payload.originalFileName(),
                    session.username(),
                    now
            ));
            auditRecorder.recordAssetLibraryUpload(
                    groupCode,
                    assetKey,
                    assetClass.name(),
                    session.username(),
                    session.displayName(),
                    sha256
            );
        }
        return toView(saved);
    }

    /**
     * SYS-NORM Wave 7 / PP-C8 — materialize an asset binary from a promotion pack into the
     * <strong>target template group</strong> (ALGI hard isolation; no platform-shared reintroduction).
     */
    @Transactional
    public AssetLibraryAssetView materializeImportedAsset(
            ManagementSessionClaims session,
            String groupCodeRaw,
            String assetKeyRaw,
            AssetLibraryAssetClass assetClass,
            byte[] bytes,
            String contentType,
            String originalFileName
    ) {
        requireUploadAccess(session, assetClass == null ? AssetLibraryAssetClass.OTHER : assetClass);
        String groupCode = requireExistingAuthorizedGroup(session, groupCodeRaw);
        String assetKey = AssetLibraryUploadValidator.normalizeAssetKey(assetKeyRaw);
        if (bytes == null || bytes.length == 0) {
            throw new AssetLibraryValidationException(
                    ApiErrorCodes.ASSET_LIBRARY_CONTENT_TYPE_MISMATCH,
                    "api.error.assetLibrary.payloadEmpty"
            );
        }
        if (bytes.length > AssetLibraryUploadValidator.MAX_BYTES) {
            throw new AssetLibraryValidationException(
                    ApiErrorCodes.ASSET_LIBRARY_PAYLOAD_TOO_LARGE,
                    "api.error.assetLibrary.payloadTooLarge"
            );
        }
        AssetLibraryAssetClass resolvedClass = assetClass == null ? AssetLibraryAssetClass.IMAGE : assetClass;
        String resolvedType = contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType;
        String fileName = originalFileName == null || originalFileName.isBlank() ? assetKey : originalFileName;

        LibraryAssetEntity existing = repository
                .findByGroupCodeAndAssetKeyAndDeletedAtIsNull(groupCode, assetKey)
                .orElse(null);
        if (existing != null && existing.getStatus() == AssetLibraryAssetStatus.ACTIVE) {
            return toView(existing);
        }

        Instant now = clock.instant();
        String sha256 = sha256Hex(bytes);
        storeObject(AssetLibraryStorageKeys.namespacedKey(groupCode, assetKey), bytes, resolvedType);
        LibraryAssetEntity saved;
        if (existing != null) {
            existing.reactivate(
                    resolvedClass,
                    resolvedType,
                    bytes.length,
                    sha256,
                    fileName,
                    session.username(),
                    now
            );
            saved = repository.save(existing);
        } else {
            saved = repository.save(new LibraryAssetEntity(
                    groupCode,
                    assetKey,
                    resolvedClass,
                    AssetLibraryAssetStatus.ACTIVE,
                    resolvedType,
                    bytes.length,
                    sha256,
                    fileName,
                    session.username(),
                    now
            ));
        }
        return toView(saved);
    }

    @Transactional
    public AssetLibraryAssetView disable(
            ManagementSessionClaims session,
            String assetKeyRaw,
            String groupCodeRaw
    ) {
        requireDisableAccess(session);
        String groupCode = requireExistingAuthorizedGroup(session, groupCodeRaw);
        String assetKey = AssetLibraryUploadValidator.normalizeAssetKey(assetKeyRaw);
        LibraryAssetEntity entity = repository
                .findByGroupCodeAndAssetKeyAndDeletedAtIsNull(groupCode, assetKey)
                .orElseThrow(() -> new AssetLibraryNotFoundException("api.error.assetLibrary.assetNotFound"));

        deleteResolvableObjects(groupCode, assetKey);

        if (entity.getStatus() == AssetLibraryAssetStatus.DISABLED) {
            return toView(entity);
        }

        Instant now = clock.instant();
        entity.markDisabled(now);
        LibraryAssetEntity saved = repository.save(entity);
        auditRecorder.recordAssetLibraryDisable(
                groupCode,
                assetKey,
                saved.getAssetClass().name(),
                session.username(),
                session.displayName(),
                saved.getContentSha256()
        );
        return toView(saved);
    }

    private String requireExistingAuthorizedGroup(ManagementSessionClaims session, String groupCodeRaw) {
        if (groupCodeRaw == null || groupCodeRaw.isBlank()) {
            throw new AssetLibraryValidationException(
                    ApiErrorCodes.ASSET_LIBRARY_GROUP_CODE_REQUIRED,
                    "api.error.assetLibrary.groupCodeRequired"
            );
        }
        String groupCode = groupCodeRaw.trim();
        if (!businessGroupRepository.existsByGroupCodeAndDeletedAtIsNull(groupCode)) {
            throw new AssetLibraryAccessDeniedException();
        }
        if (!groupAccessService.canAccessGroup(session, groupCode)) {
            throw new AssetLibraryAccessDeniedException();
        }
        return groupCode;
    }

    private static PageView<AssetLibraryAssetView> emptyPage(int page, int size) {
        Page<LibraryAssetEntity> empty = new PageImpl<>(List.of(), PageRequest.of(page, size), 0);
        return new PageView<>(List.of(), page, size, empty.getTotalElements(), empty.getTotalPages());
    }

    private void storeObject(String objectKey, byte[] bytes, String contentType) {
        objectStoragePort.put(objectKey, new ByteArrayInputStream(bytes), bytes.length, contentType);
    }

    private void deleteResolvableObjects(String groupCode, String assetKey) {
        for (String key : AssetLibraryStorageKeys.namespacedResolvableKeys(groupCode, assetKey)) {
            ensureObjectRemoved(key);
        }
    }

    private void ensureObjectRemoved(String objectKey) {
        objectStoragePort.delete(objectKey);
        if (objectStoragePort.exists(objectKey)) {
            throw new ObjectStorageException(
                    "Object still present after delete",
                    new IllegalStateException("exists returned true after delete")
            );
        }
    }

    private void requireListAccess(ManagementSessionClaims session) {
        if (!groupAccessService.canManageAssetLibrary(session)) {
            throw new AssetLibraryAccessDeniedException();
        }
    }

    private void requireUploadAccess(ManagementSessionClaims session, AssetLibraryAssetClass assetClass) {
        if (assetClass == AssetLibraryAssetClass.SEAL) {
            if (!groupAccessService.canUploadSealAsset(session)) {
                throw new AssetLibraryAccessDeniedException();
            }
            return;
        }
        if (!groupAccessService.canUploadImageOrOtherAsset(session)) {
            throw new AssetLibraryAccessDeniedException();
        }
    }

    private void requireDisableAccess(ManagementSessionClaims session) {
        if (!groupAccessService.canDisableAssetLibrary(session)) {
            throw new AssetLibraryAccessDeniedException();
        }
    }

    private AssetLibraryAssetStatus resolveListStatus(
            ManagementSessionClaims session,
            AssetLibraryListStatusFilter statusFilter
    ) {
        if (groupAccessService.isAssetLibraryTesterOnly(session)) {
            return AssetLibraryAssetStatus.ACTIVE;
        }
        if (statusFilter == null || statusFilter == AssetLibraryListStatusFilter.ACTIVE) {
            return AssetLibraryAssetStatus.ACTIVE;
        }
        if (statusFilter == AssetLibraryListStatusFilter.DISABLED) {
            return AssetLibraryAssetStatus.DISABLED;
        }
        return null;
    }

    private void warnIfPrefixMismatch(String assetKey, AssetLibraryAssetClass assetClass) {
        String upper = assetKey.toUpperCase(Locale.ROOT);
        boolean mismatch = switch (assetClass) {
            case IMAGE -> !upper.startsWith("IMG-");
            case SEAL -> !upper.startsWith("SEAL-");
            case OTHER -> false;
        };
        if (mismatch) {
            LOG.info(
                    "Asset library key prefix convention mismatch assetKey={} assetClass={}",
                    assetKey,
                    assetClass
            );
        }
    }

    private AssetLibraryAssetView toView(LibraryAssetEntity entity) {
        return new AssetLibraryAssetView(
                entity.getGroupCode(),
                entity.getAssetKey(),
                entity.getAssetClass(),
                entity.getStatus(),
                entity.getContentType(),
                entity.getSizeBytes(),
                entity.getContentSha256(),
                entity.getOriginalFileName(),
                entity.getUploadedBy(),
                entity.getUploadedAt()
        );
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
