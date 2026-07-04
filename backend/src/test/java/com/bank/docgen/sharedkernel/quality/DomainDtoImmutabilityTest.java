package com.bank.docgen.sharedkernel.quality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.apimgmt.service.AdGroupResolverProperties;
import com.bank.docgen.audit.persistence.AuditSearchPage;
import com.bank.docgen.authoring.structured.MasterStyleCatalog;
import com.bank.docgen.authoring.structured.MasterStyleCatalogEntry;
import com.bank.docgen.authoring.structured.PasteCleaningCategory;
import com.bank.docgen.authoring.structured.PasteCleaningSummary;
import com.bank.docgen.authoring.structured.PasteCleaningSummaryItem;
import com.bank.docgen.authoring.structured.StructuredContentFidelityIssue;
import com.bank.docgen.authoring.structured.StructuredContentFidelitySeverity;
import com.bank.docgen.authoring.structured.StructuredContentValidationResult;
import com.bank.docgen.authoring.structured.TableCellDefinition;
import com.bank.docgen.authoring.structured.TableColumnDefinition;
import com.bank.docgen.authoring.structured.TableComponentRenderModel;
import com.bank.docgen.rendering.PdfPageNumberStampPlan;
import com.bank.docgen.rendering.StructuredContentImageResolver;
import com.bank.docgen.rendering.domain.FidelityWarningCode;
import com.bank.docgen.runtime.api.SyncGenerateResult;
import com.bank.docgen.runtime.service.DocumentGenerationEngine;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
