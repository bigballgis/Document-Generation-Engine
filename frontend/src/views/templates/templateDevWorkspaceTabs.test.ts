import { describe, expect, it } from 'vitest'
import {
  buildDevWorkspaceQuery,
  resolveDesignSubTabFromQuery,
  resolveApprovalSubTabFromQuery,
  resolveTestingSubTabFromQuery,
  resolveTemplateDevWorkspaceTab,
  resolveTemplateDevWorkspaceTabFromQuery,
} from '@/views/templates/templateDevWorkspaceTabs'

describe('templateDevWorkspaceTabs', () => {
  it('resolves known workspace tabs', () => {
    expect(resolveTemplateDevWorkspaceTab('design')).toBe('design')
    expect(resolveTemplateDevWorkspaceTab('testing')).toBe('testing')
    expect(resolveTemplateDevWorkspaceTab('approval')).toBe('approval')
    expect(resolveTemplateDevWorkspaceTab('unknown')).toBe('design')
  })

  it('migrates legacy authoring testPreview to testing workspace tab', () => {
    expect(
      resolveTemplateDevWorkspaceTabFromQuery({ tab: 'authoring', authoringTab: 'testPreview' }),
    ).toBe('testing')
  })

  it('migrates legacy authoring sub-tabs to design workspace tab', () => {
    expect(
      resolveTemplateDevWorkspaceTabFromQuery({ tab: 'authoring', authoringTab: 'bindings' }),
    ).toBe('design')
  })

  it('migrates legacy lifecycle tab to approval workspace tab', () => {
    expect(resolveTemplateDevWorkspaceTabFromQuery({ tab: 'lifecycle' })).toBe('approval')
  })

  it('BDD-CE-U16-APC-001/002: resolves design sub-tab with bindings default and explicit priority', () => {
    expect(resolveDesignSubTabFromQuery({})).toBe('bindings')
    expect(resolveDesignSubTabFromQuery({ workspaceTab: 'design' })).toBe('bindings')
    expect(resolveDesignSubTabFromQuery({ designTab: 'variables' })).toBe('variables')
    expect(resolveDesignSubTabFromQuery({ designTab: 'contentModules' })).toBe('contentModules')
    expect(resolveDesignSubTabFromQuery({ authoringTab: 'bindings' })).toBe('bindings')
    expect(resolveDesignSubTabFromQuery({ authoringTab: 'testPreview' })).toBe('bindings')
    expect(resolveDesignSubTabFromQuery({ designTab: 'not-a-tab' })).toBe('bindings')
  })

  it('maps workflow focus to approval workspace tab', () => {
    expect(resolveTemplateDevWorkspaceTabFromQuery({ focus: 'workflow' })).toBe('approval')
    expect(resolveTemplateDevWorkspaceTabFromQuery({ tab: 'authoring', focus: 'workflow' })).toBe(
      'approval',
    )
  })

  it('builds normalized workspace query without legacy keys', () => {
    expect(
      buildDevWorkspaceQuery(
        { tab: 'authoring', authoringTab: 'variables', queue: 'REMEDIATION' },
        'design',
        'variables',
      ),
    ).toEqual({
      queue: 'REMEDIATION',
      workspaceTab: 'design',
      designTab: 'variables',
    })
    expect(
      buildDevWorkspaceQuery({ workspaceTab: 'testing' }, 'testing', 'previewRuns'),
    ).toEqual({
      workspaceTab: 'testing',
      testingTab: 'previewRuns',
    })
  })

  it('resolves testing sub-tab from query', () => {
    expect(resolveTestingSubTabFromQuery({ testingTab: 'coverage' })).toBe('coverage')
    expect(resolveTestingSubTabFromQuery({})).toBe('dataSets')
  })

  it('resolves approval sub-tab from query with focus fallback', () => {
    expect(resolveApprovalSubTabFromQuery({ approvalTab: 'riskConfig' })).toBe('riskConfig')
    expect(resolveApprovalSubTabFromQuery({ focus: 'lifecycle' })).toBe('governance')
    expect(resolveApprovalSubTabFromQuery({})).toBe('submitApproval')
  })

  it('builds approval workspace query with approvalTab', () => {
    expect(
      buildDevWorkspaceQuery({ workspaceTab: 'approval' }, 'approval', 'publishReadiness'),
    ).toEqual({
      workspaceTab: 'approval',
      approvalTab: 'publishReadiness',
    })
  })
})
