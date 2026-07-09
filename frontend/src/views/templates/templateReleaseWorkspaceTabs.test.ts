import { describe, expect, it } from 'vitest'
import {
  buildTemplateReleaseWorkspaceQuery,
  resolveTemplateReleaseWorkspaceTab,
  resolveTemplateReleaseWorkspaceTabFromQuery,
} from '@/views/templates/templateReleaseWorkspaceTabs'

describe('templateReleaseWorkspaceTabs', () => {
  it('resolves known workspace tabs and defaults to basics', () => {
    expect(resolveTemplateReleaseWorkspaceTab('testing')).toBe('testing')
    expect(resolveTemplateReleaseWorkspaceTab('approval')).toBe('approval')
    expect(resolveTemplateReleaseWorkspaceTab('unknown')).toBe('basics')
  })

  it('reads workspaceTab from route query', () => {
    expect(resolveTemplateReleaseWorkspaceTabFromQuery({ workspaceTab: 'variables' })).toBe(
      'variables',
    )
    expect(resolveTemplateReleaseWorkspaceTabFromQuery({})).toBe('basics')
  })

  it('builds query preserving unrelated params', () => {
    expect(
      buildTemplateReleaseWorkspaceQuery({ from: 'hub', workspaceTab: 'basics' }, 'approval'),
    ).toEqual({
      from: 'hub',
      workspaceTab: 'approval',
    })
  })
})
