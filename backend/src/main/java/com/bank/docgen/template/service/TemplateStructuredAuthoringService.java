package com.bank.docgen.template.service;

import com.bank.docgen.authoring.structured.MasterStyleCatalog;
import com.bank.docgen.authoring.structured.MasterStyleCatalogService;
import com.bank.docgen.authoring.structured.PasteCleaningResult;
import com.bank.docgen.authoring.structured.PasteCleaningService;
import com.bank.docgen.authoring.structured.PasteCleaningSummary;
import com.bank.docgen.authoring.structured.PasteCleaningSummaryItem;
import com.bank.docgen.template.api.MasterStyleCatalogEntryView;
import com.bank.docgen.template.api.MasterStyleCatalogView;
import com.bank.docgen.template.api.PasteCleanRequest;
import com.bank.docgen.template.api.PasteCleanResultView;
import com.bank.docgen.template.api.PasteCleaningSummaryItemView;
import com.bank.docgen.template.api.PasteCleaningSummaryView;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TemplateStructuredAuthoringService {

    private final MasterStyleCatalogService masterStyleCatalogService;
    private final PasteCleaningService pasteCleaningService;

    public TemplateStructuredAuthoringService(
            MasterStyleCatalogService masterStyleCatalogService,
            PasteCleaningService pasteCleaningService
    ) {
        this.masterStyleCatalogService = masterStyleCatalogService;
        this.pasteCleaningService = pasteCleaningService;
    }

    public MasterStyleCatalogView getMasterStyleCatalog(UUID masterId) {
        return toCatalogView(masterStyleCatalogService.loadForMaster(masterId));
    }

    public PasteCleanResultView pasteClean(PasteCleanRequest request) {
        PasteCleaningResult result = pasteCleaningService.cleanPaste(
                request.sourceHtml(),
                request.prePasteStructuredContentJson()
        );
        return toPasteCleanView(result);
    }

    private MasterStyleCatalogView toCatalogView(MasterStyleCatalog catalog) {
        List<MasterStyleCatalogEntryView> entries = catalog.stylesByKey().values().stream()
                .map(entry -> new MasterStyleCatalogEntryView(
                        entry.styleKey(),
                        entry.applicableNodeTypes(),
                        entry.renderPurpose()
                ))
                .sorted(Comparator.comparing(MasterStyleCatalogEntryView::styleKey))
                .toList();
        return new MasterStyleCatalogView(catalog.catalogVersion(), entries);
    }

    private PasteCleanResultView toPasteCleanView(PasteCleaningResult result) {
        PasteCleaningSummary summary = result.summary();
        List<PasteCleaningSummaryItemView> items = summary.items().stream()
                .map(this::toPasteSummaryItemView)
                .toList();
        PasteCleaningSummaryView summaryView = new PasteCleaningSummaryView(
                items,
                summary.transformedCount(),
                summary.removedCount(),
                summary.warningCount(),
                summary.blockedCount()
        );
        return new PasteCleanResultView(
                result.blocked(),
                result.cleanedStructuredContentJson(),
                summaryView,
                result.prePasteSnapshotJson()
        );
    }

    private PasteCleaningSummaryItemView toPasteSummaryItemView(PasteCleaningSummaryItem item) {
        return new PasteCleaningSummaryItemView(
                item.category(),
                item.messageKey(),
                item.detectionSummary()
        );
    }
}
