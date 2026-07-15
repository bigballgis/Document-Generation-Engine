package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReleaseBundleFingerprintSupportTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID VERSION_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final String HASH = "a".repeat(64);

    @Mock
    private TemplateVersionRepository templateVersionRepository;

    private ReleaseBundleFingerprintSupport support;

    @BeforeEach
    void setUp() {
        support = new ReleaseBundleFingerprintSupport(templateVersionRepository);
    }

    @Test
    void resolve_returnsSnapshotIdAndHashForPublishedRelease() {
        TemplateVersionEntity version = new TemplateVersionEntity(VERSION_ID, TEMPLATE_ID, "U0000001");
        version.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        version.setMasterFileHash(HASH);
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(TEMPLATE_ID, "1.0.0"))
                .thenReturn(Optional.of(version));

        Optional<ReleaseBundleFingerprint> fingerprint = support.resolve(TEMPLATE_ID, "1.0.0");

        assertThat(fingerprint).isPresent();
        assertThat(fingerprint.get().snapshotId()).isEqualTo(VERSION_ID);
        assertThat(fingerprint.get().bundleHash()).isEqualTo(HASH);
    }

    @Test
    void resolve_returnsEmptyWhenReleaseUnresolved() {
        assertThat(support.resolve(TEMPLATE_ID, null)).isEmpty();
        assertThat(support.resolve(TEMPLATE_ID, "  ")).isEmpty();
    }

    @Test
    void resolve_returnsEmptyWhenMasterFileHashMissing() {
        TemplateVersionEntity version = new TemplateVersionEntity(VERSION_ID, TEMPLATE_ID, "U0000001");
        version.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        version.setMasterFileHash(null);
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(TEMPLATE_ID, "1.0.0"))
                .thenReturn(Optional.of(version));

        assertThat(support.resolve(TEMPLATE_ID, "1.0.0")).isEmpty();
    }
}
