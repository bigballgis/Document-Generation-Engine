import type { ComputedRef, Ref } from 'vue'
import type { RuntimeEnvironment } from '@/config/environments'
import type { useTemplatesStore } from '@/stores/templates'
import type { useTemplateLifecycleActions } from '@/views/templates/useTemplateLifecycleActions'
import type { useTemplatePolicyCredentials } from '@/views/templates/useTemplatePolicyCredentials'
import type { useTemplateDetailVisibility } from '@/views/templates/useTemplateDetailVisibility'
import type { useTemplatePreviewActions } from '@/views/templates/useTemplatePreviewActions'
import type { useTemplateDetailNavigation } from '@/views/templates/useTemplateDetailNavigation'
import type { TemplateDetail } from '@/types/template'

type Translate = (key: string, params?: Record<string, unknown>) => string
type HasKey = (key: string) => boolean

export function assembleTemplateDetailControllerApi(options: {
  t: Translate
  te: HasKey
  formatDateTime: (value: string) => string
  templateId: ComputedRef<string>
  devVersionId: ComputedRef<string>
  isDevEditor: ComputedRef<boolean>
  template: ComputedRef<TemplateDetail | null>
  showDetailSkeleton: ComputedRef<boolean>
  authorTemplates: ComputedRef<boolean>
  decideTests: ComputedRef<boolean>
  decideApprovals: ComputedRef<boolean>
  publishTemplates: ComputedRef<boolean>
  reviewMasters: ComputedRef<boolean>
  metadataEditOpen: Ref<boolean>
  selectedContractEnvironment: Ref<RuntimeEnvironment>
  templatesStore: ReturnType<typeof useTemplatesStore>
  lifecycle: ReturnType<typeof useTemplateLifecycleActions>
  policy: ReturnType<typeof useTemplatePolicyCredentials>
  visibility: ReturnType<typeof useTemplateDetailVisibility>
  preview: ReturnType<typeof useTemplatePreviewActions>
  navigation: ReturnType<typeof useTemplateDetailNavigation>
  handleMetadataUpdate: (payload: { name: string; description: string | null }) => Promise<void>
}) {
  const {
    t,
    te,
    formatDateTime,
    templateId,
    devVersionId,
    isDevEditor,
    template,
    showDetailSkeleton,
    authorTemplates,
    decideTests,
    decideApprovals,
    publishTemplates,
    reviewMasters,
    metadataEditOpen,
    selectedContractEnvironment,
    templatesStore,
    lifecycle,
    policy,
    visibility,
    preview,
    navigation,
    handleMetadataUpdate,
  } = options

  return {
    t,
    te,
    formatDateTime,
    templateId,
    devVersionId,
    isDevEditor,
    template,
    showDetailSkeleton,
    loadFailed: navigation.loadFailed,
    activeDetailTab: navigation.activeDetailTab,
    activeDevWorkspaceTab: navigation.activeDevWorkspaceTab,
    detailTabs: navigation.detailTabs,
    authorTemplates,
    decideTests,
    decideApprovals,
    publishTemplates,
    reviewMasters,
    showAuthorJourney: navigation.showAuthorJourney,
    authorJourneyContext: navigation.authorJourneyContext,
    showTesterJourney: navigation.showTesterJourney,
    testerJourneyContext: navigation.testerJourneyContext,
    showApproverJourney: navigation.showApproverJourney,
    approverJourneyContext: navigation.approverJourneyContext,
    showTeamLeadJourney: navigation.showTeamLeadJourney,
    teamLeadJourneyContext: navigation.teamLeadJourneyContext,
    authorJourneyPrimaryCtaDisabled: lifecycle.authorJourneyPrimaryCtaDisabled,
    handleJourneyCreate: navigation.handleJourneyCreate,
    handleJourneyDesign: navigation.handleJourneyDesign,
    handleJourneyTrialGenerate: navigation.handleJourneyTrialGenerate,
    handleJourneySubmitForTest: navigation.handleJourneySubmitForTest,
    handleJourneySubmitForApproval: navigation.handleJourneySubmitForApproval,
    handleJourneyReviewRequest: navigation.handleJourneyReviewRequest,
    handleJourneyCheckEvidence: navigation.handleJourneyCheckEvidence,
    handleJourneyRecordResult: navigation.handleJourneyRecordResult,
    handleJourneyApproverReviewRequest: navigation.handleJourneyApproverReviewRequest,
    handleJourneyApproverReviewSubmission: navigation.handleJourneyApproverReviewSubmission,
    handleJourneyApproverRecordDecision: navigation.handleJourneyApproverRecordDecision,
    handleJourneyTeamLeadReviewGoLiveRequest: navigation.handleJourneyTeamLeadReviewGoLiveRequest,
    handleJourneyTeamLeadRunPreReleaseChecks: navigation.handleJourneyTeamLeadRunPreReleaseChecks,
    handleJourneyTeamLeadConfirmGoLive: navigation.handleJourneyTeamLeadConfirmGoLive,
    showLifecycleSection: lifecycle.showLifecycleSection,
    showGovernanceSection: lifecycle.showGovernanceSection,
    showDraftActions: lifecycle.showDraftActions,
    showTestingDecisionActions: lifecycle.showTestingDecisionActions,
    showSubmitForApproval: lifecycle.showSubmitForApproval,
    showApprovalDecisionActions: lifecycle.showApprovalDecisionActions,
    showPublishActions: lifecycle.showPublishActions,
    showTestGenerate: visibility.showTestGenerate,
    showStopAction: lifecycle.showStopAction,
    showRestoreAction: lifecycle.showRestoreAction,
    showDeprecateAction: lifecycle.showDeprecateAction,
    showAuthoringSection: visibility.showAuthoringSection,
    canEditContentModuleReferences: visibility.canEditContentModuleReferences,
    showPolicyPanel: policy.showPolicyPanel,
    canPolicy: policy.canPolicy,
    showExportActions: visibility.showExportActions,
    showDeleteTemplateAction: lifecycle.showDeleteTemplateAction,
    showMetadataEdit: visibility.showMetadataEdit,
    policyLoadFailed: policy.policyLoadFailed,
    apiPolicy: policy.apiPolicy,
    loadingPolicy: policy.loadingPolicy,
    policySubmitting: policy.policySubmitting,
    policyLoadErrorKey: policy.policyLoadErrorKey,
    publishGateItems: lifecycle.publishGateItems,
    publishGateReady: lifecycle.publishGateReady,
    publishVersion: lifecycle.publishVersion,
    publishBumpLevel: lifecycle.publishBumpLevel,
    publishBumpOptions: lifecycle.publishBumpOptions,
    publishVersionConflict: lifecycle.publishVersionConflict,
    loadingPublishGate: lifecycle.loadingPublishGate,
    publishGateLoadError: lifecycle.publishGateLoadError,
    publishCoverageSummary: lifecycle.publishCoverageSummary,
    publishChangeDiffSummary: lifecycle.publishChangeDiffSummary,
    submitGateItems: lifecycle.submitGateItems,
    submitGateReady: lifecycle.submitGateReady,
    loadingSubmitGate: lifecycle.loadingSubmitGate,
    submitGateLoadError: lifecycle.submitGateLoadError,
    submitCoverageSummary: lifecycle.submitCoverageSummary,
    submitChangeDiffSummary: lifecycle.submitChangeDiffSummary,
    bindingGateResult: lifecycle.bindingGateResult,
    lifecycleComment: lifecycle.lifecycleComment,
    lifecycleCommentDialogOpen: lifecycle.lifecycleCommentDialogOpen,
    decisionDialogOpen: lifecycle.decisionDialogOpen,
    decisionDialogMode: lifecycle.decisionDialogMode,
    publishSummaryOpen: lifecycle.publishSummaryOpen,
    submitSummaryOpen: lifecycle.submitSummaryOpen,
    metadataEditOpen,
    credentialSecretDialogVisible: policy.credentialSecretDialogVisible,
    credentialSecretValue: policy.credentialSecretValue,
    credentialSecretExternalId: policy.credentialSecretExternalId,
    displayedCredentialSecret: policy.displayedCredentialSecret,
    lastPreview: preview.lastPreview,
    selectedPreviewId: preview.selectedPreviewId,
    selectedTestDataSetId: preview.selectedTestDataSetId,
    generatingPreview: preview.generatingPreview,
    generatingPreviewId: preview.generatingPreviewId,
    batchTesting: preview.batchTesting,
    coverageRefreshToken: preview.coverageRefreshToken,
    submitForTestDialogOpen: lifecycle.submitForTestDialogOpen,
    testerEvidenceViewed: navigation.testerEvidenceViewed,
    approverEvidenceViewed: navigation.approverEvidenceViewed,
    credentialColumnFilters: policy.credentialColumnFilters,
    credentialsCurrentPage: policy.credentialsCurrentPage,
    paginatedCredentials: policy.paginatedCredentials,
    credentialStatusFilterOptions: policy.credentialStatusFilterOptions,
    totalCredentialRows: policy.totalCredentialRows,
    sortCredentialsByCreatedAt: policy.sortCredentialsByCreatedAt,
    selectedContractEnvironment,
    templatesStore,
    loadTemplate: navigation.loadTemplate,
    loadPublishGateData: lifecycle.loadPublishGateData,
    loadSubmitGateData: lifecycle.loadSubmitGateData,
    loadPolicyData: policy.loadPolicyData,
    backToList: navigation.backToList,
    openLifecyclePanel: navigation.openLifecyclePanel,
    openDevWorkspaceTab: navigation.openDevWorkspaceTab,
    handleTestGenerate: preview.handleTestGenerate,
    handleBatchTestGenerate: preview.handleBatchTestGenerate,
    handlePreviewSelected: preview.handlePreviewSelected,
    handlePreviewRefreshed: preview.handlePreviewRefreshed,
    handleSubmitForTest: lifecycle.handleSubmitForTest,
    handleTestDecision: lifecycle.handleTestDecision,
    openApprovalRejectDialog: lifecycle.openApprovalRejectDialog,
    submitLifecycleDecision: lifecycle.submitLifecycleDecision,
    handleSubmitForApproval: lifecycle.handleSubmitForApproval,
    confirmSubmitFromSummary: lifecycle.confirmSubmitFromSummary,
    handleApprovalDecision: lifecycle.handleApprovalDecision,
    handlePublish: lifecycle.handlePublish,
    confirmPublishFromSummary: lifecycle.confirmPublishFromSummary,
    handleGovernanceAction: lifecycle.handleGovernanceAction,
    handleMetadataUpdate,
    handleCreateCredential: policy.handleCreateCredential,
    handleRotateCredential: policy.handleRotateCredential,
    handleRevokeCredential: policy.handleRevokeCredential,
    handleDeleteTemplate: lifecycle.handleDeleteTemplate,
  }
}
