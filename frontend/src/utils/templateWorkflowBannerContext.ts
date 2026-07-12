import type { TemplateLifecycleStatus } from '@/types/template'

export type TemplateWorkflowBannerCapabilities = {
  authorTemplates: boolean
  decideTests: boolean
  decideApprovals: boolean
  publishTemplates: boolean
}

export type TemplateWorkflowBannerContext = {
  titleKey: string
  descriptionKey: string
}

export type WorkflowBannerActionKind = 'testing' | 'approval' | 'publish' | 'draft'

type TemplateWorkflowBannerLifecycleContext = {
  lifecycleStatus: TemplateLifecycleStatus
  approvalSubState?: 'PENDING_SUBMIT' | 'PENDING_DECISION' | null
}

type BannerRule = {
  kind: WorkflowBannerActionKind
  status: TemplateLifecycleStatus
  capability: keyof TemplateWorkflowBannerCapabilities
  titleKey: string
  descriptionKey: string
}

const WORKFLOW_BANNER_RULES: readonly BannerRule[] = [
  {
    kind: 'testing',
    status: 'TESTING',
    capability: 'decideTests',
    titleKey: 'dashboard.tasks.templateTest.title',
    descriptionKey: 'dashboard.tasks.templateTest.description',
  },
  {
    kind: 'approval',
    status: 'APPROVAL',
    capability: 'decideApprovals',
    titleKey: 'dashboard.tasks.templateApproval.title',
    descriptionKey: 'dashboard.tasks.templateApproval.description',
  },
  {
    kind: 'publish',
    status: 'PENDING_RELEASE',
    capability: 'publishTemplates',
    titleKey: 'dashboard.tasks.templatePublish.title',
    descriptionKey: 'dashboard.tasks.templatePublish.description',
  },
  {
    kind: 'draft',
    status: 'DRAFT',
    capability: 'authorTemplates',
    titleKey: 'dashboard.tasks.templateDraft.title',
    descriptionKey: 'dashboard.tasks.templateDraft.description',
  },
] as const

function matchesBannerRule(
  rule: BannerRule,
  lifecycle: TemplateWorkflowBannerLifecycleContext,
  capabilities: TemplateWorkflowBannerCapabilities,
): boolean {
  if (lifecycle.lifecycleStatus !== rule.status || !capabilities[rule.capability]) {
    return false
  }
  if (rule.kind === 'approval') {
    return lifecycle.approvalSubState === 'PENDING_DECISION'
  }
  return true
}

export function resolveWorkflowBannerActionKind(
  lifecycleStatus: TemplateLifecycleStatus,
  capabilities: TemplateWorkflowBannerCapabilities,
  approvalSubState?: 'PENDING_SUBMIT' | 'PENDING_DECISION' | null,
): WorkflowBannerActionKind | null {
  const lifecycle: TemplateWorkflowBannerLifecycleContext = {
    lifecycleStatus,
    approvalSubState,
  }
  const rule = WORKFLOW_BANNER_RULES.find((entry) =>
    matchesBannerRule(entry, lifecycle, capabilities),
  )
  return rule?.kind ?? null
}

export function resolveTemplateWorkflowBannerContext(
  lifecycleStatus: TemplateLifecycleStatus,
  capabilities: TemplateWorkflowBannerCapabilities,
  approvalSubState?: 'PENDING_SUBMIT' | 'PENDING_DECISION' | null,
): TemplateWorkflowBannerContext | null {
  const lifecycle: TemplateWorkflowBannerLifecycleContext = {
    lifecycleStatus,
    approvalSubState,
  }
  const rule = WORKFLOW_BANNER_RULES.find((entry) =>
    matchesBannerRule(entry, lifecycle, capabilities),
  )
  if (!rule) {
    return null
  }
  return {
    titleKey: rule.titleKey,
    descriptionKey: rule.descriptionKey,
  }
}

