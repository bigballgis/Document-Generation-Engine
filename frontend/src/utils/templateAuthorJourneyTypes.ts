import type { TemplateLifecycleStatus, TemplateSummary } from '@/types/template'

export interface TemplateAuthorJourneyContext {
  lifecycleStatus: TemplateLifecycleStatus
  approvalSubState?: 'PENDING_SUBMIT' | 'PENDING_DECISION' | null
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
  approvalSubState?: 'PENDING_SUBMIT' | 'PENDING_DECISION' | null
}

export interface TemplateAuthorRemediationItem {
  templateId: string
  createdAt: string
  updatedAt?: string
}
