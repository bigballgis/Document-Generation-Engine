import { describe, expect, it } from 'vitest'
import { buildVisibleNavGroups } from '@/navigation/navStructure'
import type { ManagementCapabilities } from '@/types/session'

const globalAdminCapabilities: ManagementCapabilities = {
  manageMasters: true,
  reviewMasters: true,
  authorTemplates: true,
  decideTests: true,
  decideApprovals: true,
  publishTemplates: true,
  stopTemplates: true,
  restoreOrDeprecateTemplates: true,
  deleteTemplates: true,
  manageApiPolicy: true,
  readAudit: true,
}

describe('navStructure', () => {
  it('does not include standalone workbench navigation', () => {
    const groups = buildVisibleNavGroups(
      ['route.dashboard-home', 'route.template-management'],
      ['TEMPLATE_TESTER'],
      {
        ...globalAdminCapabilities,
        decideApprovals: false,
        manageMasters: false,
        reviewMasters: false,
        publishTemplates: false,
        stopTemplates: false,
        restoreOrDeprecateTemplates: false,
        deleteTemplates: false,
        manageApiPolicy: false,
        readAudit: false,
      },
    )

    expect(groups.some((group) => group.id === 'workbench')).toBe(false)
  })

  it('keeps dashboard as the sole overview entry for collaboration roles', () => {
    const groups = buildVisibleNavGroups(
      ['route.dashboard-home', 'route.template-management'],
      ['TEMPLATE_TESTER'],
    )

    const overviewGroup = groups.find((group) => group.id === 'overview')
    expect(overviewGroup?.items.map((item) => item.id)).toEqual(['dashboard'])
  })
})
