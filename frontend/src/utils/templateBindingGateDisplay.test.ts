import { describe, expect, it } from 'vitest'
import {
  listInvalidBindings,
  mapBindingGateIssueItems,
  resolvePublishGateLoadErrorKey,
  shouldShowBindingGatePanel,
} from '@/utils/templateBindingGateDisplay'
import type { BindingValidationResult } from '@/types/template'

describe('templateBindingGateDisplay', () => {
  it('maps binding summary counts to issue items', () => {
    expect(
      mapBindingGateIssueItems({
        blocking: true,
        totalBindings: 3,
        validCount: 0,
        missingAnchorCount: 1,
        duplicateBindingCount: 2,
        incompatibleContentTypeCount: 0,
      }),
    ).toEqual([
      { issueKey: 'missingAnchor', count: 1 },
      { issueKey: 'duplicateBinding', count: 2 },
    ])
  })

  it('lists bindings with non-valid validation status', () => {
    expect(
      listInvalidBindings([
        { anchorId: 'A1', declaredContentType: 'TEXT', structuredContentJson: null, validationStatus: 'VALID' },
        {
          anchorId: 'A2',
          declaredContentType: 'TEXT',
          structuredContentJson: null,
          validationStatus: 'MISSING_ANCHOR',
        },
      ]),
    ).toHaveLength(1)
  })

  it('falls back to default publish gate load error key', () => {
    expect(resolvePublishGateLoadErrorKey(null)).toBe('templates.error.loadPublishGate')
    expect(resolvePublishGateLoadErrorKey('api.error.template.publishGateBlocked')).toBe(
      'api.error.template.publishGateBlocked',
    )
  })

  it('shows binding gate panel when result is present', () => {
    const result: BindingValidationResult = {
      bindings: [],
      summary: {
        blocking: false,
        totalBindings: 0,
        validCount: 0,
        missingAnchorCount: 0,
        duplicateBindingCount: 0,
        incompatibleContentTypeCount: 0,
      },
    }
    expect(shouldShowBindingGatePanel(result)).toBe(true)
    expect(shouldShowBindingGatePanel(null)).toBe(false)
  })
})
