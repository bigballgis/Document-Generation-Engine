package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalog;
import com.bank.docgen.sharedkernel.document.style.MasterStyleCatalogEntry;
import com.bank.docgen.authoring.structured.MasterStyleCatalogService;
import com.bank.docgen.authoring.structured.PasteCleaningCategory;
import com.bank.docgen.authoring.structured.PasteCleaningResult;
import com.bank.docgen.authoring.structured.PasteCleaningService;
import com.bank.docgen.authoring.structured.PasteCleaningSummary;
import com.bank.docgen.authoring.structured.PasteCleaningSummaryItem;
import com.bank.docgen.template.api.MasterStyleCatalogView;
import com.bank.docgen.template.api.PasteCleanRequest;
import com.bank.docgen.template.api.PasteCleanResultView;
import com.bank.docgen.template.api.PasteCleaningSummaryItemView;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TemplateStructuredAuthoringServiceTest {

    @Mock
    private MasterStyleCatalogService masterStyleCatalogService;
    @Mock
    private PasteCleaningService pasteCleaningService;

    private TemplateStructuredAuthoringService service;

    @BeforeEach
    void setUp() {
        service = new TemplateStructuredAuthoringService(masterStyleCatalogService, pasteCleaningService);
    }

    @Test
    void getMasterStyleCatalog_mapsEntriesSortedByStyleKey() {
        UUID masterId = UUID.randomUUID();
        Map<String, MasterStyleCatalogEntry> styles = new LinkedHashMap<>();
        styles.put("Heading1", new MasterStyleCatalogEntry("Heading1", Set.of("paragraph"), "heading"));
        styles.put("BodyText", new MasterStyleCatalogEntry("BodyText", Set.of("paragraph"), "body"));
        MasterStyleCatalog catalog = new MasterStyleCatalog("1.0", styles);
        when(masterStyleCatalogService.loadForMaster(masterId)).thenReturn(catalog);

        MasterStyleCatalogView view = service.getMasterStyleCatalog(masterId);

        assertThat(view.catalogVersion()).isEqualTo("1.0");
        assertThat(view.entries()).extracting(e -> e.styleKey()).containsExactly("BodyText", "Heading1");
        assertThat(view.entries().getFirst().applicableNodeTypes()).containsExactly("paragraph");
        assertThat(view.entries().getFirst().renderPurpose()).isEqualTo("body");
    }

    @Test
    void pasteClean_delegatesAndMapsResult() {
        PasteCleanRequest request = new PasteCleanRequest(
                "<p>Hello</p>",
                "{\"schemaVersion\":\"1.0\",\"nodes\":[]}"
        );
        PasteCleaningResult cleaningResult = new PasteCleaningResult(
                false,
                "{\"nodes\":[{\"type\":\"paragraph\"}]}",
                PasteCleaningSummary.of(List.of(
                        new PasteCleaningSummaryItem(
                                PasteCleaningCategory.TRANSFORMED,
                                "authoring.paste.transformed",
                                "p→paragraph"
                        )
                )),
                "{\"nodes\":[]}"
        );
        when(pasteCleaningService.cleanPaste(
                eq(request.sourceHtml()),
                eq(request.prePasteStructuredContentJson())
        )).thenReturn(cleaningResult);

        PasteCleanResultView view = service.pasteClean(request);

        verify(pasteCleaningService).cleanPaste(request.sourceHtml(), request.prePasteStructuredContentJson());
        assertThat(view.blocked()).isFalse();
        assertThat(view.cleanedStructuredContentJson()).contains("paragraph");
        assertThat(view.prePasteSnapshotJson()).isEqualTo("{\"nodes\":[]}");
        assertThat(view.summary().transformedCount()).isEqualTo(1);
        assertThat(view.summary().removedCount()).isZero();
        assertThat(view.summary().warningCount()).isZero();
        assertThat(view.summary().blockedCount()).isZero();
    }

    @Test
    void pasteClean_mapsSummaryItems() {
        PasteCleanRequest request = new PasteCleanRequest("<script>x</script>", "{\"nodes\":[]}");
        PasteCleaningResult cleaningResult = new PasteCleaningResult(
                true,
                null,
                PasteCleaningSummary.of(List.of(
                        new PasteCleaningSummaryItem(
                                PasteCleaningCategory.BLOCKED,
                                "authoring.paste.blockedScript",
                                "script tag"
                        )
                )),
                null
        );
        when(pasteCleaningService.cleanPaste(request.sourceHtml(), request.prePasteStructuredContentJson()))
                .thenReturn(cleaningResult);

        PasteCleanResultView view = service.pasteClean(request);

        assertThat(view.blocked()).isTrue();
        assertThat(view.summary().blockedCount()).isEqualTo(1);
        PasteCleaningSummaryItemView item = view.summary().items().getFirst();
        assertThat(item.category()).isEqualTo(PasteCleaningCategory.BLOCKED);
        assertThat(item.messageKey()).isEqualTo("authoring.paste.blockedScript");
        assertThat(item.detectionSummary()).isEqualTo("script tag");
    }
}
