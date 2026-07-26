import { describe, expect, it } from 'vitest'
import {
  buildDevWorkspaceQuery,
} from '@/views/templates/templateDevWorkspaceTabs'
import { stripAuthoringPathGuideQuery } from '@/utils/templateAuthoringPathGuide'

describe('FOS-W2-2 lifecycle stepper escapes authoring guide', () => {
  it('strips guide keys when building a Testing navigation query', () => {
    const current = {
      workspaceTab: 'design',
      authoringGuide: '1',
      authoringGuideStep: 'master',
    }
    const next = buildDevWorkspaceQuery(
      stripAuthoringPathGuideQuery(current),
      'testing',
      'previewRuns',
    )
    expect(next.authoringGuide).toBeUndefined()
    expect(next.authoringGuideStep).toBeUndefined()
    expect(next.workspaceTab).toBe('testing')
    expect(next.testingTab).toBe('previewRuns')
  })
})
