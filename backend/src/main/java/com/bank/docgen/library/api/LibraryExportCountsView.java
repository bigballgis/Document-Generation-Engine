package com.bank.docgen.library.api;

public record LibraryExportCountsView(
        int includedCount,
        int skippedCount,
        int failedCount,
        int omittedUnauthorizedOrUnknownCount,
        int uniqueMasterCount,
        int uniqueClauseCount,
        int uniqueAssetKeyCount
) {
}
