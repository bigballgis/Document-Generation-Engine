package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
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
        TemplateVersionEntity published = version(1, "1.0.0", TemplateLifecycleStatus.PUBLISHED);
        TemplateVersionEntity inFlight = version(2, null, TemplateLifecycleStatus.DRAFT);
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(inFlight, published));

        Optional<TemplateVersionEntity> result = resolver.findInFlightDevVersion(templateId);

        assertThat(result).contains(inFlight);
    }

    @Test
    void hasInFlightDevVersion_isFalseWhenOnlyPublishedRowsExist() {
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(version(1, "1.0.0", TemplateLifecycleStatus.PUBLISHED)));

        assertThat(resolver.hasInFlightDevVersion(templateId)).isFalse();
    }

    @Test
    void requireMutableInFlightDevVersion_throwsWhenPublishedOnly() {
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(version(1, "1.0.0", TemplateLifecycleStatus.PUBLISHED)));

        assertThatThrownBy(() -> resolver.requireMutableInFlightDevVersion(templateId))
                .isInstanceOf(TemplateGovernanceException.class)
                .extracting(ex -> ((TemplateGovernanceException) ex).errorCode())
                .isEqualTo("TEMPLATE_VERSION_IMMUTABLE");
    }

    @Test
    void listVersionLinesOrdered_placesInFlightBeforePublished() {
        TemplateVersionEntity published = version(1, "1.0.0", TemplateLifecycleStatus.PUBLISHED);
        TemplateVersionEntity inFlight = version(2, null, TemplateLifecycleStatus.DRAFT);
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(inFlight, published));

        List<TemplateVersionEntity> ordered = resolver.listVersionLinesOrdered(templateId);

        assertThat(ordered).containsExactly(inFlight, published);
    }

    @Test
    void maxDevVersionNumber_returnsHighestNumber() {
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(
                        version(3, null, TemplateLifecycleStatus.DRAFT),
                        version(2, "1.0.0", TemplateLifecycleStatus.PUBLISHED),
                        version(1, "0.9.0", TemplateLifecycleStatus.PUBLISHED)
                ));

        assertThat(resolver.maxDevVersionNumber(templateId)).isEqualTo(3);
    }

    @ParameterizedTest(name = "STOPPED + releaseVersion={0} → isInFlight=false")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void isInFlight_stoppedWithBlankRelease_isFalse(String releaseVersion) {
        TemplateVersionEntity stopped = version(1, releaseVersion, TemplateLifecycleStatus.STOPPED);

        assertThat(resolver.isInFlight(stopped)).isFalse();
    }

    @ParameterizedTest(name = "DEPRECATED + releaseVersion={0} → isInFlight=false")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void isInFlight_deprecatedWithBlankRelease_isFalse(String releaseVersion) {
        TemplateVersionEntity deprecated = version(1, releaseVersion, TemplateLifecycleStatus.DEPRECATED);

        assertThat(resolver.isInFlight(deprecated)).isFalse();
    }

    @ParameterizedTest(name = "{0} + blank release → isInFlight=true")
    @EnumSource(
            value = TemplateLifecycleStatus.class,
            names = {"DRAFT", "TESTING", "APPROVAL", "PENDING_RELEASE"}
    )
    void isInFlight_activeAuthoringWithBlankRelease_isTrue(TemplateLifecycleStatus status) {
        TemplateVersionEntity inFlight = version(1, null, status);

        assertThat(resolver.isInFlight(inFlight)).isTrue();
    }

    @Test
    void isInFlight_publishedWithSemver_isFalse() {
        TemplateVersionEntity published = version(1, "1.0.0", TemplateLifecycleStatus.PUBLISHED);

        assertThat(resolver.isInFlight(published)).isFalse();
    }

    @Test
    void hasInFlightDevVersion_isFalseWhenOnlyAbandonedBlankReleaseExists() {
        TemplateVersionEntity abandoned = version(1, null, TemplateLifecycleStatus.STOPPED);
        TemplateVersionEntity published = version(2, "1.0.0", TemplateLifecycleStatus.PUBLISHED);
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(published, abandoned));

        assertThat(resolver.hasInFlightDevVersion(templateId)).isFalse();
        assertThat(resolver.findInFlightDevVersion(templateId)).isEmpty();
    }

    @ParameterizedTest(name = "active status {0} still blocks hasInFlight")
    @MethodSource("activeAuthoringStatuses")
    void hasInFlightDevVersion_isTrueForActiveAuthoringBlankRelease(TemplateLifecycleStatus status) {
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(
                        version(2, null, status),
                        version(1, "1.0.0", TemplateLifecycleStatus.PUBLISHED)
                ));

        assertThat(resolver.hasInFlightDevVersion(templateId)).isTrue();
    }

    @Test
    void listVersionLinesOrdered_doesNotTreatStoppedBlankAsInFlightBucket() {
        TemplateVersionEntity abandoned = version(1, null, TemplateLifecycleStatus.STOPPED);
        TemplateVersionEntity published = version(2, "1.0.0", TemplateLifecycleStatus.PUBLISHED);
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(published, abandoned));

        List<TemplateVersionEntity> ordered = resolver.listVersionLinesOrdered(templateId);

        assertThat(ordered).containsExactly(published, abandoned);
        assertThat(ordered).noneMatch(resolver::isInFlight);
    }

    @Test
    void findLatestPublishedVersion_ignoresHigherAbandonedBlankRelease() {
        TemplateVersionEntity published = version(1, "1.0.0", TemplateLifecycleStatus.PUBLISHED);
        TemplateVersionEntity abandonedHigher = version(3, null, TemplateLifecycleStatus.STOPPED);
        TemplateVersionEntity deprecatedBlank = version(2, " ", TemplateLifecycleStatus.DEPRECATED);
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(abandonedHigher, deprecatedBlank, published));

        Optional<TemplateVersionEntity> latest = resolver.findLatestPublishedVersion(templateId);

        assertThat(latest).contains(published);
        assertThat(latest.get().getReleaseVersion()).isEqualTo("1.0.0");
    }

    private static Stream<Arguments> activeAuthoringStatuses() {
        return Stream.of(
                Arguments.of(TemplateLifecycleStatus.DRAFT),
                Arguments.of(TemplateLifecycleStatus.TESTING),
                Arguments.of(TemplateLifecycleStatus.APPROVAL),
                Arguments.of(TemplateLifecycleStatus.PENDING_RELEASE)
        );
    }

    private TemplateVersionEntity version(
            int devVersionNumber,
            String releaseVersion,
            TemplateLifecycleStatus lifecycleStatus
    ) {
        TemplateVersionEntity entity = new TemplateVersionEntity(UUID.randomUUID(), templateId, "10000001");
        entity.setDevVersionNumber(devVersionNumber);
        entity.setReleaseVersion(releaseVersion);
        entity.setLifecycleStatus(lifecycleStatus);
        return entity;
    }
}
