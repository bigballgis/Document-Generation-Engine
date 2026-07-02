# Shared FOL catalog helpers — single source of truth from Get-LmaSectionCatalog.
# Dot-sourced by generate-fol-catalog.ps1 and import-fol-demo.ps1.

$Script:FolCatalogMarker = 'fol-exec-demo-v6-hybrid-anchor-ids'
$Script:FolOverlayOnlyAnchors = @('FOL_HEADER', 'FOL_FACILITY_SUMMARY', 'FOL_SIG_BORROWER', 'FOL_SIG_LENDER')
$Script:FolExpectedAnchorCount = 40

function Get-FolCatalogMarker { return $Script:FolCatalogMarker }
function Get-FolOverlayOnlyAnchors { return ,@($Script:FolOverlayOnlyAnchors) }
function Get-FolExpectedAnchorCount { return $Script:FolExpectedAnchorCount }

function ConvertTo-FolAnchorSlug([string]$Text) {
    $slug = $Text.ToUpperInvariant()
    $slug = $slug -replace '[^A-Z0-9]+', '_'
    $slug = $slug -replace '_+', '_'
    return $slug.Trim('_')
}

function Resolve-FolHybridAnchorId {
    param([hashtable]$Entry)

    if ($Entry.Name -match '^(\d+)\.\s*(.+)$') {
        $num = '{0:D2}' -f [int]$Matches[1]
        $slug = ConvertTo-FolAnchorSlug $Matches[2]
        return "FOL_CLAUSE_${num}_$slug"
    }
    if ($Entry.Name -match '^Schedule\s+(\d+)\s*-\s*(.+)$') {
        $num = '{0:D2}' -f [int]$Matches[1]
        $slug = ConvertTo-FolAnchorSlug $Matches[2]
        return "FOL_SCHEDULE_${num}_$slug"
    }
    throw "Cannot resolve hybrid anchor id for catalog entry: $($Entry.Name)"
}

function Get-FolAnchorIdForSectionNumber([int]$Number) {
    $entry = Get-LmaSectionCatalog | Where-Object { $_.Name -match "^$Number\.\s" } | Select-Object -First 1
    if (-not $entry) { throw "No FOL section catalog entry for clause $Number" }
    return Resolve-FolHybridAnchorId $entry
}

function Get-FolAnchorIdForScheduleNumber([int]$Number) {
    $entry = Get-LmaSectionCatalog | Where-Object { $_.Name -match "^Schedule\s+$Number\s*-\s" } | Select-Object -First 1
    if (-not $entry) { throw "No FOL schedule catalog entry for schedule $Number" }
    return Resolve-FolHybridAnchorId $entry
}

function Get-FolSectionNumber([hashtable]$Entry) {
    if ($Entry.Name -match '^(\d+)\.\s') { return [int]$Matches[1] }
    return 0
}

function Get-FolScheduleNumber([hashtable]$Entry) {
    if ($Entry.Name -match '^Schedule\s+(\d+)\s*-\s*') { return [int]$Matches[1] }
    return 0
}

function Get-FolOrderedMasterAnchorIds {
    return ,@((Get-FolMasterSectionManifest | ForEach-Object { $_.anchorId }))
}

function Get-FolMasterSectionManifest {
    $sections = [System.Collections.Generic.List[object]]::new()
    $sections.Add([ordered]@{ anchorId = 'FOL_HEADER'; title = 'Letter Header' })
    $sections.Add([ordered]@{ anchorId = 'FOL_FACILITY_SUMMARY'; title = 'Schedule — Facility Particulars (Summary)' })
    foreach ($entry in Get-LmaSectionCatalog) {
        $sections.Add([ordered]@{
            anchorId = Resolve-FolHybridAnchorId $entry
            title = $entry.Name
        })
    }
    $sections.Add([ordered]@{ anchorId = 'FOL_SIG_BORROWER'; title = 'Execution — Borrower' })
    $sections.Add([ordered]@{ anchorId = 'FOL_SIG_LENDER'; title = 'Execution — Lenders / Agent' })
    return ,@($sections)
}

function Build-ClauseBindingsFromCatalog {
    $bindings = [System.Collections.Generic.List[object]]::new()
    foreach ($entry in Get-LmaSectionCatalog) {
        $anchorId = Resolve-FolHybridAnchorId $entry
        $bindings.Add([ordered]@{
            anchorId = $anchorId
            referenceKey = $anchorId
            moduleCode = $entry.Code
            title = $entry.Name
        })
    }
    return ,@($bindings)
}

function Build-ClauseCodesFromCatalog {
    return ,@(Get-LmaSectionCatalog | ForEach-Object { $_.Code })
}

function Build-CatalogManifest {
    param(
        [array]$CompositionRuleTargets
    )
    $clauseBindings = Build-ClauseBindingsFromCatalog
    return [ordered]@{
        catalogMarker = (Get-FolCatalogMarker)
        clauseBindings = $clauseBindings
        clauseCodes = (Build-ClauseCodesFromCatalog)
        expectedAnchorCount = (Get-FolExpectedAnchorCount)
        overlayOnlyAnchors = (Get-FolOverlayOnlyAnchors)
        masterAnchorIds = (Get-FolOrderedMasterAnchorIds)
        compositionRuleTargets = $CompositionRuleTargets
        generatedAt = (Get-Date -Format 'o')
    }
}
