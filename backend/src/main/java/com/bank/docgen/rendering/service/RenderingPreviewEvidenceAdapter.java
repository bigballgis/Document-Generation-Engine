package com.bank.docgen.rendering.service;

import com.bank.docgen.rendering.domain.PreviewStatus;
import com.bank.docgen.rendering.persistence.BatchTestRunRepository;
import com.bank.docgen.rendering.persistence.PreviewRecordRepository;
import com.bank.docgen.template.port.BatchTestRunGateSnapshot;
import com.bank.docgen.template.port.PreviewEvidencePort;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RenderingPreviewEvidenceAdapter implements PreviewEvidencePort {

    private final PreviewRecordRepository previewRecordRepository;
    private final BatchTestRunRepository batchTestRunRepository;

    public RenderingPreviewEvidenceAdapter(
            PreviewRecordRepository previewRecordRepository,
            BatchTestRunRepository batchTestRunRepository
    ) {
        this.previewRecordRepository = previewRecordRepository;
        this.batchTestRunRepository = batchTestRunRepository;
    }

    @Override
    public int countSuccessfulPreviews(UUID templateId, UUID templateVersionId) {
        return previewRecordRepository
                .findByTemplateIdAndTemplateVersionIdAndStatus(templateId, templateVersionId, PreviewStatus.SUCCEEDED)
                .size();
    }

    @Override
    public int countFailedPreviews(UUID templateId, UUID templateVersionId) {
        return previewRecordRepository
                .findByTemplateIdAndTemplateVersionIdAndStatus(templateId, templateVersionId, PreviewStatus.FAILED)
                .size();
    }

    @Override
    public Set<String> successfulPreviewTestDataSetExternalIds(UUID templateId, UUID templateVersionId) {
        Set<String> ids = new HashSet<>();
        previewRecordRepository
                .findByTemplateIdAndTemplateVersionIdAndStatus(templateId, templateVersionId, PreviewStatus.SUCCEEDED)
                .forEach(preview -> {
                    if (preview.getTestDataSetExternalId() != null) {
                        ids.add(preview.getTestDataSetExternalId());
                    }
                });
        return ids;
    }

    @Override
    public Optional<BatchTestRunGateSnapshot> latestBatchTestRun(UUID templateId) {
        var runs = batchTestRunRepository.findByTemplateIdOrderByCreatedAtDesc(templateId);
        if (runs.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new BatchTestRunGateSnapshot(runs.getFirst().getBlockerCount()));
    }
}
