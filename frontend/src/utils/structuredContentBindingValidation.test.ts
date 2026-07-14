import { describe, expect, it } from 'vitest'

import {
  blockPathToLocation,
  parseBlockLocation,
  validateStructuredContentDocument,
} from '@/utils/structuredContentBindingValidation'
import type { StructuredContentDocument } from '@/utils/structuredContentNodes'

describe('structuredContentBindingValidation', () => {
  const doc: StructuredContentDocument = {
    schemaVersion: '1.0',
    nodes: [
      {
        type: 'paragraph',
        children: [{ type: 'variable', key: 'missingKey' }],
      },
      {
        type: 'conditionBlock',
        conditionExpression: '',
        children: [],
      },
    ],
  }

  it('maps block paths to backend-style locations', () => {
    expect(blockPathToLocation([0])).toBe('nodes[0]')
    expect(blockPathToLocation([0, 1])).toBe('nodes[0].children[1]')
  })

  it('parses locations back to block paths', () => {
    expect(parseBlockLocation('nodes[0]')).toEqual([0])
    expect(parseBlockLocation('nodes[0].children[1]')).toEqual([0, 1])
    expect(parseBlockLocation('document')).toBeNull()
  })

  it('reports unresolved variables and empty condition expressions', () => {
    const issues = validateStructuredContentDocument(doc, new Set(['customerName']))
    expect(issues.length).toBeGreaterThanOrEqual(2)
    expect(issues.some((issue) => issue.blockPath.join('.') === '0' && issue.code === 'UNRESOLVED_VARIABLE')).toBe(
      true,
    )
    expect(
      issues.some(
        (issue) => issue.blockPath.join('.') === '1' && issue.code === 'INVALID_CONDITION_EXPRESSION',
      ),
    ).toBe(true)
  })

  it('returns no issues when variables and conditions are valid', () => {
    const valid: StructuredContentDocument = {
      schemaVersion: '1.0',
      nodes: [
        {
          type: 'paragraph',
          children: [{ type: 'variable', key: 'customerName' }],
        },
        {
          type: 'conditionBlock',
          conditionExpression: '${customerName} == true',
          children: [],
        },
      ],
    }
    expect(validateStructuredContentDocument(valid, new Set(['customerName']))).toEqual([])
  })

  it('rejects invalid location strings', () => {
    expect(parseBlockLocation('')).toBeNull()
    expect(parseBlockLocation('nodes[abc]')).toBeNull()
  })
})
