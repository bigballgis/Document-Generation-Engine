import type { SemverBumpLevel } from '@/utils/semver'
import type { PublishGateDisplayItem } from '@/utils/templateLifecycleDecisionForm'
import type {
  AnchorBinding,
  BindingValidationResult,
  CompositionRule,
  PreviewRecord,
  TemplateLifecycleStatus,
  VariableSchema,
} from '@/types/template'

export type PublishBumpOption = {
  level: SemverBumpLevel
  label: string
  version: string
}

export type GovernanceAction = 'stop' | 'restore' | 'deprecate'

export type TemplateDetailDevWorkspaceProps = {
  templateId: string
  masterId: string
  variables: VariableSchema[]
  bindings: AnchorBinding[]
  rules: CompositionRule[] | null
  groupCode: string | null
  lifecycleStatus: TemplateLifecycleStatus
  approvalSubState?: 'PENDING_SUBMIT' | 'PENDING_DECISION' | null
  canEditContentModuleReferences: boolean
  coverageRefreshToken: number
  lastPreview: PreviewRecord | null
  selectedPreviewId: string | null
  selectedTestDataSetId: string | null
  showDraftActions: boolean
  showTestingDecisionActions: boolean
  showSubmitForApproval: boolean
  showApprovalDecisionActions: boolean
  showPublishActions: boolean
  showTestGenerate: boolean
  showStopAction: boolean
  showRestoreAction: boolean
  showDeprecateAction: boolean
  showGovernanceSection: boolean
  publishGateItems: PublishGateDisplayItem[]
  loadingPublishGate: boolean
  publishBumpLevel: SemverBumpLevel
  publishVersionConflict: boolean
  publishGateReady: boolean
  publishBumpOptions: PublishBumpOption[]
  submitGateItems: PublishGateDisplayItem[]
  loadingSubmitGate: boolean
  submitGateReady: boolean
  submitGateLoadError: string | null
  bindingGateResult: BindingValidationResult | null
  publishGateLoadError: string | null
  submitting: boolean
  generatingPreview: boolean
  generatingPreviewId: string | null
  openSubmitForTestDialog?: boolean
}
