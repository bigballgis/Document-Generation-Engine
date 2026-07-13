/** Paste-cleaning residue and master style catalog types for template authoring. */

type PasteCleaningCategory = 'TRANSFORMED' | 'REMOVED' | 'WARNING' | 'BLOCKED'

/** Not yet modeled in `openapi-v1.yaml` (management paste clean). */
interface PasteCleaningSummaryItem {
  category: PasteCleaningCategory
  messageKey: string
  detectionSummary: string
}

/** Not yet modeled in `openapi-v1.yaml` (management paste clean). */
export interface PasteCleaningSummary {
  items: PasteCleaningSummaryItem[]
  transformedCount: number
  removedCount: number
  warningCount: number
  blockedCount: number
}

/** Non-sensitive paste-cleaning residue on a binding (ops-paste-binding-seam / ADR-0019). */
export interface PasteCleaningEvidenceItem {
  category: PasteCleaningCategory
  messageKey: string
  detectionSummary?: string | null
}

/**
 * Non-sensitive paste-cleaning residue persisted after Accept.
 * Forbidden: source HTML. Counts are required on Accept upsert payloads;
 * OpenAPI response fields may omit unset counters.
 */
export interface PasteCleaningEvidence {
  transformedCount?: number
  removedCount?: number
  warningCount?: number
  blockedCount?: number
  unresolvedPasteBlockers?: boolean | null
  items?: PasteCleaningEvidenceItem[]
}

/** Not yet modeled in `openapi-v1.yaml` (management paste clean). */
export interface PasteCleanResult {
  blocked: boolean
  cleanedStructuredContentJson: string | null
  summary: PasteCleaningSummary
  prePasteSnapshotJson: string
}

/** Not yet modeled in `openapi-v1.yaml` (management master style catalog). */
interface MasterStyleCatalogEntry {
  styleKey: string
  applicableNodeTypes: string[]
  renderPurpose: string
}

/** Not yet modeled in `openapi-v1.yaml` (management master style catalog). */
export interface MasterStyleCatalog {
  catalogVersion: string
  entries: MasterStyleCatalogEntry[]
}
