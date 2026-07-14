package com.bank.docgen.template.service;

import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.persistence.MasterRevisionLineEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CE-K01: retroactively pins already-published template versions that predate the publish-time
 * pinning flow. For each PUBLISHED {@code template_version} row with a missing pin (revision id
 * or file hash), the service resolves the master's current revision, computes the DOCX SHA-256,
 * and writes {@code pinOrigin = PINNED_RETROACTIVELY}. The run is idempotent (only fills NULL
 * fields) and reports — never fakes — rows whose master/revision is unavailable
 * (BDD-CE-K01-019); those rows subsequently fail-closed at runtime per BDD-CE-K01-008.
 */
@Service
public class ReleaseBundleBackfillService {

    private static final Logger LOG = LoggerFactory.getLogger(ReleaseBundleBackfillService.class);

    private final TemplateVersionRepository templateVersionRepository;
    private final TemplateRepository templateRepository;
    private final MasterDocumentRepository masterDocumentRepository;
    private final MasterRevisionLineRepository masterRevisionLineRepository;
    private final ObjectStoragePort objectStoragePort;
    private final ObjectMapper objectMapper;

    public ReleaseBundleBackfillService(
            TemplateVersionRepository templateVersionRepository,
            TemplateRepository templateRepository,
            MasterDocumentRepository masterDocumentRepository,
            MasterRevisionLineRepository masterRevisionLineRepository,
            ObjectStoragePort objectStoragePort,
            ObjectMapper objectMapper
    ) {
        this.templateVersionRepository = templateVersionRepository;
        this.templateRepository = templateRepository;
        this.masterDocumentRepository = masterDocumentRepository;
        this.masterRevisionLineRepository = masterRevisionLineRepository;
        this.objectStoragePort = objectStoragePort;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ReleaseBundleBackfillResult runBackfill() {
        int pinned = 0;
        List<ReleaseBundleBackfillAnomaly> anomalies = new ArrayList<>();

        // Rows with revision id set but hash missing: recompute hash only (idempotent partial fill).
        for (TemplateVersionEntity version : templateVersionRepository
                .findByLifecycleStatusAndMasterRevisionIdNotNullAndDeletedAtIsNull(TemplateLifecycleStatus.PUBLISHED)) {
            if (version.getMasterFileHash() != null) {
                continue;
            }
            String hash = computeHashForExistingPin(version);
            if (hash == null) {
                anomalies.add(new ReleaseBundleBackfillAnomaly(
                        version.getTemplateId(), version.getReleaseVersion(), "PINNED_REVISION_STORAGE_UNAVAILABLE"));
                continue;
            }
            version.setMasterFileHash(hash);
            templateVersionRepository.save(version);
            pinned++;
        }

        // Rows with no pin at all: resolve master's current revision + hash.
        for (TemplateVersionEntity version : templateVersionRepository
                .findByLifecycleStatusAndMasterRevisionIdIsNullAndDeletedAtIsNull(TemplateLifecycleStatus.PUBLISHED)) {
            ReleaseBundleBackfillAnomaly anomaly = pinRetroactively(version);
            if (anomaly == null) {
                templateVersionRepository.save(version);
                pinned++;
            } else {
                anomalies.add(anomaly);
            }
        }
        LOG.info("CE-K01 backfill complete: pinned={}, anomalies={}", pinned, anomalies.size());
        return new ReleaseBundleBackfillResult(pinned, anomalies.size(), List.copyOf(anomalies));
    }

    private ReleaseBundleBackfillAnomaly pinRetroactively(TemplateVersionEntity version) {
        TemplateEntity template = templateRepository.findById(version.getTemplateId()).orElse(null);
        if (template == null) {
            return anomaly(version, "TEMPLATE_MISSING");
        }
        MasterDocumentEntity master = masterDocumentRepository
                .findByIdAndDeletedAtIsNull(template.getMasterId()).orElse(null);
        if (master == null || master.getCurrentRevisionLineId() == null) {
            return anomaly(version, "MASTER_OR_CURRENT_REVISION_MISSING");
        }
        UUID revisionId = master.getCurrentRevisionLineId();
        MasterRevisionLineEntity revision = masterRevisionLineRepository
                .findByIdAndMasterIdAndDeletedAtIsNull(revisionId, master.getId()).orElse(null);
        if (revision == null) {
            return anomaly(version, "CURRENT_REVISION_LINE_DELETED");
        }
        String hash = readSha256(revision.getStorageKey());
        if (hash == null) {
            return anomaly(version, "REVISION_DOCX_STORAGE_UNAVAILABLE");
        }
        version.setMasterRevisionId(revisionId);
        version.setMasterFileHash(hash);
        version.setPinMetadataJson(writeMetadata(ReleaseBundlePinMetadata.retroactive(
                Instant.now().toString(), "system-backfill")));
        return null;
    }

    private String computeHashForExistingPin(TemplateVersionEntity version) {
        TemplateEntity template = templateRepository.findById(version.getTemplateId()).orElse(null);
        if (template == null) {
            return null;
        }
        MasterRevisionLineEntity revision = masterRevisionLineRepository
                .findByIdAndMasterIdAndDeletedAtIsNull(version.getMasterRevisionId(), template.getMasterId())
                .orElse(null);
        if (revision == null) {
            return null;
        }
        return readSha256(revision.getStorageKey());
    }

    private String readSha256(String storageKey) {
        try (InputStream stream = objectStoragePort.get(storageKey)) {
            return sha256Hex(stream.readAllBytes());
        } catch (IOException | RuntimeException ex) {
            LOG.warn("CE-K01 backfill hash computation failed for {}: {}", storageKey, ex.getMessage());
            return null;
        }
    }

    private String writeMetadata(ReleaseBundlePinMetadata metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (IOException ex) {
            return "{\"pinOrigin\":\"" + metadata.pinOrigin() + "\"}";
        }
    }

    private static ReleaseBundleBackfillAnomaly anomaly(TemplateVersionEntity version, String reason) {
        LOG.warn("CE-K01 backfill anomaly: template={}, releaseVersion={}, reason={}",
                version.getTemplateId(), version.getReleaseVersion(), reason);
        return new ReleaseBundleBackfillAnomaly(version.getTemplateId(), version.getReleaseVersion(), reason);
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(bytes);
            StringBuilder hex = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
