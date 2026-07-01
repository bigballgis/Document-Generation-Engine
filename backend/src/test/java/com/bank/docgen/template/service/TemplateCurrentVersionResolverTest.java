package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TemplateCurrentVersionResolverTest {

    @Mock
    private TemplateVersionRepository templateVersionRepository;

    private TemplateCurrentVersionResolver resolver;
    private UUID templateId;

    @BeforeEach
    void setUp() {
        resolver = new TemplateCurrentVersionResolver(templateVersionRepository);
        templateId = UUID.randomUUID();
    }

    @Test
    void findInFlightDevVersion_returnsHighestUnpublishedRow() {
        TemplateVersionEntity published = version(1, "1.0.0");
        TemplateVersionEntity inFlight = version(2, null);
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(inFlight, published));

        Optional<TemplateVersionEntity> result = resolver.findInFlightDevVersion(templateId);

        assertThat(result).contains(inFlight);
    }

    @Test
    void hasInFlightDevVersion_isFalseWhenOnlyPublishedRowsExist() {
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(version(1, "1.0.0")));

        assertThat(resolver.hasInFlightDevVersion(templateId)).isFalse();
    }

    @Test
    void requireMutableInFlightDevVersion_throwsWhenPublishedOnly() {
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(version(1, "1.0.0")));

        assertThatThrownBy(() -> resolver.requireMutableInFlightDevVersion(templateId))
                .isInstanceOf(TemplateGovernanceException.class)
                .extracting(ex -> ((TemplateGovernanceException) ex).errorCode())
                .isEqualTo("TEMPLATE_VERSION_IMMUTABLE");
    }

    @Test
    void listVersionLinesOrdered_placesInFlightBeforePublished() {
        TemplateVersionEntity published = version(1, "1.0.0");
        TemplateVersionEntity inFlight = version(2, null);
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(inFlight, published));

        List<TemplateVersionEntity> ordered = resolver.listVersionLinesOrdered(templateId);

        assertThat(ordered).containsExactly(inFlight, published);
    }

    @Test
    void maxDevVersionNumber_returnsHighestNumber() {
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(version(3, null), version(2, "1.0.0"), version(1, "0.9.0")));

        assertThat(resolver.maxDevVersionNumber(templateId)).isEqualTo(3);
    }

    private TemplateVersionEntity version(int devVersionNumber, String releaseVersion) {
        TemplateVersionEntity entity = new TemplateVersionEntity(UUID.randomUUID(), templateId, "10000001");
        entity.setDevVersionNumber(devVersionNumber);
        if (releaseVersion != null) {
            entity.setReleaseVersion(releaseVersion);
        }
        return entity;
    }
}
