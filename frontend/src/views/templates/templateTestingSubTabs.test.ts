import { describe, expect, it } from 'vitest'
import {
  resolveTemplateTestingSubTab,
  templateTestingSubTabLabelKey,
} from '@/views/templates/templateTestingSubTabs'

describe('templateTestingSubTabs', () => {
  it('resolves known testing sub-tabs', () => {
    expect(resolveTemplateTestingSubTab('dataSets')).toBe('dataSets')
    expect(resolveTemplateTestingSubTab('previewRuns')).toBe('previewRuns')
    expect(resolveTemplateTestingSubTab('coverage')).toBe('coverage')
    expect(resolveTemplateTestingSubTab('changeDiff')).toBe('changeDiff')
    expect(resolveTemplateTestingSubTab('unknown')).toBe('dataSets')
  })

  it('maps sub-tab label keys', () => {
    expect(templateTestingSubTabLabelKey('dataSets')).toBe(
      'templates.devWorkspace.testing.subTabs.dataSets',
    )
  })
})
