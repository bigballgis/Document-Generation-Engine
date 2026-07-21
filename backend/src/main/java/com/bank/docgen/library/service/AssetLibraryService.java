package com.bank.docgen.library.service;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.api.CatalogPageSupport;
import com.bank.docgen.authorization.management.api.PageView;
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
import java.util.HexFormat;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
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
    private final ManagementAuditRecorder auditRecorder;
    private final Clock clock;

    public AssetLibraryService(
            LibraryAssetRepository repository,
            ObjectStoragePort objectStoragePort,
            GroupAccessService groupAccessService,
            ManagementAuditRecorder auditRecorder,
            Clock clock
    ) {
        this.repository = repository;
        this.objectStoragePort = objectStoragePort;
        this.groupAccessService = groupAccessService;
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
            String q
    ) {
        requireListAccess(session);
        int normalizedPage = CatalogPageSupport.normalizePage(page);
        int normalizedSize = CatalogPageSupport.normalizeSize(size);
        AssetLibraryAssetStatus status = resolveListStatus(session, statusFilter);
        String query = CatalogPageSupport.blankToNull(q);
        Pageable pageable = PageRequest.of(
                normalizedPage,
                normalizedSize,
                Sort.by(Sort.Direction.DESC, "uploadedAt")
        );
        // Split null-q vs non-null-q: Postgres+Hibernate null String binds as bytea inside LOWER(CONCAT).
        Page<LibraryAssetEntity> result = query == null
                ? repository.search(status, assetClass, pageable)
                : repository.searchByQuery(status, assetClass, query, pageable);
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
            AssetLibraryAssetClass assetClass
    ) {
        requireUploadAccess(session, assetClass);
        String assetKey = AssetLibraryUploadValidator.normalizeAssetKey(assetKeyRaw);
        AssetLibraryUploadValidator.ValidatedPayload payload = AssetLibraryUploadValidator.validateFile(file);
        warnIfPrefixMismatch(assetKey, assetClass);

        LibraryAssetEntity existing = repository.findById(assetKey).orElse(null);
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
        storeObject(assetKey, payload.bytes(), payload.contentType());

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
                    assetKey,
                    assetClass.name(),
                    session.username(),
                    session.displayName(),
                    sha256
            );
        } else {
            saved = repository.save(new LibraryAssetEntity(
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
     * SYS-NORM Wave 7 / PP-C8 — materialize an asset binary from a promotion pack in the
     * same transaction as template import. Fail-closed on conflict with an ACTIVE key.
     */
    @Transactional
    public AssetLibraryAssetView materializeImportedAsset(
            ManagementSessionClaims session,
            String assetKeyRaw,
            AssetLibraryAssetClass assetClass,
            byte[] bytes,
            String contentType,
            String originalFileName
    ) {
        requireUploadAccess(session, assetClass == null ? AssetLibraryAssetClass.OTHER : assetClass);
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

        LibraryAssetEntity existing = repository.findById(assetKey).orElse(null);
        if (existing != null && existing.getStatus() == AssetLibraryAssetStatus.ACTIVE) {
            // Already present — treat as idempotent success for import materialize.
            return toView(existing);
        }

        Instant now = clock.instant();
        String sha256 = sha256Hex(bytes);
        storeObject(assetKey, bytes, resolvedType);
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
    public AssetLibraryAssetView disable(ManagementSessionClaims session, String assetKeyRaw) {
        requireDisableAccess(session);
        String assetKey = AssetLibraryUploadValidator.normalizeAssetKey(assetKeyRaw);
        LibraryAssetEntity entity = repository.findById(assetKey)
                .orElseThrow(() -> new AssetLibraryNotFoundException("api.error.assetLibrary.assetNotFound"));

        // E02-C6 fail-closed: remove resolvable objects first; only then commit DISABLED.
        // Storage errors / post-delete existence must abort before catalog status changes.
        deleteResolvableObjects(assetKey);

        if (entity.getStatus() == AssetLibraryAssetStatus.DISABLED) {
            return toView(entity);
        }

        Instant now = clock.instant();
        entity.markDisabled(now);
        LibraryAssetEntity saved = repository.save(entity);
        auditRecorder.recordAssetLibraryDisable(
                assetKey,
                saved.getAssetClass().name(),
                session.username(),
                session.displayName(),
                saved.getContentSha256()
        );
        return toView(saved);
    }

    private void storeObject(String assetKey, byte[] bytes, String contentType) {
        objectStoragePort.put(assetKey, new ByteArrayInputStream(bytes), bytes.length, contentType);
    }

    private void deleteResolvableObjects(String assetKey) {
        ensureObjectRemoved(assetKey);
        if (!assetKey.contains(".")) {
            ensureObjectRemoved(assetKey + ".png");
            ensureObjectRemoved(assetKey + ".jpg");
            ensureObjectRemoved(assetKey + ".jpeg");
        }
    }

    /**
     * Delete-then-verify: always attempt delete (MinIO remove is idempotent for missing keys),
     * then require {@code exists == false}. Ambiguous storage errors from {@code exists}/{@code delete}
     * propagate as {@link com.bank.docgen.infrastructure.storage.ObjectStorageException}.
     */
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
