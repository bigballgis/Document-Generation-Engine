import { describe, expect, it } from 'vitest'
import {
  resolvePublishGateGoFixQuery,
  resolvePublishGateGoFixTarget,
} from '@/utils/publishGateGoFixLink'

describe('publishGateGoFixLink (BDD-CE-U15-LSS)', () => {
  it('LSS-004: ANCHOR_INTEGRITY maps to design/bindings', () => {
    expect(resolvePublishGateGoFixQuery('ANCHOR_INTEGRITY')).toEqual({
      workspaceTab: 'design',
      designTab: 'bindings',
    })
  })

  it('LSS-005: ready items and unknown codes have no Go fix target', () => {
    expect(resolvePublishGateGoFixTarget('VARIABLE_SCHEMA', true)).toBeNull()
    expect(resolvePublishGateGoFixTarget('X_UNKNOWN', false)).toBeNull()
    expect(resolvePublishGateGoFixQuery('X_UNKNOWN')).toBeNull()
  })

  it('LSS-006: samples for coverage / fidelity / content module expired', () => {
    expect(resolvePublishGateGoFixTarget('COVERAGE_THRESHOLDS', false)).toEqual({
      workspaceTab: 'testing',
      testingTab: 'coverage',
    })
    expect(resolvePublishGateGoFixTarget('FIDELITY_WARNINGS_VIEWED', false)).toEqual({
      workspaceTab: 'testing',
      testingTab: 'previewRuns',
    })
    expect(resolvePublishGateGoFixTarget('CONTENT_MODULE_EFFECTIVE_EXPIRED', false)).toEqual({
      workspaceTab: 'design',
      designTab: 'contentModules',
    })
  })

  it('maps remaining U15-D7 codes without inventing routes', () => {
    expect(resolvePublishGateGoFixQuery('VARIABLE_SCHEMA')).toEqual({
      workspaceTab: 'design',
      designTab: 'variables',
    })
    expect(resolvePublishGateGoFixQuery('RULE_BOUNDS')).toEqual({
      workspaceTab: 'design',
      designTab: 'bindings',
    })
    expect(resolvePublishGateGoFixQuery('TEST_RESULTS')).toEqual({
      workspaceTab: 'testing',
      testingTab: 'previewRuns',
    })
    expect(resolvePublishGateGoFixQuery('CHANGE_DIFF')).toEqual({
      workspaceTab: 'testing',
      testingTab: 'changeDiff',
    })
    expect(resolvePublishGateGoFixQuery('APPROVAL_SUMMARY')).toEqual({
      workspaceTab: 'approval',
      approvalTab: 'submitApproval',
    })
    expect(resolvePublishGateGoFixQuery('API_POLICY')).toEqual({
      workspaceTab: 'approval',
      approvalTab: 'publishReadiness',
    })
  })

  it('still shows Go fix for informational pending items (U15-D6)', () => {
    expect(resolvePublishGateGoFixTarget('API_POLICY', false)).not.toBeNull()
  })
})
