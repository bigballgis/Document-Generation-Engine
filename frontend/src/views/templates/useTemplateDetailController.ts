import { computed, ref, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { DEFAULT_ENVIRONMENT, type RuntimeEnvironment } from '@/config/environments'
import { useCapabilities } from '@/composables/useCapabilities'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { useTemplatesStore } from '@/stores/templates'
import {
  useTemplateLifecycleActions,
  type GovernanceAction,
  type LifecycleDecisionDialogMode,
} from '@/views/templates/useTemplateLifecycleActions'
import { useTemplatePolicyCredentials } from '@/views/templates/useTemplatePolicyCredentials'
import {
  resolveTemplateDetailTabFromQuery,
  type TemplateDetailTab,
} from '@/views/templates/templateDetailTabs'
import {
  resolveTemplateDevWorkspaceTabFromQuery,
  type TemplateDevWorkspaceTab,
} from '@/views/templates/templateDevWorkspaceTabs'
import { useTemplateDetailNavigation } from '@/views/templates/useTemplateDetailNavigation'
import { useTemplatePreviewActions } from '@/views/templates/useTemplatePreviewActions'
import { useTemplateDetailVisibility } from '@/views/templates/useTemplateDetailVisibility'

export type { GovernanceAction, LifecycleDecisionDialogMode }

export function useTemplateDetailController(workspace: Ref<'legacy' | 'dev-editor'>) {
  const isDevEditor = computed(() => workspace.value === 'dev-editor')
  const { t, te } = useI18n()
  const { formatDateTime } = useLocaleFormatters()
  const route = useRoute()
  const templatesStore = useTemplatesStore()
  const { authorTemplates, decideTests, decideApprovals, publishTemplates, reviewMasters } =
    useCapabilities()

  const metadataEditOpen = ref(false)
  const selectedContractEnvironment = ref<RuntimeEnvironment>(DEFAULT_ENVIRONMENT)
  const templateId = computed(() => route.params.templateId as string)
  const devVersionId = computed(() =>
    isDevEditor.value ? String(route.params.devVersionId ?? '') : '',
  )
  const loadTemplateHolder = { fn: async (): Promise<void> => {} }

  const template = computed(() => {
    if (templatesStore.selectedTemplate?.id !== templateId.value) {
      return null
    }
    return templatesStore.selectedTemplate
  })

  const showDetailSkeleton = computed(() => {
    const selected = templatesStore.selectedTemplate
    // Keep the mounted workspace during soft refresh of the same template.
    if (selected?.id === templateId.value) {
      return false
    }
    if (templatesStore.loadingDetail) {
      return true
    }
    return selected !== null && selected.id !== templateId.value
  })

  const errorMessage = computed(() => {
    const key = templatesStore.lastErrorMessageKey
    return !key ? '' : te(key) ? t(key) : t('templates.error.loadDetail')
  })

  const activeDetailTab = ref<TemplateDetailTab>(
    isDevEditor.value ? 'authoring' : resolveTemplateDetailTabFromQuery(route.query),
  )
  const activeDevWorkspaceTab = ref<TemplateDevWorkspaceTab>(
    resolveTemplateDevWorkspaceTabFromQuery(route.query),
  )

  const lifecycle = useTemplateLifecycleActions({
    templateId,
    template,
    isDevEditor,
    errorMessage,
    loadTemplate: () => loadTemplateHolder.fn(),
    activeDetailTab,
  })

  const policy = useTemplatePolicyCredentials({ templateId, template, errorMessage })

  const visibility = useTemplateDetailVisibility({ isDevEditor, template })

  const previewActionsRef: { openDevWorkspaceTab: (tab: TemplateDevWorkspaceTab) => void } = {
    openDevWorkspaceTab: () => {},
  }

  const preview = useTemplatePreviewActions({
    templateId,
    errorMessage,
    openDevWorkspaceTab: (tab) => previewActionsRef.openDevWorkspaceTab(tab),
  })

  const navigation = useTemplateDetailNavigation({
    isDevEditor,
    templateId,
    devVersionId,
    template,
    lastPreview: preview.lastPreview,
    showAuthoringSection: visibility.showAuthoringSection,
    activeDetailTab,
    activeDevWorkspaceTab,
    loadTemplateHolder,
    lifecycle,
    policy,
    handleTestGenerate: preview.handleTestGenerate,
  })

  previewActionsRef.openDevWorkspaceTab = navigation.openDevWorkspaceTab

  async function handleMetadataUpdate(payload: { name: string; description: string | null }) {
    try {
      await templatesStore.updateTemplateMetadata(templateId.value, payload)
      metadataEditOpen.value = false
      ElMessage.success(t('templates.metadata.success'))
    } catch {
      ElMessage.error(errorMessage.value || t('templates.error.updateMetadata'))
    }
  }

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
