import { describe, expect, it } from 'vitest'
import {
  sortMasterAnchorsByDocumentSequence,
  toMasterAnchorDocumentPosition,
} from '@/utils/masterAnchorDocumentOrder'
import type { MasterAnchor } from '@/types/master'

describe('masterAnchorDocumentOrder', () => {
  const anchors: MasterAnchor[] = [
    { anchorId: 'FOOTER', displayLabel: 'Footer', documentSequence: 2 },
    { anchorId: 'HEADER', displayLabel: 'Header', documentSequence: 0 },
    { anchorId: 'BODY', displayLabel: 'Body', documentSequence: 1 },
  ]

  it('BDD-CE-U06-MAC-001 — sorts by documentSequence ascending', () => {
    const ordered = sortMasterAnchorsByDocumentSequence(anchors)
    expect(ordered.map((a) => a.anchorId)).toEqual(['HEADER', 'BODY', 'FOOTER'])
  })

  it('maps 0-based documentSequence to 1-based UI position', () => {
    expect(toMasterAnchorDocumentPosition(0)).toBe(1)
    expect(toMasterAnchorDocumentPosition(2)).toBe(3)
  })

  it('does not mutate the input array', () => {
    const original = [...anchors]
    sortMasterAnchorsByDocumentSequence(anchors)
    expect(anchors).toEqual(original)
  })
})
