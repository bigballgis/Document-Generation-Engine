package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authoring.structured.FidelityValidationService;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VersionFidelityWarningServiceTest {

    private static final UUID VERSION_ID = UUID.randomUUID();
    private static final UUID MASTER_ID = UUID.randomUUID();

    @Mock
    private FidelityValidationService fidelityValidationService;

    private VersionFidelityWarningService service;

    @BeforeEach
    void setUp() {
        service = new VersionFidelityWarningService(fidelityValidationService, new ObjectMapper());
    }

    @Test
    void snapshotOnPublish_persistsWarningCodesJson() {
        TemplateVersionEntity version = publishedVersion();
        when(fidelityValidationService.collectWarningCodesForVersion(VERSION_ID, MASTER_ID))
                .thenReturn(List.of("IMAGE_SCALING_ADJUSTED", "FONT_SUBSTITUTION"));

        service.snapshotOnPublish(version, MASTER_ID);

        assertThat(version.getFidelityWarningCodesJson())
                .isEqualTo("[\"IMAGE_SCALING_ADJUSTED\",\"FONT_SUBSTITUTION\"]");
    }

    @Test
    void resolveWarningCodes_usesCacheForPublishedVersion() {
        TemplateVersionEntity version = publishedVersion();
        version.setFidelityWarningCodesJson("[\"CACHED_WARNING\"]");

        List<String> codes = service.resolveWarningCodes(version, MASTER_ID);

        assertThat(codes).containsExactly("CACHED_WARNING");
        verify(fidelityValidationService, never()).collectWarningCodesForVersion(VERSION_ID, MASTER_ID);
    }

    @Test
    void resolveWarningCodes_recomputesForDraftVersion() {
        TemplateVersionEntity version = publishedVersion();
        version.setLifecycleStatus(TemplateLifecycleStatus.DRAFT);
        version.setFidelityWarningCodesJson("[\"STALE\"]");
        when(fidelityValidationService.collectWarningCodesForVersion(VERSION_ID, MASTER_ID))
                .thenReturn(List.of("LIVE_WARNING"));

        List<String> codes = service.resolveWarningCodes(version, MASTER_ID);

        assertThat(codes).containsExactly("LIVE_WARNING");
    }

    private TemplateVersionEntity publishedVersion() {
        TemplateVersionEntity version = new TemplateVersionEntity(VERSION_ID, UUID.randomUUID(), "10000001");
        version.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        version.setReleaseVersion("1.0.0");
        return version;
    }
}
