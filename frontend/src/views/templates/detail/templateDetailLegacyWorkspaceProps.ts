import type { SemverBumpLevel } from '@/utils/semver'
import type { PublishGateDisplayItem } from '@/utils/templateLifecycleDecisionForm'
import type {
  ApiCredentialSummary,
  ApiPolicy,
  BindingValidationResult,
  TemplateDetail,
} from '@/types/template'

export type PublishBumpOption = {
  level: SemverBumpLevel
  label: string
  version: string
}

export type TemplateDetailLegacyWorkspaceProps = {
  template: TemplateDetail
  templateId: string
  detailTabs: Array<{ name: string; labelKey: string }>
  formatDateTime: (value: string) => string
  showLifecycleSection: boolean
  showGovernanceSection: boolean
  showDraftActions: boolean
  showTestingDecisionActions: boolean
  showSubmitForApproval: boolean
  showApprovalDecisionActions: boolean
  showPublishActions: boolean
  showTestGenerate: boolean
  showStopAction: boolean
  showRestoreAction: boolean
  showDeprecateAction: boolean
  showAuthoringSection: boolean
  canEditContentModuleReferences: boolean
  showPolicyPanel: boolean
  coverageRefreshToken: number
  publishGateItems: PublishGateDisplayItem[]
  loadingPublishGate: boolean
  publishVersionConflict: boolean
  publishGateReady: boolean
  publishBumpOptions: PublishBumpOption[]
  bindingGateResult: BindingValidationResult | null
  publishGateLoadError: string | null
  submitGateItems: PublishGateDisplayItem[]
  loadingSubmitGate: boolean
  submitGateReady: boolean
  submitGateLoadError: string | null
  submitting: boolean
  loadingPolicy: boolean
  apiPolicy: ApiPolicy | null
  policyLoadFailed: boolean
  policyLoadErrorKey: string | null
  paginatedCredentials: ApiCredentialSummary[]
  credentialStatusFilterOptions: Array<{ label: string; value: string }>
  totalCredentialRows: number
  policySubmitting: boolean
  sortCredentialsByCreatedAt: (a: ApiCredentialSummary, b: ApiCredentialSummary) => number
}
