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

export function resolveWorkflowBannerActionKind(
  lifecycleStatus: TemplateLifecycleStatus,
  capabilities: TemplateWorkflowBannerCapabilities,
): WorkflowBannerActionKind | null {
  const rule = WORKFLOW_BANNER_RULES.find(
    (entry) => entry.status === lifecycleStatus && capabilities[entry.capability],
  )
  return rule?.kind ?? null
}

export function resolveTemplateWorkflowBannerContext(
  lifecycleStatus: TemplateLifecycleStatus,
  capabilities: TemplateWorkflowBannerCapabilities,
): TemplateWorkflowBannerContext | null {
  const rule = WORKFLOW_BANNER_RULES.find(
    (entry) => entry.status === lifecycleStatus && capabilities[entry.capability],
  )
  if (!rule) {
    return null
  }
  return {
    titleKey: rule.titleKey,
    descriptionKey: rule.descriptionKey,
  }
}

export function hasWorkflowBannerAction(
  lifecycleStatus: TemplateLifecycleStatus,
  capabilities: TemplateWorkflowBannerCapabilities,
): boolean {
  return resolveWorkflowBannerActionKind(lifecycleStatus, capabilities) !== null
}
