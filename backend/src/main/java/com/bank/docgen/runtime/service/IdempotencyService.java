package com.bank.docgen.runtime.service;

import com.bank.docgen.runtime.persistence.GenerationIdempotencyEntity;
import com.bank.docgen.runtime.persistence.GenerationIdempotencyRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotencyService {

    private final GenerationIdempotencyRepository repository;
    private final IdempotencyCachePort idempotencyCachePort;

    public IdempotencyService(
            GenerationIdempotencyRepository repository,
            IdempotencyCachePort idempotencyCachePort
    ) {
        this.repository = repository;
        this.idempotencyCachePort = idempotencyCachePort;
    }

    @Transactional
    public Optional<GenerationIdempotencyEntity> findExisting(String idempotencyKey, UUID templateId, String requestHash) {
        // The database record is authoritative for conflict detection. A still-live
        // record with a different request hash is an idempotency conflict (ADR 0004),
        // not a "no record" signal â€?returning empty here previously caused begin() to
        // hit the unique constraint and surface a 500.
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
        return Optional.of(record);
    }

    @Transactional
    public GenerationIdempotencyEntity begin(String idempotencyKey, UUID templateId, String requestHash) {
        GenerationIdempotencyEntity entity = new GenerationIdempotencyEntity(
                UUID.randomUUID(),
                idempotencyKey,
                templateId,
                requestHash,
                "IN_PROGRESS",
                Instant.now().plusSeconds(86400)
        );
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
                Instant.now().plusSeconds(86400)
        );
        entity.complete(storageKey, documentId);
        repository.save(entity);
    }

    public String hashRequest(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            return payload;
        }
    }

    private String cacheKey(UUID templateId, String idempotencyKey) {
        return templateId + ":" + idempotencyKey;
    }
}
