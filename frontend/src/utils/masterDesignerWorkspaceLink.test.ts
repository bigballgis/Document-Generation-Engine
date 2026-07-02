import { describe, expect, it } from 'vitest'
import { resolveMasterDesignerWorkspaceNavigation } from '@/utils/masterDesignerWorkspaceLink'

describe('masterDesignerWorkspaceLink', () => {
  it('maps designer journey steps to workspace navigation targets', () => {
    expect(resolveMasterDesignerWorkspaceNavigation('placeholders')).toBe('design')
    expect(resolveMasterDesignerWorkspaceNavigation('submitReview')).toBe('approval')
    expect(resolveMasterDesignerWorkspaceNavigation('upload')).toBe('upload')
    expect(resolveMasterDesignerWorkspaceNavigation('unknown')).toBeNull()
  })
})
