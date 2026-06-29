package com.bank.docgen.authoring.structured;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.rendering.persistence.PreviewRecordEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RenderProfileServiceTest {

    private final RenderProfileService service = new RenderProfileService(new ObjectMapper());

    @Test
    void publish_locksRenderProfileVersion() {
        TemplateVersionEntity version = new TemplateVersionEntity(UUID.randomUUID(), UUID.randomUUID(), "10000001");

        assertThat(version.getRenderProfileVersion()).isNull();
        assertThat(version.getRenderProfileJson()).isNull();

        service.lockForPublish(version);

        assertThat(version.getRenderProfileVersion()).isEqualTo(RenderProfileService.DEFAULT_RENDER_PROFILE_VERSION);
        assertThat(version.getRenderProfileJson()).isNotBlank();
        assertThat(service.resolveEffectiveProfile(version, callerOverride("STRETCH", "FAST")).renderProfileVersion())
                .isEqualTo(RenderProfileService.DEFAULT_RENDER_PROFILE_VERSION);
    }

    @Test
    void runtime_ignoresCallerRenderOverride() {
        TemplateVersionEntity version = new TemplateVersionEntity(UUID.randomUUID(), UUID.randomUUID(), "10000001");
        service.lockForPublish(version);
        RenderProfile locked = service.resolveEffectiveProfile(version, CallerRenderOverride.empty());

        RenderProfile effective = service.resolveEffectiveProfile(
                version,
                callerOverride("STRETCH", "LOSSLESS")
        );

        assertThat(effective).isEqualTo(locked);
        assertThat(effective.imageScalingPolicy()).isEqualTo(locked.imageScalingPolicy());
        assertThat(effective.pdfConversionPolicy()).isEqualTo(locked.pdfConversionPolicy());
        assertThat(effective.imageScalingPolicy()).isNotEqualTo("STRETCH");
        assertThat(effective.pdfConversionPolicy()).isNotEqualTo("LOSSLESS");
    }

    @Test
    void previewRecord_carriesRenderProfileVersion() {
        TemplateVersionEntity version = new TemplateVersionEntity(UUID.randomUUID(), UUID.randomUUID(), "10000001");
        service.lockForPublish(version);
        PreviewRecordEntity preview = new PreviewRecordEntity(
                UUID.randomUUID(),
                version.getTemplateId(),
                version.getId(),
                "DOCX",
                "abc123",
                "10000001",
                null,
                null
        );

        service.applyPreviewRenderProfileVersion(preview, version);

        assertThat(preview.getRenderProfileVersion())
                .isEqualTo(RenderProfileService.DEFAULT_RENDER_PROFILE_VERSION);
    }

    private CallerRenderOverride callerOverride(String imageScalingPolicy, String pdfConversionPolicy) {
        return new CallerRenderOverride(
                imageScalingPolicy,
                pdfConversionPolicy,
                "FLOAT",
                "SKIP",
                "RELAXED"
        );
    }
}
