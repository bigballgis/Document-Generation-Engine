import { describe, expect, it } from 'vitest'
import {
  buildAcceptedPasteCleaningEvidence,
  buildBindingUpsertWithPasteEvidence,
  hasUnresolvedPasteBlockers,
  toPasteCleaningEvidence,
} from '@/utils/pasteCleaningEvidence'
import type { PasteCleaningSummary } from '@/types/template'

const cleanSummary: PasteCleaningSummary = {
  items: [
    {
      category: 'TRANSFORMED',
      messageKey: 'paste.summary.transformed',
      detectionSummary: 'Transformed paragraph element into controlled structured node.',
    },
  ],
  transformedCount: 1,
  removedCount: 0,
  warningCount: 0,
  blockedCount: 0,
}

describe('pasteCleaningEvidence', () => {
  it('maps paste-clean summary to non-sensitive binding evidence', () => {
    const evidence = toPasteCleaningEvidence(cleanSummary)

    expect(evidence).toEqual({
      transformedCount: 1,
      removedCount: 0,
      warningCount: 0,
      blockedCount: 0,
      unresolvedPasteBlockers: false,
      items: [
        {
          category: 'TRANSFORMED',
          messageKey: 'paste.summary.transformed',
          detectionSummary: 'Transformed paragraph element into controlled structured node.',
        },
      ],
    })
    expect(JSON.stringify(evidence)).not.toMatch(/<object|<script|sourceHtml/i)
  })

  it('builds accepted evidence with blockedCount=0 and no BLOCKED items', () => {
    const evidence = buildAcceptedPasteCleaningEvidence({
      ...cleanSummary,
      removedCount: 1,
      items: [
        ...cleanSummary.items,
        {
          category: 'REMOVED',
          messageKey: 'paste.summary.removed',
          detectionSummary: 'Removed empty span.',
        },
      ],
    })

    expect(evidence.blockedCount).toBe(0)
    expect(evidence.unresolvedPasteBlockers).toBe(false)
    expect(evidence.removedCount).toBe(1)
    expect((evidence.items ?? []).every((item) => item.category !== 'BLOCKED')).toBe(true)
    expect(JSON.stringify(evidence)).not.toContain('sourceHtml')
  })

  it('strips HTML tags from detectionSummary so residue never stores markup', () => {
    const evidence = toPasteCleaningEvidence({
      ...cleanSummary,
      items: [
        {
          category: 'REMOVED',
          messageKey: 'paste.summary.removed',
          detectionSummary: 'Removed <object data="x">snippet</object> from paste.',
        },
      ],
    })

    expect(evidence.items?.[0]?.detectionSummary).toBe('Removed snippet from paste.')
    expect(evidence.items?.[0]?.detectionSummary).not.toContain('<object')
  })

  it('detects unresolved paste blockers from counts, flag, or BLOCKED items', () => {
    expect(hasUnresolvedPasteBlockers(undefined)).toBe(false)
    expect(hasUnresolvedPasteBlockers(toPasteCleaningEvidence(cleanSummary))).toBe(false)
    expect(
      hasUnresolvedPasteBlockers({
        transformedCount: 0,
        removedCount: 0,
        warningCount: 0,
        blockedCount: 1,
        unresolvedPasteBlockers: false,
        items: [],
      }),
    ).toBe(true)
    expect(
      hasUnresolvedPasteBlockers({
        transformedCount: 0,
        removedCount: 0,
        warningCount: 0,
        blockedCount: 0,
        unresolvedPasteBlockers: true,
        items: [],
      }),
    ).toBe(true)
    expect(
      hasUnresolvedPasteBlockers({
        transformedCount: 0,
        removedCount: 0,
        warningCount: 0,
        blockedCount: 0,
        unresolvedPasteBlockers: false,
        items: [
          {
            category: 'BLOCKED',
            messageKey: 'paste.summary.blocked',
            detectionSummary: 'Absolute positioning',
          },
        ],
      }),
    ).toBe(true)
  })

  it('attaches accepted evidence to binding upsert payload without source HTML', () => {
    const evidence = buildAcceptedPasteCleaningEvidence(cleanSummary)
    const payload = buildBindingUpsertWithPasteEvidence(
      {
        anchorId: 'body',
        declaredContentType: 'RICH_TEXT',
        structuredContentJson: '{"schemaVersion":"1.0","nodes":[]}',
      },
      { pendingPasteEvidence: evidence },
    )

    expect(payload.pasteCleaningEvidence).toEqual(evidence)
    expect(payload.clearPasteCleaningEvidence).toBeUndefined()
    expect(JSON.stringify(payload)).not.toMatch(/sourceHtml|<p>/i)
  })

  it('sets clearPasteCleaningEvidence for clean rewrite clear path', () => {
    const payload = buildBindingUpsertWithPasteEvidence(
      {
        anchorId: 'body',
        declaredContentType: 'TEXT',
        structuredContentJson: '{"schemaVersion":"1.0","nodes":[]}',
      },
      { clearPasteCleaningEvidence: true },
    )

    expect(payload.clearPasteCleaningEvidence).toBe(true)
    expect(payload.pasteCleaningEvidence).toBeUndefined()
  })
})
