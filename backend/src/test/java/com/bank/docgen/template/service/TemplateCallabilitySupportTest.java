package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TemplateCallabilitySupportTest {

    private static final UUID TEMPLATE_ID = UUID.randomUUID();

    @Test
    void allowsPublishedVersionWhenTemplateReleaseVersionDiffers() {
        TemplateEntity template = publishedTemplate("2.0.0");
        TemplateVersionEntity version = publishedVersion("1.0.0");

        assertThatCode(() -> TemplateCallabilitySupport.assertReleaseVersionCallable(template, version, "1.0.0"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsStoppedTemplate() {
        TemplateEntity template = publishedTemplate("1.0.0");
        template.setLifecycleStatus(TemplateLifecycleStatus.STOPPED);
        TemplateVersionEntity version = publishedVersion("1.0.0");

        assertThatThrownBy(() -> TemplateCallabilitySupport.assertReleaseVersionCallable(template, version, "1.0.0"))
                .isInstanceOf(TemplateValidationException.class)
                .hasFieldOrPropertyWithValue("messageKey", "api.error.runtime.versionNotCallable");
    }

    @Test
    void rejectsStoppedVersionWhileTemplateRemainsPublished() {
        TemplateEntity template = publishedTemplate("1.0.0");
        TemplateVersionEntity version = publishedVersion("1.0.0");
        version.setLifecycleStatus(TemplateLifecycleStatus.STOPPED);

        assertThatThrownBy(() -> TemplateCallabilitySupport.assertReleaseVersionCallable(template, version, "1.0.0"))
                .isInstanceOf(TemplateValidationException.class)
                .hasFieldOrPropertyWithValue("messageKey", "api.error.runtime.versionNotCallable");
    }

    private TemplateEntity publishedTemplate(String releaseVersion) {
        TemplateEntity template = new TemplateEntity(
                TEMPLATE_ID,
                "TPL-001",
                "RETAIL",
                "Sample",
                null,
                UUID.randomUUID(),
                "10000001"
        );
        template.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        template.setReleaseVersion(releaseVersion);
        return template;
    }

    private TemplateVersionEntity publishedVersion(String releaseVersion) {
        TemplateVersionEntity version = new TemplateVersionEntity(UUID.randomUUID(), TEMPLATE_ID, "10000001");
        version.setReleaseVersion(releaseVersion);
        version.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        return version;
    }
}
