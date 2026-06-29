import { describe, expect, it } from 'vitest'
import {
  CONFIRMED_V1_NODE_TYPES,
  DISABLED_TOOLBAR_CAPABILITIES,
  insertBlockNode,
  isConfirmedNodeType,
  parseStructuredContent,
  serializeStructuredContent,
} from '@/utils/structuredContentNodes'

describe('structuredContentNodes', () => {
  it('only confirmed v1 node types are insertable', () => {
    const unsupported = ['div', 'script', 'table', 'html', 'customBlock']
    for (const type of unsupported) {
      expect(isConfirmedNodeType(type)).toBe(false)
    }
    for (const type of CONFIRMED_V1_NODE_TYPES) {
      expect(isConfirmedNodeType(type)).toBe(true)
    }
  })

  it('insertBlockNode adds only confirmed block nodes', () => {
    const base = parseStructuredContent('{"schemaVersion":"1.0","nodes":[]}')
    const updated = insertBlockNode(base, 'paragraph')
    expect(updated.nodes).toHaveLength(1)
    expect(updated.nodes[0]?.type).toBe('paragraph')

    const unchanged = insertBlockNode(base, 'variable')
    expect(unchanged.nodes).toHaveLength(0)
  })

  it('disabled toolbar capabilities are not confirmed node types', () => {
    for (const capability of DISABLED_TOOLBAR_CAPABILITIES) {
      expect(isConfirmedNodeType(capability.id)).toBe(false)
    }
  })

  it('round-trips structured content JSON', () => {
    const doc = parseStructuredContent(
      '{"schemaVersion":"1.0","nodes":[{"type":"paragraph","children":[{"type":"textRun","value":"Hello"}]}]}',
    )
    const json = serializeStructuredContent(doc)
    expect(JSON.parse(json).nodes[0].children[0].value).toBe('Hello')
  })
})
