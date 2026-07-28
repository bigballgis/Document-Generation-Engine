import { afterEach, describe, expect, it } from 'vitest'
import {
  AUTHORING_PATH_GUIDE_STEPS,
  buildPostCreateAuthoringPath,
  dismissAuthoringPathGuide,
  isAuthoringPathGuideDismissed,
  isAuthoringPathGuideQueryActive,
  isAuthoringPathGuideVisible,
  nextAuthoringPathGuideStep,
  resolveAuthoringPathGuideNavigateQuery,
  resolveAuthoringPathGuideStep,
  stripAuthoringPathGuideQuery,
} from '@/utils/templateAuthoringPathGuide'

describe('templateAuthoringPathGuide', () => {
  afterEach(() => {
    sessionStorage.clear()
  })

  it('FOS-W2-3 / BDD-CE-U16-APC: orders Master → Variables → Bindings → Preview', () => {
    expect(AUTHORING_PATH_GUIDE_STEPS).toEqual(['master', 'variables', 'bindings', 'preview'])
  })

  it('BDD-CE-U16-APC-003: post-create path lands on dev with guide at Master', () => {
    expect(buildPostCreateAuthoringPath('tpl-1', 'dev-9')).toBe(
      '/templates/tpl-1/dev/dev-9?workspaceTab=design&authoringGuide=1&authoringGuideStep=master',
    )
  })

  it('BDD-CE-U16-APC-004: maps guide steps to workspace / sub-tab query', () => {
    expect(resolveAuthoringPathGuideNavigateQuery('bindings')).toEqual({
      workspaceTab: 'design',
      designTab: 'bindings',
      authoringGuide: '1',
      authoringGuideStep: 'bindings',
    })
    expect(resolveAuthoringPathGuideNavigateQuery('variables')).toEqual({
      workspaceTab: 'design',
      designTab: 'variables',
      authoringGuide: '1',
      authoringGuideStep: 'variables',
    })
    expect(resolveAuthoringPathGuideNavigateQuery('preview')).toEqual({
      workspaceTab: 'testing',
      testingTab: 'previewRuns',
      authoringGuide: '1',
      authoringGuideStep: 'preview',
    })
  })

  it('resolves current step from authoringGuideStep or workspace inference', () => {
    expect(resolveAuthoringPathGuideStep({ authoringGuide: '1', authoringGuideStep: 'master' })).toBe(
      'master',
    )
    expect(resolveAuthoringPathGuideStep({ authoringGuide: '1', designTab: 'variables' })).toBe(
      'variables',
    )
    expect(
      resolveAuthoringPathGuideStep({ authoringGuide: '1', workspaceTab: 'testing', testingTab: 'previewRuns' }),
    ).toBe('preview')
    expect(resolveAuthoringPathGuideStep({ authoringGuide: '1' })).toBe('master')
  })

  it('BDD-CE-U16-APC-005: dismiss hides guide for the template session', () => {
    expect(isAuthoringPathGuideQueryActive({ authoringGuide: '1' })).toBe(true)
    expect(isAuthoringPathGuideVisible('tpl-1', { authoringGuide: '1' })).toBe(true)
    dismissAuthoringPathGuide('tpl-1')
    expect(isAuthoringPathGuideDismissed('tpl-1')).toBe(true)
    expect(isAuthoringPathGuideVisible('tpl-1', { authoringGuide: '1' })).toBe(false)
    expect(isAuthoringPathGuideVisible('tpl-2', { authoringGuide: '1' })).toBe(true)
  })

  it('BDD-CE-U16-APC-007: without authoringGuide query the guide is inactive', () => {
    expect(isAuthoringPathGuideQueryActive({})).toBe(false)
    expect(isAuthoringPathGuideVisible('tpl-1', { workspaceTab: 'design' })).toBe(false)
  })

  it('advances Next through steps and strips guide markers on dismiss query', () => {
    expect(nextAuthoringPathGuideStep('master')).toBe('variables')
    expect(nextAuthoringPathGuideStep('variables')).toBe('bindings')
    expect(nextAuthoringPathGuideStep('preview')).toBeNull()
    expect(
      stripAuthoringPathGuideQuery({
        workspaceTab: 'design',
        designTab: 'bindings',
        authoringGuide: '1',
        authoringGuideStep: 'bindings',
        queue: 'REMEDIATION',
      }),
    ).toEqual({
      workspaceTab: 'design',
      designTab: 'bindings',
      queue: 'REMEDIATION',
    })
  })
})
