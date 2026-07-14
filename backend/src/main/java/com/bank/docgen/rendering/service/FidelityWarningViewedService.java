package com.bank.docgen.rendering.service;

import com.bank.docgen.rendering.api.FidelityWarningView;
import com.bank.docgen.rendering.api.PreviewRecordView;
import com.bank.docgen.rendering.persistence.PreviewRecordEntity;
import com.bank.docgen.rendering.persistence.PreviewRecordRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.port.TemplatePreviewAuthorizationPort;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FidelityWarningViewedService {

    private final TemplatePreviewAuthorizationPort previewAuthorizationPort;
    private final PreviewRecordRepository previewRecordRepository;
    private final FidelityWarningJsonSupport fidelityWarningJsonSupport;
    private final PreviewGenerationService previewGenerationService;

    public FidelityWarningViewedService(
            TemplatePreviewAuthorizationPort previewAuthorizationPort,
            PreviewRecordRepository previewRecordRepository,
            FidelityWarningJsonSupport fidelityWarningJsonSupport,
            PreviewGenerationService previewGenerationService
    ) {
        this.previewAuthorizationPort = previewAuthorizationPort;
        this.previewRecordRepository = previewRecordRepository;
        this.fidelityWarningJsonSupport = fidelityWarningJsonSupport;
        this.previewGenerationService = previewGenerationService;
    }

    @Transactional
    public PreviewRecordView markWarningViewed(
            UUID templateId,
            UUID previewId,
            int warningIndex,
            ManagementSessionClaims session
    ) {
        PreviewRecordEntity preview = previewRecordRepository.findById(previewId)
                .orElseThrow(PreviewNotFoundException::new);
        if (!preview.getTemplateId().equals(templateId)) {
            throw new PreviewNotFoundException();
        }
        previewAuthorizationPort.requireReadableSnapshot(templateId, session);

        List<FidelityWarningView> warnings = new ArrayList<>(
                fidelityWarningJsonSupport.readWarnings(preview.getFidelityWarningsJson())
        );
        if (warningIndex < 0 || warningIndex >= warnings.size()) {
            throw new PreviewValidationException("api.error.rendering.fidelityWarningIndexOutOfRange");
        }

        FidelityWarningView current = warnings.get(warningIndex);
        if (!Boolean.TRUE.equals(current.viewed())) {
            warnings.set(
                    warningIndex,
                    new FidelityWarningView(
                            current.code(),
                            current.messageKey(),
                            current.location(),
                            current.artifact(),
                            Boolean.TRUE
                    )
            );
            preview.updateFidelityWarningsJson(fidelityWarningJsonSupport.writeWarnings(warnings));
            previewRecordRepository.save(preview);
        }

        return previewGenerationService.getPreview(templateId, previewId, session);
    }
}
