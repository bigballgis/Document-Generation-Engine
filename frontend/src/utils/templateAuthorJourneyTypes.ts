import type { ApprovalSubState } from '@/types/approvalMatrix'
import type { TemplateLifecycleStatus, TemplateSummary } from '@/types/template'

export interface TemplateAuthorJourneyContext {
  lifecycleStatus: TemplateLifecycleStatus
  approvalSubState?: ApprovalSubState | null
  bindingsCount?: number
  hasSuccessfulTrialOutput?: boolean
  isRemediation?: boolean
}

export interface TemplateAuthorJourneyResolution {
  currentStepIndex: number | null
  guidanceKey?: string
  activeStepId?: string
  targetTemplateId?: string
}

export type TemplateAuthorDashboardTemplate = TemplateSummary & {
  bindingsCount?: number
  hasSuccessfulTrialOutput?: boolean
  approvalSubState?: ApprovalSubState | null
}

export interface TemplateAuthorRemediationItem {
  templateId: string
  createdAt: string
  updatedAt?: string
}
