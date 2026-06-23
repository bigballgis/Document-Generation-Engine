package com.bank.docgen.runtime.service;

import com.bank.docgen.runtime.persistence.GenerationIdempotencyEntity;
import com.bank.docgen.runtime.persistence.GenerationIdempotencyRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
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
        String cacheKey = cacheKey(templateId, idempotencyKey);
        Optional<String> cachedHash = idempotencyCachePort.findRequestHash(cacheKey);
        if (cachedHash.isPresent() && !cachedHash.get().equals(requestHash)) {
            return Optional.empty();
        }
        return repository.findByIdempotencyKeyAndTemplateId(idempotencyKey, templateId)
                .filter(record -> record.getExpiresAt().isAfter(Instant.now()))
                .filter(record -> record.getRequestHash().equals(requestHash));
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
        GenerationIdempotencyEntity saved = repository.save(entity);
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
