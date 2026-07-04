import { computed, ref, watch, type ComputedRef, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useCapabilities } from '@/composables/useCapabilities'
import { useConfirmAction } from '@/composables/useConfirmAction'
import { ROUTE_PATH_BY_KEY, ROUTE_KEYS, templatePackageHubPath } from '@/routing/routeKeys'
import { useApiPolicyStore } from '@/stores/apiPolicy'
import { useTemplatesStore } from '@/stores/templates'
import * as templatesApi from '@/api/templates'
import { conflictsWithExisting, suggestNextVersions, type SemverBumpLevel } from '@/utils/semver'
import { resolveWorkflowBannerActionKind } from '@/utils/templateWorkflowBannerContext'
import {
  isPublishGateReady,
  isSubmitGateReady,
  mapPublishGateChecklistItems,
} from '@/utils/templateLifecycleDecisionForm'
import { resolvePublishGateLoadErrorKey } from '@/utils/templateBindingGateDisplay'
import type {
  BindingValidationResult,
  ChangeDiffSummary,
  CoverageSummary,
  DeleteTemplatePayload,
  LifecycleGovernanceAction,
  PublishGateChecklist,
  TemplateDetail,
} from '@/types/template'
import type { TemplateDetailTab } from '@/views/templates/templateDetailTabs'

export type GovernanceAction = 'stop' | 'restore' | 'deprecate'

export type LifecycleDecisionDialogMode =
  | 'test-fail'
  | 'test-pass'
  | 'approval-reject'
  | 'approval-approve'

export interface UseTemplateLifecycleActionsOptions {
  templateId: ComputedRef<string>
  template: ComputedRef<TemplateDetail | null>
  isDevEditor: ComputedRef<boolean>
  errorMessage: ComputedRef<string>
  loadTemplate: () => Promise<void>
  activeDetailTab: Ref<TemplateDetailTab>
}

