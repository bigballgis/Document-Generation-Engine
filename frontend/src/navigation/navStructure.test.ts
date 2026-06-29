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
  it('includes workbench nav items based on role capabilities', () => {
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

    const workbenchGroup = groups.find((group) => group.id === 'workbench')
    expect(workbenchGroup?.items.map((item) => item.id)).toEqual(['tester-workbench'])
  })

  it('includes tester workbench from role when capabilities are absent', () => {
    const groups = buildVisibleNavGroups(
      ['route.dashboard-home', 'route.template-management'],
      ['TEMPLATE_TESTER'],
    )

    const workbenchGroup = groups.find((group) => group.id === 'workbench')
    expect(workbenchGroup?.items.map((item) => item.id)).toEqual(['tester-workbench'])
  })

  it('includes approver workbench from role when capabilities are absent', () => {
    const groups = buildVisibleNavGroups(
      ['route.dashboard-home', 'route.template-management'],
      ['TEMPLATE_APPROVER'],
    )

    const workbenchGroup = groups.find((group) => group.id === 'workbench')
    expect(workbenchGroup?.items.map((item) => item.id)).toEqual(['approver-workbench'])
  })

  it('includes escalation workbench for group admins', () => {
    const groups = buildVisibleNavGroups(
      ['route.dashboard-home', 'route.template-management'],
      ['GROUP_ADMIN'],
      globalAdminCapabilities,
    )

    const workbenchGroup = groups.find((group) => group.id === 'workbench')
    expect(workbenchGroup?.items.some((item) => item.id === 'escalation-workbench')).toBe(true)
  })
})
