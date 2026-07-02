export const TEMPLATE_APPROVAL_SUB_TABS = [
  'submitApproval',
  'publishReadiness',
  'riskConfig',
  'governance',
] as const

export type TemplateApprovalSubTab = (typeof TEMPLATE_APPROVAL_SUB_TABS)[number]

export const DEFAULT_TEMPLATE_APPROVAL_SUB_TAB: TemplateApprovalSubTab = 'submitApproval'

export const TEMPLATE_APPROVAL_SUB_TAB_LABEL_KEYS: Record<TemplateApprovalSubTab, string> = {
  submitApproval: 'templates.devWorkspace.approval.subTabs.submitApproval',
  publishReadiness: 'templates.devWorkspace.approval.subTabs.publishReadiness',
  riskConfig: 'templates.devWorkspace.approval.subTabs.riskConfig',
  governance: 'templates.devWorkspace.approval.subTabs.governance',
}

export function resolveTemplateApprovalSubTab(value: unknown): TemplateApprovalSubTab {
  if (typeof value === 'string' && (TEMPLATE_APPROVAL_SUB_TABS as readonly string[]).includes(value)) {
    return value as TemplateApprovalSubTab
  }
  return DEFAULT_TEMPLATE_APPROVAL_SUB_TAB
}

export function templateApprovalSubTabLabelKey(tab: TemplateApprovalSubTab): string {
  return TEMPLATE_APPROVAL_SUB_TAB_LABEL_KEYS[tab]
}
