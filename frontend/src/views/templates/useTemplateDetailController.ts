import { computed, ref, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { DEFAULT_ENVIRONMENT, type RuntimeEnvironment } from '@/config/environments'
import { useCapabilities } from '@/composables/useCapabilities'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { useTemplatesStore } from '@/stores/templates'
import * as templatesApi from '@/api/templates'
import { isTemplateExportEligible } from '@/utils/templateExportEligibility'
import type { PreviewRecord } from '@/types/template'
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

export type { GovernanceAction, LifecycleDecisionDialogMode }

export function useTemplateDetailController(workspace: Ref<'legacy' | 'dev-editor'>) {
  const isDevEditor = computed(() => workspace.value === 'dev-editor')

  const { t, te } = useI18n()
  const { formatDateTime } = useLocaleFormatters()
  const route = useRoute()
  const templatesStore = useTemplatesStore()
  const {
    authorTemplates,
    decideTests,
    decideApprovals,
    publishTemplates,
    reviewMasters,
    exportTemplates,
    editTemplateMetadata,
  } = useCapabilities()

  const lastPreview = ref<PreviewRecord | null>(null)
  const selectedPreviewId = ref<string | null>(null)
  const selectedTestDataSetId = ref<string | null>(null)
  const generatingPreview = ref(false)
  const generatingPreviewId = ref<string | null>(null)
  const batchTesting = ref(false)
  const coverageRefreshToken = ref(0)
  const metadataEditOpen = ref(false)
  const selectedContractEnvironment = ref<RuntimeEnvironment>(DEFAULT_ENVIRONMENT)

  const templateId = computed(() => route.params.templateId as string)
  const devVersionId = computed(() =>
    isDevEditor.value ? String(route.params.devVersionId ?? '') : '',
  )

  const loadTemplateHolder = { fn: async (): Promise<void> => {} }

  const templateMatchesRoute = computed(
    () => templatesStore.selectedTemplate?.id === templateId.value,
  )

  const template = computed(() => {
    if (!templateMatchesRoute.value) {
      return null
    }
    return templatesStore.selectedTemplate
  })

  const showDetailSkeleton = computed(() => {
    if (templatesStore.loadingDetail) {
      return true
    }
    const selected = templatesStore.selectedTemplate
    return selected !== null && selected.id !== templateId.value
  })

  const errorMessage = computed(() => {
    const key = templatesStore.lastErrorMessageKey
    if (!key) {
      return ''
    }
    return te(key) ? t(key) : t('templates.error.loadDetail')
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

  const policy = useTemplatePolicyCredentials({
    templateId,
    template,
    errorMessage,
  })

  const showMetadataEdit = computed(() => {
    const status = template.value?.lifecycleStatus
    if (!status || !editTemplateMetadata.value) {
      return false
    }
    return status !== 'PUBLISHED' && status !== 'STOPPED' && status !== 'DEPRECATED'
  })

  const showExportActions = computed(
    () =>
      exportTemplates.value &&
      Boolean(template.value) &&
      isTemplateExportEligible(template.value!.lifecycleStatus),
  )

  const showAuthoringSection = computed(() => {
    const status = template.value?.lifecycleStatus
    if (
      !status ||
      status === 'PUBLISHED' ||
      status === 'STOPPED' ||
      status === 'DEPRECATED'
    ) {
      return false
    }
    if (authorTemplates.value) {
      return true
    }
    return (
      isDevEditor.value &&
      decideTests.value &&
      (status === 'DRAFT' || status === 'TESTING')
    )
  })

  const canEditContentModuleReferences = computed(
    () => authorTemplates.value && template.value?.lifecycleStatus === 'DRAFT',
  )

  const showTestGenerate = computed(
    () =>
      authorTemplates.value &&
      (template.value?.lifecycleStatus === 'DRAFT' ||
        template.value?.lifecycleStatus === 'TESTING'),
  )

  const handleTestGenerateRef: { fn: (testDataSetId?: string) => Promise<void> } = {
    fn: async () => {},
  }

  const navigation = useTemplateDetailNavigation({
    isDevEditor,
    templateId,
    devVersionId,
    template,
    lastPreview,
    showAuthoringSection,
    activeDetailTab,
    activeDevWorkspaceTab,
    loadTemplateHolder,
    lifecycle,
    policy,
    handleTestGenerate: (testDataSetId) => handleTestGenerateRef.fn(testDataSetId),
  })

  async function handleTestGenerate(testDataSetId?: string) {
    const resolvedId = testDataSetId ?? selectedTestDataSetId.value ?? undefined
    generatingPreview.value = true
    generatingPreviewId.value = resolvedId ?? null
    try {
      const preview = await templatesStore.testGenerate(templateId.value, {
        testDataSetId: resolvedId,
      })
      lastPreview.value = preview
      selectedPreviewId.value = preview.previewId
      if (resolvedId) {
        selectedTestDataSetId.value = resolvedId
      }
      navigation.openDevWorkspaceTab('testing')
      ElMessage.success(t('templates.testGenerate.success', { previewId: preview.previewId }))
    } catch {
      ElMessage.error(errorMessage.value || t('templates.error.testGenerate'))
    } finally {
      generatingPreview.value = false
      generatingPreviewId.value = null
    }
  }

  handleTestGenerateRef.fn = handleTestGenerate

  async function handleBatchTestGenerate() {
    batchTesting.value = true
    try {
      const dataSets = await templatesApi.listTestDataSets(templateId.value)
      if (dataSets.length === 0) {
        ElMessage.warning(t('templates.testDataSets.error.noDataSetsForBatch'))
        return
      }
      const summary = await templatesApi.batchTestGenerate(templateId.value, {
        testDataSetIds: dataSets.map((row) => row.testDataSetId),
      })
      coverageRefreshToken.value += 1
      navigation.openDevWorkspaceTab('testing')
      ElMessage.success(
        t('templates.testDataSets.batchSuccess', {
          succeeded: summary.succeededCount,
          failed: summary.failedCount,
          warnings: summary.warningCount,
        }),
      )
    } catch {
      ElMessage.error(t('templates.testDataSets.error.batch'))
    } finally {
      batchTesting.value = false
    }
  }

  async function handlePreviewSelected(previewId: string | null) {
    selectedPreviewId.value = previewId
    if (!previewId) {
      lastPreview.value = null
      return
    }
    try {
      lastPreview.value = await templatesApi.getPreview(templateId.value, previewId)
    } catch {
      ElMessage.error(errorMessage.value || t('templates.previewHistory.error.loadDetail'))
    }
  }

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
    // i18n
    t,
    te,
    // formatters
    formatDateTime,
    // route state
    templateId,
    devVersionId,
    // core mode
    isDevEditor,
    // template data
    template,
    showDetailSkeleton,
    loadFailed: navigation.loadFailed,
    // tabs
    activeDetailTab: navigation.activeDetailTab,
    activeDevWorkspaceTab: navigation.activeDevWorkspaceTab,
    detailTabs: navigation.detailTabs,
    // capabilities
    authorTemplates,
    decideTests,
    decideApprovals,
    publishTemplates,
    reviewMasters,
    // journey display
    showAuthorJourney: navigation.showAuthorJourney,
    authorJourneyContext: navigation.authorJourneyContext,
    showTesterJourney: navigation.showTesterJourney,
    testerJourneyContext: navigation.testerJourneyContext,
    showApproverJourney: navigation.showApproverJourney,
    approverJourneyContext: navigation.approverJourneyContext,
    showTeamLeadJourney: navigation.showTeamLeadJourney,
    teamLeadJourneyContext: navigation.teamLeadJourneyContext,
    authorJourneyPrimaryCtaDisabled: lifecycle.authorJourneyPrimaryCtaDisabled,
    // journey handlers
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
    // lifecycle visibility
    showLifecycleSection: lifecycle.showLifecycleSection,
    showGovernanceSection: lifecycle.showGovernanceSection,
    showDraftActions: lifecycle.showDraftActions,
    showTestingDecisionActions: lifecycle.showTestingDecisionActions,
    showSubmitForApproval: lifecycle.showSubmitForApproval,
    showApprovalDecisionActions: lifecycle.showApprovalDecisionActions,
    showPublishActions: lifecycle.showPublishActions,
    showTestGenerate,
    showStopAction: lifecycle.showStopAction,
    showRestoreAction: lifecycle.showRestoreAction,
    showDeprecateAction: lifecycle.showDeprecateAction,
    // authoring visibility
    showAuthoringSection,
    canEditContentModuleReferences,
    // policy
    showPolicyPanel: policy.showPolicyPanel,
    canPolicy: policy.canPolicy,
    showExportActions,
    showDeleteTemplateAction: lifecycle.showDeleteTemplateAction,
    showMetadataEdit,
    policyLoadFailed: policy.policyLoadFailed,
    apiPolicy: policy.apiPolicy,
    loadingPolicy: policy.loadingPolicy,
    policySubmitting: policy.policySubmitting,
    policyLoadErrorKey: policy.policyLoadErrorKey,
    // publish gate
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
    // submit gate
    submitGateItems: lifecycle.submitGateItems,
    submitGateReady: lifecycle.submitGateReady,
    loadingSubmitGate: lifecycle.loadingSubmitGate,
    submitGateLoadError: lifecycle.submitGateLoadError,
    submitCoverageSummary: lifecycle.submitCoverageSummary,
    submitChangeDiffSummary: lifecycle.submitChangeDiffSummary,
    // binding gate
    bindingGateResult: lifecycle.bindingGateResult,
    // dialog state
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
    // test state
    lastPreview,
    selectedPreviewId,
    selectedTestDataSetId,
    generatingPreview,
    generatingPreviewId,
    batchTesting,
    coverageRefreshToken,
    submitForTestDialogOpen: lifecycle.submitForTestDialogOpen,
    // tester evidence
    testerEvidenceViewed: navigation.testerEvidenceViewed,
    approverEvidenceViewed: navigation.approverEvidenceViewed,
    // credentials table
    credentialColumnFilters: policy.credentialColumnFilters,
    credentialsCurrentPage: policy.credentialsCurrentPage,
    paginatedCredentials: policy.paginatedCredentials,
    credentialStatusFilterOptions: policy.credentialStatusFilterOptions,
    totalCredentialRows: policy.totalCredentialRows,
    sortCredentialsByCreatedAt: policy.sortCredentialsByCreatedAt,
    // contract
    selectedContractEnvironment,
    // store
    templatesStore,
    // handlers
    loadTemplate: navigation.loadTemplate,
    loadPublishGateData: lifecycle.loadPublishGateData,
    loadSubmitGateData: lifecycle.loadSubmitGateData,
    loadPolicyData: policy.loadPolicyData,
    backToList: navigation.backToList,
    openLifecyclePanel: navigation.openLifecyclePanel,
    openDevWorkspaceTab: navigation.openDevWorkspaceTab,
    handleTestGenerate,
    handleBatchTestGenerate,
    handlePreviewSelected,
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
