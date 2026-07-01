import { describe, expect, it } from 'vitest'
import {
  normalizeLegacyStructuredContent,
  normalizeStructuredContentJson,
} from '@/utils/structuredContentCompat'

describe('structuredContentCompat', () => {
  it('passes through v1 nodes documents unchanged', () => {
    const doc = normalizeLegacyStructuredContent({
      schemaVersion: '1.0',
      nodes: [{ type: 'paragraph', children: [{ type: 'textRun', value: 'Hello' }] }],
    })
    expect(doc.nodes).toHaveLength(1)
    expect(doc.nodes[0]?.children?.[0]?.value).toBe('Hello')
  })

  it('converts legacy blocks array to nodes', () => {
    const doc = normalizeLegacyStructuredContent({
      blocks: [{ type: 'paragraph', text: 'Existing clause' }],
    })
    expect(doc.schemaVersion).toBe('1.0')
    expect(doc.nodes[0]?.type).toBe('paragraph')
    expect(doc.nodes[0]?.children?.[0]?.value).toBe('Existing clause')
  })

  it('normalizes empty legacy blocks to empty nodes', () => {
    const doc = normalizeStructuredContentJson('{"blocks":[]}')
    expect(doc).toEqual({ schemaVersion: '1.0', nodes: [] })
  })
})
