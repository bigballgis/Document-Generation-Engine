package com.bank.docgen.authoring.structured;

import com.bank.docgen.sharedkernel.document.RenderProfile;
import com.bank.docgen.rendering.persistence.PreviewRecordEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Resolves publish-locked render profiles for preview and runtime generation (P18-T08).
 */
@Service
public class RenderProfileService {

    public static final String DEFAULT_RENDER_PROFILE_VERSION = "rp-v1";

    private static final String DEFAULT_PROFILE_RESOURCE = "authoring/default-render-profile-v1.json";

    private final ObjectMapper objectMapper;
    private final RenderProfile defaultProfile;

    public RenderProfileService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.defaultProfile = loadDefaultProfile();
    }

    public RenderProfile defaultProfile() {
        return defaultProfile;
    }

    public void lockForPublish(TemplateVersionEntity version) {
        if (version.getRenderProfileVersion() != null && !version.getRenderProfileVersion().isBlank()) {
            return;
        }
        RenderProfile profile = defaultProfile;
        version.setRenderProfileVersion(profile.renderProfileVersion());
        version.setRenderProfileJson(writeProfile(profile));
    }

    public RenderProfile resolveEffectiveProfile(TemplateVersionEntity version, CallerRenderOverride callerOverride) {
        RenderProfile locked = resolveLockedOrDraft(version);
        if (callerOverride != null && !callerOverride.isEmpty()) {
            return locked;
        }
        return locked;
    }

    public void applyPreviewRenderProfileVersion(PreviewRecordEntity preview, TemplateVersionEntity version) {
        preview.setRenderProfileVersion(resolveRenderProfileVersion(version));
    }

    public String resolveRenderProfileVersion(TemplateVersionEntity version) {
        if (version.getRenderProfileVersion() != null && !version.getRenderProfileVersion().isBlank()) {
            return version.getRenderProfileVersion();
        }
        return defaultProfile.renderProfileVersion();
    }

    private RenderProfile resolveLockedOrDraft(TemplateVersionEntity version) {
        if (version.getRenderProfileJson() != null && !version.getRenderProfileJson().isBlank()) {
            try {
                return RenderProfile.fromJsonNode(objectMapper.readTree(version.getRenderProfileJson()));
            } catch (JsonProcessingException ex) {
                return defaultProfile;
            } catch (IllegalArgumentException ex) {
                if ("api.error.rendering.renderProfileInvalid".equals(ex.getMessage())) {
                    throw new StructuredContentSchemaException("api.error.rendering.renderProfileInvalid");
                }
                throw ex;
            }
        }
        return defaultProfile;
    }

    private String writeProfile(RenderProfile profile) {
        try {
            return objectMapper.writeValueAsString(profile);
        } catch (JsonProcessingException ex) {
            throw new StructuredContentSchemaException("api.error.rendering.renderProfileInvalid");
        }
    }

    private RenderProfile loadDefaultProfile() {
        try (InputStream inputStream = new ClassPathResource(DEFAULT_PROFILE_RESOURCE).getInputStream()) {
            JsonNode root = objectMapper.readTree(inputStream);
            return RenderProfile.fromJsonNode(root);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load default render profile", ex);
        }
    }
}
