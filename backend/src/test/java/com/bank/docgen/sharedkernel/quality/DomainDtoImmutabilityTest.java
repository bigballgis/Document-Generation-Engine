package com.bank.docgen.sharedkernel.quality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.apimgmt.service.AdGroupResolverProperties;
import com.bank.docgen.apimgmt.service.TemplateAdGroupAuthorizationCache;
import com.bank.docgen.audit.persistence.AuditSearchPage;
import com.bank.docgen.authorization.management.api.ManagementSessionView;
import com.bank.docgen.authorization.management.service.ManagementAuthService;
import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalog;
import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalogEntry;
import com.bank.docgen.authoring.structured.PasteCleaningCategory;
import com.bank.docgen.authoring.structured.PasteCleaningResult;
import com.bank.docgen.authoring.structured.PasteCleaningSummary;
import com.bank.docgen.authoring.structured.PasteCleaningSummaryItem;
import com.bank.docgen.authoring.structured.StructuredContentFidelityIssue;
import com.bank.docgen.authoring.structured.StructuredContentFidelitySeverity;
import com.bank.docgen.authoring.structured.StructuredContentValidationResult;
import com.bank.docgen.authoring.structured.TableCellDefinition;
import com.bank.docgen.authoring.structured.TableColumnDefinition;
import com.bank.docgen.authoring.structured.TableComponentRenderModel;
import com.bank.docgen.authoring.structured.TableComponentValidationResult;
import com.bank.docgen.infrastructure.config.DocgenAsyncProperties;
import com.bank.docgen.infrastructure.storage.StorageProperties;
import com.bank.docgen.rendering.PdfPageNumberStampPlan;
import com.bank.docgen.rendering.StructuredContentImageResolver;
import com.bank.docgen.sharedkernel.document.fidelity.FidelityWarningCode;
import com.bank.docgen.runtime.api.BatchResultItemView;
import com.bank.docgen.runtime.api.BatchResultView;
import com.bank.docgen.runtime.api.BatchSummaryView;
import com.bank.docgen.runtime.domain.TaskStatus;
import com.bank.docgen.runtime.api.SyncGenerateResult;
import com.bank.docgen.runtime.service.BatchExecutionService;
import com.bank.docgen.runtime.service.DocumentGenerationEngine;
import com.bank.docgen.template.service.RiskPromptConfigService;
import com.bank.docgen.template.service.TemplateExportService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DomainDtoImmutabilityTest {

    @Test
    void structuredContentValidationResultDefensivelyCopiesBlockersAndWarnings() {
        List<StructuredContentFidelityIssue> blockers = new ArrayList<>(List.of(sampleIssue("nodes[0]")));
        List<StructuredContentFidelityIssue> warnings = new ArrayList<>(List.of(sampleIssue("nodes[1]")));

        StructuredContentValidationResult result = StructuredContentValidationResult.of(blockers, warnings);

        blockers.add(sampleIssue("nodes[2]"));
        warnings.add(sampleIssue("nodes[3]"));

        assertThat(result.blockers()).hasSize(1);
        assertThat(result.warnings()).hasSize(1);
        assertThatThrownBy(() -> result.blockers().add(sampleIssue("x")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void masterStyleCatalogDefensivelyCopiesStylesMap() {
        Map<String, MasterStyleCatalogEntry> styles = new HashMap<>();
        Set<String> nodeTypes = new HashSet<>(Set.of("paragraph"));
        styles.put("body", new MasterStyleCatalogEntry("body", nodeTypes, "BODY"));

        MasterStyleCatalog catalog = new MasterStyleCatalog("1.0", styles);

        styles.put("heading", new MasterStyleCatalogEntry("heading", Set.of("heading"), "HEADING"));
        nodeTypes.add("table");

        assertThat(catalog.stylesByKey()).containsOnlyKeys("body");
        assertThat(catalog.find("body").applicableNodeTypes()).containsExactly("paragraph");
        assertThatThrownBy(() -> catalog.stylesByKey().put("x", null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void auditSearchPageDefensivelyCopiesContent() {
        List<String> content = new ArrayList<>(List.of("event-1"));

        AuditSearchPage<String> page = new AuditSearchPage<>(content, 1L, 1);

        content.add("event-2");

        assertThat(page.content()).containsExactly("event-1");
        assertThatThrownBy(() -> page.content().add("event-3"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void pdfPageNumberStampPlanDefensivelyCopiesSectionStartPages() {
        List<Integer> sectionStarts = new ArrayList<>(List.of(1, 5));

        PdfPageNumberStampPlan plan = PdfPageNumberStampPlan.sectionAndGlobal(sectionStarts);

        sectionStarts.add(10);

        assertThat(plan.sectionStartPages()).containsExactly(1, 5);
        assertThatThrownBy(() -> plan.sectionStartPages().add(99))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void resolvedImageDefensivelyCopiesBytes() {
        byte[] bytes = new byte[] {1, 2, 3};

        StructuredContentImageResolver.ResolvedImage image =
                new StructuredContentImageResolver.ResolvedImage(bytes, "demo.png");

        bytes[0] = 99;

        assertThat(image.bytes()).containsExactly((byte) 1, (byte) 2, (byte) 3);
    }

    @Test
    void syncGenerateResultDefensivelyCopiesArtifactBytesAndWarnings() {
        byte[] artifactBytes = new byte[] {10, 20};
        List<String> warnings = new ArrayList<>(List.of("WARN-1"));

        SyncGenerateResult result = new SyncGenerateResult(
                artifactBytes,
                null,
                "application/pdf",
                "doc-1",
                "1.0",
                warnings,
                "CREATED"
        );

        artifactBytes[0] = 99;
        warnings.add("WARN-2");

        assertThat(result.artifactBytes()).containsExactly((byte) 10, (byte) 20);
        assertThat(result.fidelityWarningCodes()).containsExactly("WARN-1");
    }

    @Test
    void tableComponentRenderModelDefensivelyCopiesNestedRows() {
        List<TableColumnDefinition> columns = new ArrayList<>(List.of(new TableColumnDefinition("col-a", 50)));
        List<List<TableCellDefinition>> headerRows = new ArrayList<>(List.of(
                new ArrayList<>(List.of(new TableCellDefinition("col-a", "Header", null)))
        ));

        TableComponentRenderModel model = new TableComponentRenderModel(
                "table-1",
                columns,
                headerRows,
                true,
                null,
                List.of()
        );

        columns.add(new TableColumnDefinition("col-b", 50));
        headerRows.getFirst().add(new TableCellDefinition("col-b", "Extra", null));

        assertThat(model.columns()).hasSize(1);
        assertThat(model.headerRows()).hasSize(1);
        assertThat(model.headerRows().getFirst()).hasSize(1);
    }

    @Test
    void generatedDocumentDefensivelyCopiesArtifactBytesAndWarnings() {
        byte[] artifactBytes = new byte[] {5, 6};
        List<String> warnings = new ArrayList<>(List.of("FIDELITY-1"));

        DocumentGenerationEngine.GeneratedDocument document = new DocumentGenerationEngine.GeneratedDocument(
                "doc-1",
                "storage/key",
                artifactBytes,
                "application/pdf",
                "PDF",
                warnings
        );

        artifactBytes[0] = 0;
        warnings.add("FIDELITY-2");

        assertThat(document.artifactBytes()).containsExactly((byte) 5, (byte) 6);
        assertThat(document.fidelityWarningCodes()).containsExactly("FIDELITY-1");
    }

    @Test
    void pasteCleaningSummaryDefensivelyCopiesItems() {
        List<PasteCleaningSummaryItem> items = new ArrayList<>(List.of(
                new PasteCleaningSummaryItem(
                        PasteCleaningCategory.TRANSFORMED,
                        "paste.clean.transformed",
                        "Removed inline styles"
                )
        ));

        PasteCleaningSummary summary = PasteCleaningSummary.of(items);

        items.add(new PasteCleaningSummaryItem(
                PasteCleaningCategory.REMOVED,
                "paste.clean.removed",
                "Removed script tag"
        ));

        assertThat(summary.items()).hasSize(1);
        assertThatThrownBy(() -> summary.items().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void adGroupResolverPropertiesDefensivelyCopiesAccountGroups() {
        AdGroupResolverProperties properties = new AdGroupResolverProperties();
        List<String> groups = new ArrayList<>(List.of("DOCGEN-API-RETAIL"));
        properties.setAccountGroups(new HashMap<>(Map.of("svc-1", groups)));

        Map<String, List<String>> returned = properties.getAccountGroups();
        groups.add("DOCGEN-API-CORP");

        assertThat(returned.get("svc-1")).containsExactly("DOCGEN-API-RETAIL");
        assertThatThrownBy(() -> returned.put("svc-2", List.of()))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> returned.get("svc-1").add("OTHER"))
                .isInstanceOf(UnsupportedOperationException.class);

        List<String> storedGroups = new ArrayList<>(List.of("DOCGEN-API-WHOLESALE"));
        properties.setAccountGroups(Map.of("svc-2", storedGroups));
        storedGroups.add("MUTATED");
        assertThat(properties.getAccountGroups().get("svc-2")).containsExactly("DOCGEN-API-WHOLESALE");
    }

    @Test
    void templateExportZipArtifactDefensivelyCopiesContent() {
        byte[] content = new byte[] {7, 8, 9};

        TemplateExportService.TemplateExportZipArtifact artifact =
                new TemplateExportService.TemplateExportZipArtifact("bundle.zip", content);

        content[0] = 0;

        assertThat(artifact.content()).containsExactly((byte) 7, (byte) 8, (byte) 9);
    }

    @Test
    void templateAdGroupCacheReturnsImmutableAllowedGroups() {
        TemplateAdGroupAuthorizationCache cache = new TemplateAdGroupAuthorizationCache();
        UUID templateId = UUID.randomUUID();
        List<String> groups = new ArrayList<>(List.of("GROUP-A"));

        cache.rememberAllowedGroups(templateId, groups);
        groups.add("GROUP-B");

        List<String> cached = cache.getAllowedGroups(templateId).orElseThrow();
        assertThat(cached).containsExactly("GROUP-A");
        assertThatThrownBy(() -> cached.add("GROUP-C"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void effectiveRiskPromptConfigDefensivelyCopiesCollections() throws Exception {
        List<String> categories = new ArrayList<>(List.of("CATEGORY-A"));
        Map<String, String> copy = new HashMap<>(Map.of("key-a", "prompt-a"));

        RiskPromptConfigService.EffectiveRiskPromptConfig config = new RiskPromptConfigService.EffectiveRiskPromptConfig(
                categories,
                copy,
                "2026-07-04T00:00:00Z"
        );

        categories.add("CATEGORY-B");
        copy.put("key-b", "prompt-b");

        assertThat(config.reasonCategories()).containsExactly("CATEGORY-A");
        assertThat(config.riskPromptCopy()).containsExactly(Map.entry("key-a", "prompt-a"));
        assertThatThrownBy(() -> config.reasonCategories().add("x"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> config.riskPromptCopy().put("key-c", "prompt-c"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void docgenAsyncPropertiesDefensivelyCopiesKafkaSettings() {
        DocgenAsyncProperties properties = new DocgenAsyncProperties();
        DocgenAsyncProperties.Kafka kafka = new DocgenAsyncProperties.Kafka();
        kafka.setAsyncBatchTopic("custom.async-batch.v1");
        properties.setKafka(kafka);

        DocgenAsyncProperties.Kafka returned = properties.getKafka();
        kafka.setConsumerGroup("mutated-group");

        assertThat(returned.getAsyncBatchTopic()).isEqualTo("custom.async-batch.v1");
        assertThat(returned.getConsumerGroup()).isEqualTo("docgen-async-batch-worker");
        assertThat(properties.getKafka().getAsyncBatchTopic()).isEqualTo("custom.async-batch.v1");

        DocgenAsyncProperties.Kafka external = new DocgenAsyncProperties.Kafka();
        external.setDeadLetterTopic("external.dlt");
        properties.setKafka(external);
        external.setDeadLetterTopic("mutated.dlt");
        assertThat(properties.getKafka().getDeadLetterTopic()).isEqualTo("external.dlt");
    }

    @Test
    void pasteCleaningResultDefensivelyCopiesSummaryItems() {
        List<PasteCleaningSummaryItem> items = new ArrayList<>(List.of(
                new PasteCleaningSummaryItem(
                        PasteCleaningCategory.TRANSFORMED,
                        "paste.clean.transformed",
                        "Removed inline styles"
                )
        ));
        PasteCleaningSummary summary = PasteCleaningSummary.of(items);

        PasteCleaningResult result = new PasteCleaningResult(false, "{}", summary, null);

        items.add(new PasteCleaningSummaryItem(
                PasteCleaningCategory.REMOVED,
                "paste.clean.removed",
                "Removed script tag"
        ));

        assertThat(result.summary().items()).hasSize(1);
    }

    @Test
    void tableComponentValidationResultDefensivelyCopiesNestedModels() {
        List<StructuredContentFidelityIssue> blockers = new ArrayList<>(List.of(sampleIssue("nodes[0]")));
        StructuredContentValidationResult fidelity = StructuredContentValidationResult.of(blockers, List.of());
        List<TableColumnDefinition> columns = new ArrayList<>(List.of(new TableColumnDefinition("col-a", 50)));
        TableComponentRenderModel renderModel = new TableComponentRenderModel(
                "table-1",
                columns,
                List.of(),
                true,
                null,
                List.of()
        );

        TableComponentValidationResult result = new TableComponentValidationResult(
                fidelity,
                Optional.of(renderModel)
        );

        blockers.add(sampleIssue("nodes[1]"));
        columns.add(new TableColumnDefinition("col-b", 50));

        assertThat(result.fidelity().blockers()).hasSize(1);
        assertThat(result.renderModel()).get().extracting(TableComponentRenderModel::columns).asList().hasSize(1);
    }

    @Test
    void storagePropertiesDefensivelyCopiesMinioNestedRecord() {
        StorageProperties.MinioProperties minio = new StorageProperties.MinioProperties(
                "http://minio:9000",
                "access-key",
                "secret-key"
        );

        StorageProperties properties = new StorageProperties("minio", "docgen", minio);

        assertThat(properties.minio()).isNotSameAs(minio);
        assertThat(properties.minio().endpoint()).isEqualTo("http://minio:9000");
    }

    @Test
    void loginSessionDefensivelyCopiesNestedSessionView() {
        List<String> roles = new ArrayList<>(List.of("TEMPLATE_AUTHOR"));
        ManagementSessionView session = new ManagementSessionView(
                "user01",
                "User One",
                "user01@example.com",
                "LOCAL",
                roles,
                List.of("RETAIL"),
                "/templates",
                List.of("/templates"),
                null,
                Instant.parse("2026-07-04T12:00:00Z"),
                Instant.parse("2026-07-04T20:00:00Z")
        );

        ManagementAuthService.LoginSession loginSession = new ManagementAuthService.LoginSession(
                "token",
                Instant.parse("2026-07-04T12:00:00Z"),
                Instant.parse("2026-07-04T20:00:00Z"),
                session
        );

        roles.add("TEMPLATE_APPROVER");

        assertThat(loginSession.session().roles()).containsExactly("TEMPLATE_AUTHOR");
    }

    @Test
    void batchExecutionOutcomeDefensivelyCopiesBatchResultItems() {
        List<BatchResultItemView> items = new ArrayList<>(List.of(
                new BatchResultItemView("item-1", "SUCCESS", null, null, "doc-1", List.of(), null)
        ));
        BatchResultView batchResult = new BatchResultView(
                "batch-1",
                new BatchSummaryView(1, 1, 1, 0, 0),
                items
        );

        BatchExecutionService.BatchExecutionOutcome outcome =
                new BatchExecutionService.BatchExecutionOutcome(batchResult, TaskStatus.SUCCEEDED);

        items.add(new BatchResultItemView("item-2", "FAILED", null, null, null, List.of(), null));

        assertThat(outcome.batchResult().items()).hasSize(1);
    }

    private static StructuredContentFidelityIssue sampleIssue(String location) {
        return new StructuredContentFidelityIssue(
                StructuredContentFidelitySeverity.BLOCKER,
                FidelityWarningCode.UNRESOLVED_VARIABLE,
                "structured.fidelity.undeclaredVariable",
                location,
                "Undeclared variable",
                "Declare the variable"
        );
    }
}
