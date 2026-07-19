import type { ApprovalSubState } from '@/types/approvalMatrix'
import type { TemplateLifecycleStatus } from '@/types/template'
import {
  isPendingApproverDecision,
  isPendingLegalDecision,
} from '@/utils/approvalMatrix'

export type TemplateWorkflowBannerCapabilities = {
  authorTemplates: boolean
  decideTests: boolean
  decideApprovals: boolean
  decideLegalApprovals: boolean
  publishTemplates: boolean
}

export type TemplateWorkflowBannerContext = {
  titleKey: string
  descriptionKey: string
}

export type WorkflowBannerActionKind = 'testing' | 'approval' | 'publish' | 'draft'

type TemplateWorkflowBannerLifecycleContext = {
  lifecycleStatus: TemplateLifecycleStatus
  approvalSubState?: ApprovalSubState | null
}

function matchesApprovalBanner(
  lifecycle: TemplateWorkflowBannerLifecycleContext,
  capabilities: TemplateWorkflowBannerCapabilities,
): boolean {
  if (lifecycle.lifecycleStatus !== 'APPROVAL') {
    return false
  }
  if (isPendingLegalDecision(lifecycle.approvalSubState)) {
    return capabilities.decideLegalApprovals
  }
  if (isPendingApproverDecision(lifecycle.approvalSubState)) {
    return capabilities.decideApprovals
  }
  return false
}

export function resolveWorkflowBannerActionKind(
  lifecycleStatus: TemplateLifecycleStatus,
  capabilities: TemplateWorkflowBannerCapabilities,
  approvalSubState?: ApprovalSubState | null,
): WorkflowBannerActionKind | null {
  const lifecycle: TemplateWorkflowBannerLifecycleContext = {
    lifecycleStatus,
    approvalSubState,
  }
  if (lifecycleStatus === 'TESTING' && capabilities.decideTests) {
    return 'testing'
  }
  if (matchesApprovalBanner(lifecycle, capabilities)) {
    return 'approval'
  }
  if (lifecycleStatus === 'PENDING_RELEASE' && capabilities.publishTemplates) {
    return 'publish'
  }
  if (lifecycleStatus === 'DRAFT' && capabilities.authorTemplates) {
    return 'draft'
  }
  return null
}

export function resolveTemplateWorkflowBannerContext(
  lifecycleStatus: TemplateLifecycleStatus,
  capabilities: TemplateWorkflowBannerCapabilities,
  approvalSubState?: ApprovalSubState | null,
): TemplateWorkflowBannerContext | null {
  const kind = resolveWorkflowBannerActionKind(
    lifecycleStatus,
    capabilities,
    approvalSubState,
  )
  if (!kind) {
    return null
  }
  if (kind === 'approval' && isPendingLegalDecision(approvalSubState)) {
    return {
      titleKey: 'dashboard.tasks.templateLegalApproval.title',
      descriptionKey: 'dashboard.tasks.templateLegalApproval.description',
    }
  }
  const byKind: Record<WorkflowBannerActionKind, TemplateWorkflowBannerContext> = {
    testing: {
      titleKey: 'dashboard.tasks.templateTest.title',
      descriptionKey: 'dashboard.tasks.templateTest.description',
    },
    approval: {
      titleKey: 'dashboard.tasks.templateApproval.title',
      descriptionKey: 'dashboard.tasks.templateApproval.description',
    },
    publish: {
      titleKey: 'dashboard.tasks.templatePublish.title',
      descriptionKey: 'dashboard.tasks.templatePublish.description',
    },
    draft: {
      titleKey: 'dashboard.tasks.templateDraft.title',
      descriptionKey: 'dashboard.tasks.templateDraft.description',
    },
  }
  return byKind[kind]
}
