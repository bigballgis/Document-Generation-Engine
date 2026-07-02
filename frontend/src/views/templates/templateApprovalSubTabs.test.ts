import { describe, expect, it } from 'vitest'
import {
  resolveTemplateApprovalSubTab,
  templateApprovalSubTabLabelKey,
} from '@/views/templates/templateApprovalSubTabs'

describe('templateApprovalSubTabs', () => {
  it('resolves known approval sub-tabs', () => {
    expect(resolveTemplateApprovalSubTab('submitApproval')).toBe('submitApproval')
    expect(resolveTemplateApprovalSubTab('publishReadiness')).toBe('publishReadiness')
    expect(resolveTemplateApprovalSubTab('riskConfig')).toBe('riskConfig')
    expect(resolveTemplateApprovalSubTab('governance')).toBe('governance')
    expect(resolveTemplateApprovalSubTab('unknown')).toBe('submitApproval')
  })

  it('maps sub-tab label keys', () => {
    expect(templateApprovalSubTabLabelKey('publishReadiness')).toBe(
      'templates.devWorkspace.approval.subTabs.publishReadiness',
    )
  })
})
