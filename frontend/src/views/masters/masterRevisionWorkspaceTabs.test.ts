import { describe, expect, it } from 'vitest'
import {
  buildMasterRevisionWorkspaceQuery,
  resolveMasterRevisionWorkspaceTab,
  resolveMasterRevisionWorkspaceTabFromQuery,
} from '@/views/masters/masterRevisionWorkspaceTabs'

describe('masterRevisionWorkspaceTabs', () => {
  it('resolves workspace tab from query with fallback', () => {
    expect(resolveMasterRevisionWorkspaceTabFromQuery({})).toBe('design')
    expect(resolveMasterRevisionWorkspaceTabFromQuery({ workspaceTab: 'approval' })).toBe('approval')
    expect(resolveMasterRevisionWorkspaceTab('invalid')).toBe('design')
  })

  it('builds query preserving unrelated keys', () => {
    expect(
      buildMasterRevisionWorkspaceQuery({ focus: 'anchors', workspaceTab: 'design' }, 'approval'),
    ).toEqual({ focus: 'anchors', workspaceTab: 'approval' })
  })
})
