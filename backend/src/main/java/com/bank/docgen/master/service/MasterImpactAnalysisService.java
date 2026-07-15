package com.bank.docgen.master.service;

import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.api.MasterAnchorSetDeltaView;
import com.bank.docgen.master.api.MasterImpactAnalysisView;
import com.bank.docgen.master.api.MasterReferencedTemplateView;
import com.bank.docgen.master.api.MasterRevisionDiffView;
import com.bank.docgen.master.persistence.MasterRevisionLineAnchorEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.master.service.MasterAnchorSetDeltaCalculator.AnchorRef;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Real master impact analysis and revision diff (CE-K05).
 */
@Service
public class MasterImpactAnalysisService {

    private final MasterDocumentAccessSupport access;
    private final MasterRevisionLineRepository masterRevisionLineRepository;
    private final TemplateRepository templateRepository;
    private final ObjectStoragePort objectStoragePort;
    private final GroupAccessService groupAccessService;

    public MasterImpactAnalysisService(
            com.bank.docgen.master.persistence.MasterDocumentRepository masterDocumentRepository,
            MasterRevisionLineRepository masterRevisionLineRepository,
            TemplateRepository templateRepository,
            ObjectStoragePort objectStoragePort,
            GroupAccessService groupAccessService
    ) {
        this.access = new MasterDocumentAccessSupport(masterDocumentRepository, groupAccessService);
        this.masterRevisionLineRepository = masterRevisionLineRepository;
        this.templateRepository = templateRepository;
        this.objectStoragePort = objectStoragePort;
        this.groupAccessService = groupAccessService;
    }

    @Transactional(readOnly = true)
    public MasterImpactAnalysisView impactAnalysis(UUID masterId, ManagementSessionClaims session) {
        access.requireReadableMaster(masterId, session);
        List<MasterReferencedTemplateView> referenced = loadReferencedTemplates(masterId, session);
        MasterAnchorSetDeltaView delta = computeCurrentVsPreviousDelta(masterId);
        boolean retestRequired = !referenced.isEmpty() && !delta.isEmpty();
        List<String> ids = referenced.stream().map(MasterReferencedTemplateView::templateId).toList();
        return new MasterImpactAnalysisView(
                masterId.toString(),
                ids,
                referenced,
                retestRequired,
                delta
        );
    }

    @Transactional(readOnly = true)
    public MasterRevisionDiffView revisionDiff(
            UUID masterId,
            UUID baselineRevisionLineId,
            UUID candidateRevisionLineId,
            ManagementSessionClaims session
    ) {
        access.requireReadableMaster(masterId, session);
        MasterRevisionLineEntity baseline = resolveBaseline(masterId, baselineRevisionLineId);
        MasterRevisionLineEntity candidate = resolveCandidate(masterId, candidateRevisionLineId);
        MasterAnchorSetDeltaView delta = MasterAnchorSetDeltaCalculator.compute(
                toRefs(baseline.getAnchors()),
                toRefs(candidate.getAnchors())
        );
        return new MasterRevisionDiffView(
                masterId.toString(),
                baseline.getId().toString(),
                candidate.getId().toString(),
                delta.addedAnchors(),
                delta.removedAnchors(),
                delta.renamedAnchors(),
                sha256OfStorageKey(baseline.getStorageKey()),
                sha256OfStorageKey(candidate.getStorageKey())
        );
    }

    private List<MasterReferencedTemplateView> loadReferencedTemplates(
            UUID masterId,
            ManagementSessionClaims session
    ) {
        return templateRepository.findByMasterIdAndDeletedAtIsNull(masterId).stream()
                .filter(template -> groupAccessService.canAccessGroup(session, template.getGroupCode()))
                .sorted(Comparator.comparing(TemplateEntity::getName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(t -> t.getId().toString()))
                .map(template -> new MasterReferencedTemplateView(
                        template.getId().toString(),
                        template.getName(),
                        template.getLifecycleStatus().name(),
                        template.getExternalId()
                ))
                .toList();
    }

    private MasterAnchorSetDeltaView computeCurrentVsPreviousDelta(UUID masterId) {
        Optional<MasterRevisionLineEntity> current =
                masterRevisionLineRepository.findWithAnchorsByMasterIdAndCurrentTrueAndDeletedAtIsNull(masterId);
        if (current.isEmpty() || current.get().getRevisionSequence() <= 1) {
            return new MasterAnchorSetDeltaView(List.of(), List.of(), List.of());
        }
        Optional<MasterRevisionLineEntity> previous =
                masterRevisionLineRepository.findWithAnchorsByMasterIdAndRevisionSequenceAndDeletedAtIsNull(
                        masterId,
                        current.get().getRevisionSequence() - 1
                );
        if (previous.isEmpty()) {
            return new MasterAnchorSetDeltaView(List.of(), List.of(), List.of());
        }
        return MasterAnchorSetDeltaCalculator.compute(
                toRefs(previous.get().getAnchors()),
                toRefs(current.get().getAnchors())
        );
    }

    private MasterRevisionLineEntity resolveBaseline(UUID masterId, UUID baselineRevisionLineId) {
        if (baselineRevisionLineId != null) {
            return requireRevisionLine(masterId, baselineRevisionLineId);
        }
        MasterRevisionLineEntity current = masterRevisionLineRepository
                .findWithAnchorsByMasterIdAndCurrentTrueAndDeletedAtIsNull(masterId)
                .orElseThrow(MasterNotFoundException::new);
        if (current.getRevisionSequence() <= 1) {
            throw new MasterValidationException("api.error.master.revisionDiffBaselineUnavailable");
        }
        return masterRevisionLineRepository
                .findWithAnchorsByMasterIdAndRevisionSequenceAndDeletedAtIsNull(
                        masterId,
                        current.getRevisionSequence() - 1
                )
                .orElseThrow(MasterNotFoundException::new);
    }

    private MasterRevisionLineEntity resolveCandidate(UUID masterId, UUID candidateRevisionLineId) {
        if (candidateRevisionLineId != null) {
            return requireRevisionLine(masterId, candidateRevisionLineId);
        }
        return masterRevisionLineRepository
                .findWithAnchorsByMasterIdAndCurrentTrueAndDeletedAtIsNull(masterId)
                .orElseThrow(MasterNotFoundException::new);
    }

    private MasterRevisionLineEntity requireRevisionLine(UUID masterId, UUID revisionLineId) {
        return masterRevisionLineRepository.findByIdAndMasterIdAndDeletedAtIsNull(revisionLineId, masterId)
                .orElseThrow(MasterNotFoundException::new);
    }

    private static List<AnchorRef> toRefs(List<MasterRevisionLineAnchorEntity> anchors) {
        return anchors.stream()
                .map(anchor -> new AnchorRef(anchor.getAnchorId(), anchor.getDocumentSequence()))
                .toList();
    }

    private String sha256OfStorageKey(String storageKey) {
        try (InputStream stream = objectStoragePort.get(storageKey)) {
            return sha256Hex(stream.readAllBytes());
        } catch (IOException ex) {
            throw new MasterValidationException("api.error.master.downloadFailed");
        }
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
