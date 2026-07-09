package com.bank.docgen.runtime.service;

import com.bank.docgen.runtime.persistence.GenerationIdempotencyEntity;
import com.bank.docgen.runtime.persistence.GenerationIdempotencyRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Lazy
public class IdempotencyService {

    private final GenerationIdempotencyRepository repository;
    private final IdempotencyCachePort idempotencyCachePort;

    @Autowired
    public IdempotencyService(
            GenerationIdempotencyRepository repository,
            IdempotencyCachePort idempotencyCachePort
    ) {
        this.repository = repository;
        this.idempotencyCachePort = idempotencyCachePort;
    }

    @Transactional(readOnly = true)
    public Optional<GenerationIdempotencyEntity> findLiveRecord(String idempotencyKey, UUID templateId) {
        return repository.findByIdempotencyKeyAndTemplateId(idempotencyKey, templateId)
                .filter(record -> record.getExpiresAt().isAfter(Instant.now()));
    }

    @Transactional
    public Optional<GenerationIdempotencyEntity> findExisting(String idempotencyKey, UUID templateId, String requestHash) {
        // The database record is authoritative for conflict detection. A still-live
        // record with a different request hash is an idempotency conflict (ADR 0004),
        // not a "no record" signal -- returning empty here previously caused begin() to
        // hit the unique constraint and surface a 500.
        String cacheLookupKey = cacheKey(templateId, idempotencyKey);
        Optional<String> cachedRequestHash = idempotencyCachePort.findRequestHash(cacheLookupKey);
        if (cachedRequestHash.isPresent() && !cachedRequestHash.get().equals(requestHash)) {
            throw new IdempotencyConflictException(idempotencyKey);
        }

        Optional<GenerationIdempotencyEntity> live = repository
                .findByIdempotencyKeyAndTemplateId(idempotencyKey, templateId)
                .filter(record -> record.getExpiresAt().isAfter(Instant.now()));
        if (live.isEmpty()) {
            return Optional.empty();
        }
        GenerationIdempotencyEntity record = live.get();
        if (!record.getRequestHash().equals(requestHash)) {
            throw new IdempotencyConflictException(idempotencyKey);
        }
        if (cachedRequestHash.isEmpty()) {
            idempotencyCachePort.remember(cacheLookupKey, requestHash, record.getExpiresAt());
        }
        return Optional.of(record);
    }

    @Transactional
    public GenerationIdempotencyEntity begin(String idempotencyKey, UUID templateId, String requestHash) {
        return begin(idempotencyKey, templateId, requestHash, null);
    }

    @Transactional
    public GenerationIdempotencyEntity begin(
            String idempotencyKey,
            UUID templateId,
            String requestHash,
            String resolvedReleaseVersion
    ) {
        GenerationIdempotencyEntity entity = new GenerationIdempotencyEntity(
                UUID.randomUUID(),
                idempotencyKey,
                templateId,
                requestHash,
                "IN_PROGRESS",
                Instant.now().plusSeconds(IdempotencyConstants.RETENTION_SECONDS)
        );
        entity.setResolvedReleaseVersion(resolvedReleaseVersion);
        GenerationIdempotencyEntity saved;
        try {
            saved = repository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException ex) {
            // Concurrent begin() for the same (idempotencyKey, templateId): re-read the
            // winning record. Same semantics -> replay it; different semantics -> conflict.
            GenerationIdempotencyEntity existing = repository
                    .findByIdempotencyKeyAndTemplateId(idempotencyKey, templateId)
                    .orElseThrow(() -> ex);
            if (!existing.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException(idempotencyKey);
            }
            return existing;
        }
        idempotencyCachePort.remember(cacheKey(templateId, idempotencyKey), requestHash, saved.getExpiresAt());
        return saved;
    }

    @Transactional
    public void complete(GenerationIdempotencyEntity entity, String storageKey, String documentId) {
        entity.complete(storageKey, documentId);
        repository.save(entity);
        idempotencyCachePort.remember(
                cacheKey(entity.getTemplateId(), entity.getIdempotencyKey()),
                entity.getRequestHash(),
                entity.getExpiresAt()
        );
    }

    @Transactional
    public void registerDownloadableDocument(UUID templateId, String documentId, String storageKey) {
        String idempotencyKey = "artifact-" + documentId;
        if (repository.findByIdempotencyKeyAndTemplateId(idempotencyKey, templateId).isPresent()) {
            return;
        }
        GenerationIdempotencyEntity entity = new GenerationIdempotencyEntity(
                UUID.randomUUID(),
                idempotencyKey,
                templateId,
                idempotencyKey,
                "COMPLETED",
                Instant.now().plusSeconds(IdempotencyConstants.RETENTION_SECONDS)
        );
        entity.complete(storageKey, documentId);
        repository.save(entity);
    }

    public String hashRequest(String payload) {
        try {
            MessageDigest digest = newDigest();
            return HexFormat.of().formatHex(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            // LR-B7 (OPT-E9): hard failure by design. Falling back to the raw payload would
            // silently weaken idempotency semantics and persist raw variable values.
            throw new IdempotencyDigestException(ex);
        }
    }

    /** Seam for tests to simulate digest unavailability. */
    protected MessageDigest newDigest() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256");
    }

    private String cacheKey(UUID templateId, String idempotencyKey) {
        return templateId + ":" + idempotencyKey;
    }
}
