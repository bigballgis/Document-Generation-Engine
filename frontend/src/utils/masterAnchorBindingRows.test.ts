import { describe, expect, it } from 'vitest'
import { buildMasterAnchorBindingRows } from '@/utils/masterAnchorBindingRows'
import type { AnchorBinding } from '@/types/template'
import type { MasterAnchor } from '@/types/master'

describe('buildMasterAnchorBindingRows', () => {
  const masterAnchors: MasterAnchor[] = [
    { anchorId: 'FOL_CLAUSE_01_DEFINITIONS_AND_INTERPRETATION', displayLabel: '1. Definitions and Interpretation', documentSequence: 0 },
    { anchorId: 'FOL_CLAUSE_02_THE_FACILITY', displayLabel: '2. The Facility', documentSequence: 1 },
    { anchorId: 'FOL_CLAUSE_03_PURPOSE', displayLabel: '3. Purpose', documentSequence: 2 },
  ]

  it('preserves master anchor order and marks unconfigured anchors', () => {
    const bindings: AnchorBinding[] = [
      {
        anchorId: 'FOL_CLAUSE_02_THE_FACILITY',
        declaredContentType: 'TEXT',
        structuredContentJson: '{"nodes":[]}',
        validationStatus: 'VALID',
        updatedAt: '2026-07-17T10:00:00Z',
      },
    ]

    const rows = buildMasterAnchorBindingRows(masterAnchors, bindings)

    expect(rows.map((row) => row.anchorId)).toEqual([
      'FOL_CLAUSE_01_DEFINITIONS_AND_INTERPRETATION',
      'FOL_CLAUSE_02_THE_FACILITY',
      'FOL_CLAUSE_03_PURPOSE',
    ])
    expect(rows[0]?.configured).toBe(false)
    expect(rows[1]?.configured).toBe(true)
    expect(rows[1]?.declaredContentType).toBe('TEXT')
    expect(rows[2]?.configured).toBe(false)
  })

  it('appends orphan bindings after master anchors', () => {
    const bindings: AnchorBinding[] = [
      {
        anchorId: 'ORPHAN',
        declaredContentType: 'TEXT',
        structuredContentJson: '{"nodes":[]}',
        updatedAt: '2026-07-17T10:00:00Z',
      },
    ]

    const rows = buildMasterAnchorBindingRows(masterAnchors, bindings)

    expect(rows.at(-1)?.anchorId).toBe('ORPHAN')
  })
})
