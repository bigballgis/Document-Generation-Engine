import type { MasterRevisionWorkspaceTab } from '@/views/masters/masterRevisionWorkspaceTabs'

export type MasterDesignerWorkspaceNavigation = MasterRevisionWorkspaceTab | 'upload'

export function resolveMasterDesignerWorkspaceNavigation(
  stepId: string | undefined,
): MasterDesignerWorkspaceNavigation | null {
  if (!stepId) {
    return null
  }

  switch (stepId) {
    case 'placeholders':
      return 'design'
    case 'submitReview':
      return 'approval'
    case 'upload':
    case 'rework':
      return 'upload'
    default:
      return null
  }
}
