package com.bank.docgen.runtime.service;

import com.bank.docgen.runtime.api.GenerateRequestBody;
import com.bank.docgen.runtime.persistence.GenerationIdempotencyEntity;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * Package-private idempotency lookup / default-route conflict helpers for RuntimeGenerationService.
 */
final class RuntimeGenerationIdempotencySupport {

    private final IdempotencyService idempotencyService;
    private final TemplateVersionRepository templateVersionRepository;

    RuntimeGenerationIdempotencySupport(
            IdempotencyService idempotencyService,
            TemplateVersionRepository templateVersionRepository
    ) {
        this.idempotencyService = idempotencyService;
        this.templateVersionRepository = templateVersionRepository;
    }

    Optional<GenerationIdempotencyEntity> findExistingIdempotency(
            GenerateRequestBody request,
            TemplateEntity template,
            String explicitReleaseVersion,
            String resolvedVersion,
            String requestHash,
            BiFunction<GenerateRequestBody, String, String> writeRequest
    ) {
        try {
            return idempotencyService.findExisting(request.idempotencyKey(), template.getId(), requestHash);
        } catch (IdempotencyConflictException ex) {
            if (explicitReleaseVersion != null) {
                throw ex;
            }
            Optional<GenerationIdempotencyEntity> stored = idempotencyService.findLiveRecord(
                    request.idempotencyKey(),
                    template.getId()
            );
            if (stored.isPresent()) {
                String originalVersion = findMatchingReleaseVersion(request, stored.get(), template.getId(), writeRequest);
                if (originalVersion != null && !originalVersion.equals(resolvedVersion)) {
                    throw IdempotencyConflictException.defaultRouteChanged(
                            request.idempotencyKey(),
                            originalVersion
                    );
                }
            }
            throw ex;
        }
    }

    private String findMatchingReleaseVersion(
            GenerateRequestBody request,
            GenerationIdempotencyEntity stored,
            UUID templateId,
            BiFunction<GenerateRequestBody, String, String> writeRequest
    ) {
        String cached = stored.getResolvedReleaseVersion();
        if (cached != null && !cached.isBlank()) {
            return cached;
        }
        return templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId).stream()
                .filter(version -> version.getLifecycleStatus() == TemplateLifecycleStatus.PUBLISHED)
                .map(TemplateVersionEntity::getReleaseVersion)
                .filter(releaseVersion -> releaseVersion != null && !releaseVersion.isBlank())
                .filter(releaseVersion -> idempotencyService.hashRequest(writeRequest.apply(request, releaseVersion))
                        .equals(stored.getRequestHash()))
                .findFirst()
                .orElse(null);
    }
}
