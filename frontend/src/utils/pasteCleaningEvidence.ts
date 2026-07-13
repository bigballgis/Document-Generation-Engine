import type {
  PasteCleaningEvidence,
  PasteCleaningEvidenceItem,
  PasteCleaningSummary,
  UpsertBindingPayload,
} from '@/types/template'

/**
 * Maps a paste-clean summary into non-sensitive binding residue for upsert.
 * Never carries source HTML or pasted plaintext.
 */
export function toPasteCleaningEvidence(summary: PasteCleaningSummary): PasteCleaningEvidence {
  const items: PasteCleaningEvidenceItem[] = summary.items.map((item) => ({
    category: item.category,
    messageKey: item.messageKey,
    detectionSummary: sanitizeDetectionSummary(item.detectionSummary),
  }))

  const blockedFromItems = items.filter((item) => item.category === 'BLOCKED').length
  const blockedCount = Math.max(summary.blockedCount, blockedFromItems)
  const unresolvedPasteBlockers =
    blockedCount > 0 || items.some((item) => item.category === 'BLOCKED')

  return {
    transformedCount: summary.transformedCount,
    removedCount: summary.removedCount,
    warningCount: summary.warningCount,
    blockedCount,
    unresolvedPasteBlockers,
    items,
  }
}

/**
 * Evidence for a successful Accept path.
 * Accept is only enabled when paste is not blocked; persist summary-derived
 * residue with blockedCount=0 (never source HTML).
 */
export function buildAcceptedPasteCleaningEvidence(
  summary: PasteCleaningSummary,
): PasteCleaningEvidence {
  const evidence = toPasteCleaningEvidence(summary)
  return {
    ...evidence,
    blockedCount: 0,
    unresolvedPasteBlockers: false,
    // Accept UI is disabled when any BLOCKED item exists; drop them for defense-in-depth.
    items: (evidence.items ?? []).filter((item) => item.category !== 'BLOCKED'),
  }
}

export function hasUnresolvedPasteBlockers(
  evidence: PasteCleaningEvidence | null | undefined,
): boolean {
  if (!evidence) {
    return false
  }
  if (evidence.unresolvedPasteBlockers === true || (evidence.blockedCount ?? 0) > 0) {
    return true
  }
  return (evidence.items ?? []).some((item) => item.category === 'BLOCKED')
}

/**
 * Builds the PUT bindings payload, attaching Accept residue or an explicit clear flag.
 * Never includes source HTML.
 */
export function buildBindingUpsertWithPasteEvidence(
  form: Pick<UpsertBindingPayload, 'anchorId' | 'declaredContentType' | 'structuredContentJson'>,
  options: {
    pendingPasteEvidence?: PasteCleaningEvidence | null
    clearPasteCleaningEvidence?: boolean
  } = {},
): UpsertBindingPayload {
  const payload: UpsertBindingPayload = {
    anchorId: form.anchorId,
    declaredContentType: form.declaredContentType,
    structuredContentJson: form.structuredContentJson,
  }

  if (options.pendingPasteEvidence) {
    payload.pasteCleaningEvidence = options.pendingPasteEvidence
  } else if (options.clearPasteCleaningEvidence) {
    payload.clearPasteCleaningEvidence = true
  }

  return payload
}

function sanitizeDetectionSummary(value: string | null | undefined): string | null {
  if (value == null || value.trim() === '') {
    return null
  }
  return value.replace(/<[^>]+>/gi, '').trim() || null
}
