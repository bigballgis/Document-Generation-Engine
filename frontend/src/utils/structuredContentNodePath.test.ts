import { describe, expect, it } from 'vitest'

import {
  STRUCTURED_CONTENT_MAX_NEST_DEPTH,
  appendChildBlockAtPath,
  canAddNestedBlockChildren,
  getNodeAtPath,
  removeNodeAtPath,
  updateNodeAtPath,
} from '@/utils/structuredContentNodePath'
import type { StructuredContentDocument } from '@/utils/structuredContentNodes'

const nestedDoc: StructuredContentDocument = {
  schemaVersion: '1.0',
  nodes: [
    {
      type: 'conditionBlock',
      conditionExpression: 'x',
      children: [
        {
          type: 'loopBlock',
          loopVariable: 'items',
          children: [
            {
              type: 'paragraph',
              children: [{ type: 'textRun', value: 'deep' }],
            },
          ],
        },
      ],
    },
  ],
}

describe('structuredContentNodePath', () => {
  it('reads nodes at nested paths', () => {
    expect(getNodeAtPath(nestedDoc, [0])?.type).toBe('conditionBlock')
    expect(getNodeAtPath(nestedDoc, [0, 0])?.type).toBe('loopBlock')
    expect(getNodeAtPath(nestedDoc, [0, 0, 0])?.children?.[0]?.value).toBe('deep')
  })

  it('enforces max nest depth of 3 layers', () => {
    expect(STRUCTURED_CONTENT_MAX_NEST_DEPTH).toBe(3)
    expect(canAddNestedBlockChildren([])).toBe(true)
    expect(canAddNestedBlockChildren([0])).toBe(true)
    expect(canAddNestedBlockChildren([0, 0])).toBe(false)
  })

  it('updates nested block fields by path', () => {
    const updated = updateNodeAtPath(nestedDoc, [0, 0, 0], (node) => ({
      ...node,
      children: [{ type: 'textRun', value: 'edited' }],
    }))
    expect(getNodeAtPath(updated, [0, 0, 0])?.children?.[0]?.value).toBe('edited')
  })

  it('removes nested blocks by path', () => {
    const updated = removeNodeAtPath(nestedDoc, [0, 0])
    expect(getNodeAtPath(updated, [0])?.children).toEqual([])
  })

  it('appends child blocks under a container path', () => {
    const updated = appendChildBlockAtPath(nestedDoc, [0], {
      type: 'paragraph',
      children: [{ type: 'textRun', value: 'new' }],
    })
    expect(getNodeAtPath(updated, [0])?.children).toHaveLength(2)
    expect(getNodeAtPath(updated, [0, 1])?.type).toBe('paragraph')
  })
})