export function useTemplateLifecycleActions(options: UseTemplateLifecycleActionsOptions) {
  const { templateId, template, isDevEditor, errorMessage, loadTemplate, activeDetailTab } = options

  const { t, te } = useI18n()
  const router = useRouter()
  const templatesStore = useTemplatesStore()
  const apiPolicyStore = useApiPolicyStore()
  const {
    authorTemplates,
    decideTests,
    decideApprovals,
    publishTemplates,
    stopTemplates,
    restoreOrDeprecateTemplates,
    deleteTemplates,
  } = useCapabilities()
  const { confirmAction } = useConfirmAction()

  const lifecycleComment = ref('')
  const decisionDialogOpen = ref(false)
  const decisionDialogMode = ref<LifecycleDecisionDialogMode>('test-fail')
  const publishBumpLevel = ref<SemverBumpLevel>('patch')
  const publishVersion = ref('1.0.0')
  const publishSummaryOpen = ref(false)
  const submitSummaryOpen = ref(false)
  const publishedReleaseVersions = ref<string[]>([])
  const submitForTestDialogOpen = ref(false)
  const lifecycleCommentDialogOpen = ref(false)
  const bindingGateResult = ref<BindingValidationResult | null>(null)
  const publishGateChecklist = ref<PublishGateChecklist | null>(null)
  const submitGateChecklist = ref<PublishGateChecklist | null>(null)
  const publishCoverageSummary = ref<CoverageSummary | null>(null)
  const submitCoverageSummary = ref<CoverageSummary | null>(null)
  const publishChangeDiffSummary = ref<ChangeDiffSummary | null>(null)
  const submitChangeDiffSummary = ref<ChangeDiffSummary | null>(null)
  const loadingPublishGate = ref(false)
  const loadingSubmitGate = ref(false)
  const publishGateLoadError = ref<string | null>(null)
  const submitGateLoadError = ref<string | null>(null)

  const approvalSubState = computed(() => template.value?.approvalSubState)

  const workflowBannerCapabilities = computed(() => ({
    authorTemplates: authorTemplates.value,
    decideTests: decideTests.value,
    decideApprovals: decideApprovals.value,
    publishTemplates: publishTemplates.value,
  }))

  const workflowBannerActionKind = computed(() => {
    const status = template.value?.lifecycleStatus
    if (!status) {
      return null
    }
    return resolveWorkflowBannerActionKind(
      status,
      workflowBannerCapabilities.value,
      template.value?.approvalSubState ?? null,
    )
  })

  const showDraftActions = computed(() => workflowBannerActionKind.value === 'draft')
  const showTestingDecisionActions = computed(() => workflowBannerActionKind.value === 'testing')
  const showSubmitForApproval = computed(() => {
    if (template.value?.lifecycleStatus !== 'APPROVAL' || !authorTemplates.value) {
      return false
    }
    if (approvalSubState.value === 'PENDING_DECISION') {
      return false
    }
    if (decideApprovals.value && !authorTemplates.value) {
      return false
    }
    return true
  })
  const showApprovalDecisionActions = computed(() => {
    if (template.value?.lifecycleStatus !== 'APPROVAL' || !decideApprovals.value) {
      return false
    }
    if (approvalSubState.value === 'PENDING_SUBMIT') {
      return false
    }
    return true
  })
  const showPublishActions = computed(() => workflowBannerActionKind.value === 'publish')
  const showStopAction = computed(
    () => template.value?.lifecycleStatus === 'PUBLISHED' && stopTemplates.value,
  )
  const showRestoreAction = computed(
    () => template.value?.lifecycleStatus === 'STOPPED' && restoreOrDeprecateTemplates.value,
  )
  const showDeprecateAction = computed(
    () => template.value?.lifecycleStatus === 'STOPPED' && restoreOrDeprecateTemplates.value,
  )
  const showGovernanceSection = computed(
    () => showStopAction.value || showRestoreAction.value || showDeprecateAction.value,
  )
  const showDeleteTemplateAction = computed(
    () => deleteTemplates.value && template.value?.lifecycleStatus !== 'DELETED',
  )

  function resolvePublishGateItemLabel(item: {
    checkCode: string
    messageKey: string
    summary: string
  }): string {
    if (te(item.messageKey)) {
      return t(item.messageKey)
    }
    const codeKey = `templates.publishGate.checkCodes.${item.checkCode}`
    if (te(codeKey)) {
      return t(codeKey)
    }
    return item.summary
  }

  const publishGateItems = computed(() => {
    const apiItems = publishGateChecklist.value
      ? mapPublishGateChecklistItems(publishGateChecklist.value.items, resolvePublishGateItemLabel)
      : []
    return [
      {
        key: 'releaseVersion',
        label: t('templates.publishGate.releaseVersionProvided'),
        ready: Boolean(publishVersion.value.trim()),
        informational: false,
      },
      ...apiItems,
    ]
  })

  const publishGateReady = computed(() =>
    isPublishGateReady({
      checklistReady: Boolean(publishGateChecklist.value?.ready),
      releaseVersion: publishVersion.value,
      versionConflict: publishVersionConflict.value,
    }),
  )

  const submitGateItems = computed(() => {
    if (!submitGateChecklist.value) {
      return []
    }
    return mapPublishGateChecklistItems(
      submitGateChecklist.value.items,
      resolvePublishGateItemLabel,
    )
  })

  const submitGateReady = computed(() =>
    isSubmitGateReady({
      checklistReady: Boolean(submitGateChecklist.value?.ready),
    }),
  )

  const authorJourneyPrimaryCtaDisabled = computed(
    () =>
      showSubmitForApproval.value &&
      (loadingSubmitGate.value || !submitGateReady.value || Boolean(submitGateLoadError.value)),
  )

  const suggestedVersions = computed(() =>
    suggestNextVersions(template.value?.releaseVersion ?? null),
  )

  const publishVersionConflict = computed(() =>
    conflictsWithExisting(publishVersion.value, publishedReleaseVersions.value),
  )

  const publishBumpOptions = computed(() => [
    {
      level: 'major' as SemverBumpLevel,
      label: t('templates.lifecycle.bumpMajor'),
      version: suggestedVersions.value.major,
    },
    {
      level: 'minor' as SemverBumpLevel,
      label: t('templates.lifecycle.bumpMinor'),
      version: suggestedVersions.value.minor,
    },
    {
      level: 'patch' as SemverBumpLevel,
      label: t('templates.lifecycle.bumpPatch'),
      version: suggestedVersions.value.patch,
    },
  ])

  const showLifecycleSection = computed(
    () =>
      showDraftActions.value ||
      showTestingDecisionActions.value ||
      showSubmitForApproval.value ||
      showApprovalDecisionActions.value ||
      showPublishActions.value ||
      (authorTemplates.value &&
        (template.value?.lifecycleStatus === 'DRAFT' ||
          template.value?.lifecycleStatus === 'TESTING')),
  )

  watch(
    showPublishActions,
    async (active) => {
      if (!active) {
        bindingGateResult.value = null
        publishGateChecklist.value = null
        publishCoverageSummary.value = null
        publishChangeDiffSummary.value = null
        publishedReleaseVersions.value = []
        publishGateLoadError.value = null
        return
      }
      publishBumpLevel.value = 'patch'
      publishVersion.value = suggestedVersions.value.patch
      await loadPublishGateData()
    },
    { immediate: true },
  )

  watch(
    showSubmitForApproval,
    async (active) => {
      if (!active) {
        submitGateChecklist.value = null
        submitCoverageSummary.value = null
        submitChangeDiffSummary.value = null
        submitGateLoadError.value = null
        return
      }
      await loadSubmitGateData()
    },
    { immediate: true },
  )

  watch(publishBumpLevel, (level) => {
    publishVersion.value = suggestedVersions.value[level]
  })

  watch(suggestedVersions, (versions) => {
    publishVersion.value = versions[publishBumpLevel.value]
  })

  async function loadSubmitGateData() {
    submitGateLoadError.value = null
    loadingSubmitGate.value = true
    try {
      const [checklist, coverage, changeDiff] = await Promise.all([
        templatesApi.fetchPublishGate(templateId.value, 'SUBMIT_FOR_APPROVAL'),
        templatesApi.getTemplateCoverage(templateId.value),
        templatesApi.fetchChangeDiff(templateId.value),
      ])
      submitGateChecklist.value = checklist
      submitCoverageSummary.value = coverage
      submitChangeDiffSummary.value = changeDiff
    } catch {
      submitGateLoadError.value = resolvePublishGateLoadErrorKey(templatesStore.lastErrorMessageKey)
      submitGateChecklist.value = null
      submitCoverageSummary.value = null
      submitChangeDiffSummary.value = null
    } finally {
      loadingSubmitGate.value = false
    }
  }

  async function loadPublishGateData() {
    publishGateLoadError.value = null
    loadingPublishGate.value = true
    try {
      apiPolicyStore.setActiveTemplate(templateId.value)
      await apiPolicyStore.fetchPolicy(templateId.value)
      const [bindings, checklist, coverage, changeDiff, versions] = await Promise.all([
        templatesStore.validateBindings(templateId.value),
        templatesApi.fetchPublishGate(templateId.value),
        templatesApi.getTemplateCoverage(templateId.value),
        templatesApi.fetchChangeDiff(templateId.value),
        templatesApi.fetchReleaseVersions(templateId.value),
      ])
      bindingGateResult.value = bindings
      publishGateChecklist.value = checklist
      publishCoverageSummary.value = coverage
      publishChangeDiffSummary.value = changeDiff
      publishedReleaseVersions.value = versions.map((entry) => entry.releaseVersion)
    } catch {
      publishGateLoadError.value = resolvePublishGateLoadErrorKey(
        apiPolicyStore.lastErrorMessageKey ?? templatesStore.lastErrorMessageKey,
      )
      bindingGateResult.value = null
      publishGateChecklist.value = null
      publishCoverageSummary.value = null
      publishChangeDiffSummary.value = null
      publishedReleaseVersions.value = []
    } finally {
      loadingPublishGate.value = false
    }
  }

  function resetLifecycleTransientState() {
    lifecycleComment.value = ''
    bindingGateResult.value = null
    publishGateChecklist.value = null
    submitGateChecklist.value = null
    publishCoverageSummary.value = null
    submitCoverageSummary.value = null
    publishChangeDiffSummary.value = null
    submitChangeDiffSummary.value = null
    publishedReleaseVersions.value = []
    loadingPublishGate.value = false
    loadingSubmitGate.value = false
    publishGateLoadError.value = null
    submitGateLoadError.value = null
  }

  async function handleSubmitForTest(comment = '') {
    try {
      await templatesStore.submitForTest(templateId.value, { commentSummary: comment })
      lifecycleComment.value = ''
      ElMessage.success(t('templates.lifecycle.submitTestSuccess'))
    } catch {
      ElMessage.error(errorMessage.value || t('templates.error.lifecycle'))
    }
  }

  async function handleTestDecision(decision: 'PASSED' | 'FAILED') {
    if (decision === 'FAILED') {
      decisionDialogMode.value = 'test-fail'
      decisionDialogOpen.value = true
      return
    }
    decisionDialogMode.value = 'test-pass'
    decisionDialogOpen.value = true
  }

  function openApprovalRejectDialog() {
    decisionDialogMode.value = 'approval-reject'
    decisionDialogOpen.value = true
  }

  async function submitLifecycleDecision(payload: {
    reasonCategory?: string
    impactSummary?: string
    commentSummary?: string
    remediationTestRecordId?: string
    remediationChangeDiffRef?: string
    remediationChecklistCode?: string
    fidelityViewedConfirmed?: boolean
    coverageViewedConfirmed?: boolean
    previewViewedConfirmed?: boolean
    exceptionIntervention?: boolean
    exceptionReason?: string
    secondaryConfirmed?: boolean
    keyEvidenceConfirmed?: boolean
  }) {
    const mode = decisionDialogMode.value
    try {
      if (mode === 'test-fail') {
        await templatesStore.recordTestDecision(templateId.value, {
          decision: 'FAILED',
          reasonCategory: payload.reasonCategory,
          impactSummary: payload.impactSummary,
          commentSummary: payload.commentSummary,
          remediationTestRecordId: payload.remediationTestRecordId,
          remediationChangeDiffRef: payload.remediationChangeDiffRef,
          remediationChecklistCode: payload.remediationChecklistCode,
        })
        ElMessage.success(t('templates.lifecycle.testDecisionSuccess'))
      } else if (mode === 'test-pass') {
        await templatesStore.recordTestDecision(templateId.value, {
          decision: 'PASSED',
          commentSummary: payload.commentSummary,
          fidelityViewedConfirmed: payload.fidelityViewedConfirmed,
          coverageViewedConfirmed: payload.coverageViewedConfirmed,
          previewViewedConfirmed: payload.previewViewedConfirmed,
          exceptionIntervention: payload.exceptionIntervention,
          exceptionReason: payload.exceptionReason,
          secondaryConfirmed: payload.secondaryConfirmed,
        })
        ElMessage.success(t('templates.lifecycle.testDecisionSuccess'))
      } else if (mode === 'approval-reject') {
        await templatesStore.recordApprovalDecision(templateId.value, {
          decision: 'REJECTED',
          reasonCategory: payload.reasonCategory,
          impactSummary: payload.impactSummary,
          commentSummary: payload.commentSummary,
          remediationTestRecordId: payload.remediationTestRecordId,
          remediationChangeDiffRef: payload.remediationChangeDiffRef,
          remediationChecklistCode: payload.remediationChecklistCode,
        })
        ElMessage.success(t('templates.lifecycle.approvalDecisionSuccess'))
      } else if (mode === 'approval-approve') {
        await templatesStore.recordApprovalDecision(templateId.value, {
          decision: 'APPROVED',
          commentSummary: payload.commentSummary,
          keyEvidenceConfirmed: payload.keyEvidenceConfirmed,
          exceptionIntervention: payload.exceptionIntervention,
          exceptionReason: payload.exceptionReason,
          secondaryConfirmed: payload.secondaryConfirmed,
        })
        ElMessage.success(t('templates.lifecycle.approvalDecisionSuccess'))
      }
      decisionDialogOpen.value = false
      lifecycleComment.value = ''
    } catch {
      ElMessage.error(errorMessage.value || t('templates.error.lifecycle'))
    }
  }

  async function handleSubmitForApproval() {
    if (submitGateLoadError.value) {
      ElMessage.error(t('templates.submitGate.loadError'))
      return
    }
    if (loadingSubmitGate.value) {
      return
    }
    if (!submitGateReady.value) {
      ElMessage.warning(t('templates.lifecycle.submitGateBlocked'))
      return
    }
    submitSummaryOpen.value = true
  }

  async function confirmSubmitFromSummary() {
    submitSummaryOpen.value = false
    try {
      await templatesStore.submitForApproval(templateId.value, {
        commentSummary: lifecycleComment.value,
      })
      lifecycleComment.value = ''
      ElMessage.success(t('templates.lifecycle.submitApprovalSuccess'))
    } catch {
      ElMessage.error(errorMessage.value || t('templates.error.lifecycle'))
    }
  }

  async function handleApprovalDecision(decision: 'APPROVED' | 'REJECTED') {
    if (decision === 'REJECTED') {
      openApprovalRejectDialog()
      return
    }
    decisionDialogMode.value = 'approval-approve'
    decisionDialogOpen.value = true
  }

  async function handlePublish() {
    if (!publishGateReady.value) {
      return
    }
    publishSummaryOpen.value = true
  }

  async function confirmPublishFromSummary() {
    publishSummaryOpen.value = false
    try {
      await templatesStore.publishTemplate(templateId.value, {
        releaseVersion: publishVersion.value,
      })
      await loadTemplate()
      if (isDevEditor.value) {
        router.push(templatePackageHubPath(templateId.value))
      } else {
        activeDetailTab.value = 'releaseVersions'
      }
      ElMessage.success(t('templates.lifecycle.publishSuccess'))
    } catch {
      ElMessage.error(errorMessage.value || t('templates.error.lifecycle'))
    }
  }

  const governanceActionConfig = {
    stop: {
      previewAction: 'STOP' as LifecycleGovernanceAction,
      titleKey: 'templates.lifecycle.stopTitle',
      reasonKey: 'templates.lifecycle.stopReasonPrompt',
      confirmTitleKey: 'templates.lifecycle.confirmStopTitle',
      confirmMessageKey: 'templates.lifecycle.confirmStopMessage',
      successKey: 'templates.lifecycle.stopSuccess',
    },
    restore: {
      previewAction: 'RESTORE' as LifecycleGovernanceAction,
      titleKey: 'templates.lifecycle.restoreTitle',
      reasonKey: 'templates.lifecycle.restoreReasonPrompt',
      confirmTitleKey: 'templates.lifecycle.confirmRestoreTitle',
      confirmMessageKey: 'templates.lifecycle.confirmRestoreMessage',
      successKey: 'templates.lifecycle.restoreSuccess',
    },
    deprecate: {
      previewAction: 'DEPRECATE' as LifecycleGovernanceAction,
      titleKey: 'templates.lifecycle.deprecateTitle',
      reasonKey: 'templates.lifecycle.deprecateReasonPrompt',
      confirmTitleKey: 'templates.lifecycle.confirmDeprecateTitle',
      confirmMessageKey: 'templates.lifecycle.confirmDeprecateMessage',
      successKey: 'templates.lifecycle.deprecateSuccess',
    },
  } as const

  async function buildImpactPreviewMessage(
    action: LifecycleGovernanceAction,
    releaseVersion?: string,
  ): Promise<string> {
    const preview = await templatesStore.fetchLifecycleImpactPreview(templateId.value, {
      action,
      releaseVersion,
    })
    const summary = te(preview.summaryMessageKey)
      ? t(preview.summaryMessageKey)
      : t(`templates.governance.impactSummary.${action}`)
    const callable = preview.callableReleaseVersions.length
      ? t('templates.governance.impactCallableVersions', {
          versions: preview.callableReleaseVersions.join(', '),
        })
      : t('templates.governance.impactNoCallableVersions')
    const defaultRoute = preview.defaultRouteReleaseVersion
      ? t('templates.governance.impactDefaultRoute', {
          version: preview.defaultRouteReleaseVersion,
        })
      : ''
    const routeImpact = preview.defaultRouteImpacted
      ? t('templates.governance.impactDefaultRouteAffected')
      : ''
    return [summary, callable, defaultRoute, routeImpact, t('templates.governance.impactConfirmPrompt')]
      .filter(Boolean)
      .join('\n\n')
  }

  async function handleGovernanceAction(action: GovernanceAction) {
    const config = governanceActionConfig[action]
    let reason = ''
    try {
      const result = await ElMessageBox.prompt(t(config.reasonKey), t(config.titleKey), {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        inputValidator: (value) =>
          value.trim().length > 0 ? true : t('templates.lifecycle.reasonRequired'),
      })
      reason = result.value.trim()
    } catch {
      return
    }

    try {
      const impactMessage = await buildImpactPreviewMessage(config.previewAction)
      const confirmBody = [impactMessage, t(config.confirmMessageKey)].join('\n\n')
      await ElMessageBox.confirm(confirmBody, t(config.confirmTitleKey), {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        type: 'warning',
      })
    } catch {
      return
    }

    const payload = { reason, confirmed: true }
    try {
      if (action === 'stop') {
        await templatesStore.stopTemplate(templateId.value, payload)
      } else if (action === 'restore') {
        await templatesStore.restoreTemplate(templateId.value, payload)
      } else {
        await templatesStore.deprecateTemplate(templateId.value, payload)
      }
      ElMessage.success(t(config.successKey))
    } catch {
      ElMessage.error(errorMessage.value || t('templates.error.lifecycle'))
    }
  }

  async function handleDeleteTemplate() {
    let reason = ''
    try {
      const result = await ElMessageBox.prompt(
        t('templates.deleteAction.reasonPrompt'),
        t('templates.deleteAction.title'),
        {
          confirmButtonText: t('common.confirm'),
          cancelButtonText: t('common.cancel'),
          inputValidator: (value) =>
            value.trim().length > 0 ? true : t('templates.deleteAction.reasonRequired'),
        },
      )
      reason = result.value.trim()
    } catch {
      return
    }

    const confirmed = await confirmAction({
      titleKey: 'templates.deleteAction.confirmTitle',
      messageKey: 'templates.deleteAction.confirmMessage',
      type: 'warning',
    })
    if (!confirmed) {
      return
    }

    try {
      const payload: DeleteTemplatePayload = { reason }
      await templatesStore.deleteTemplate(templateId.value, payload)
      ElMessage.success(t('templates.deleteAction.success'))
      router.push(ROUTE_PATH_BY_KEY[ROUTE_KEYS.templateManagement])
    } catch {
      ElMessage.error(errorMessage.value || t('templates.error.delete'))
    }
  }

  return {
    lifecycleComment,
    lifecycleCommentDialogOpen,
    decisionDialogOpen,
    decisionDialogMode,
    publishSummaryOpen,
    submitSummaryOpen,
    submitForTestDialogOpen,
    publishBumpLevel,
    publishVersion,
    bindingGateResult,
    publishGateChecklist,
    submitGateChecklist,
    publishCoverageSummary,
    submitCoverageSummary,
    publishChangeDiffSummary,
    submitChangeDiffSummary,
    loadingPublishGate,
    loadingSubmitGate,
    publishGateLoadError,
    submitGateLoadError,
    showLifecycleSection,
    showGovernanceSection,
    showDraftActions,
    showTestingDecisionActions,
    showSubmitForApproval,
    showApprovalDecisionActions,
    showPublishActions,
    showStopAction,
    showRestoreAction,
    showDeprecateAction,
    showDeleteTemplateAction,
    publishGateItems,
    publishGateReady,
    publishBumpOptions,
    publishVersionConflict,
    submitGateItems,
    submitGateReady,
    authorJourneyPrimaryCtaDisabled,
    loadPublishGateData,
    loadSubmitGateData,
    resetLifecycleTransientState,
    handleSubmitForTest,
    handleTestDecision,
    openApprovalRejectDialog,
    submitLifecycleDecision,
    handleSubmitForApproval,
    confirmSubmitFromSummary,
    handleApprovalDecision,
    handlePublish,
    confirmPublishFromSummary,
    handleGovernanceAction,
    handleDeleteTemplate,
  }
}
