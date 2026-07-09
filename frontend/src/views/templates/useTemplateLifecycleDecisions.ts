import { ref, type ComputedRef, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useConfirmAction } from '@/composables/useConfirmAction'
import { ROUTE_PATH_BY_KEY, ROUTE_KEYS, templatePackageHubPath } from '@/routing/routeKeys'
import { useTemplatesStore } from '@/stores/templates'
import type { DeleteTemplatePayload, LifecycleGovernanceAction, TemplateDetail } from '@/types/template'
import type { TemplateDetailTab } from '@/views/templates/templateDetailTabs'
import type { useTemplateLifecycleGates } from '@/views/templates/useTemplateLifecycleGates'

export type GovernanceAction = 'stop' | 'restore' | 'deprecate'

export type LifecycleDecisionDialogMode =
  | 'test-fail'
  | 'test-pass'
  | 'approval-reject'
  | 'approval-approve'

export interface UseTemplateLifecycleDecisionsOptions {
  templateId: ComputedRef<string>
  template: ComputedRef<TemplateDetail | null>
  isDevEditor: ComputedRef<boolean>
  errorMessage: ComputedRef<string>
  loadTemplate: () => Promise<void>
  activeDetailTab: Ref<TemplateDetailTab>
  gates: ReturnType<typeof useTemplateLifecycleGates>
}

export function useTemplateLifecycleDecisions(options: UseTemplateLifecycleDecisionsOptions) {
  const { templateId, isDevEditor, errorMessage, loadTemplate, activeDetailTab, gates } = options

  const { t, te } = useI18n()
  const router = useRouter()
  const templatesStore = useTemplatesStore()
  const { confirmAction } = useConfirmAction()

  const lifecycleComment = ref('')
  const decisionDialogOpen = ref(false)
  const decisionDialogMode = ref<LifecycleDecisionDialogMode>('test-fail')
  const publishSummaryOpen = ref(false)
  const submitSummaryOpen = ref(false)
  const submitForTestDialogOpen = ref(false)
  const lifecycleCommentDialogOpen = ref(false)

  function resetDecisionState() {
    lifecycleComment.value = ''
    gates.resetGateState()
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
    if (gates.submitGateLoadError.value) {
      ElMessage.error(t('templates.submitGate.loadError'))
      return
    }
    if (gates.loadingSubmitGate.value) {
      return
    }
    if (!gates.submitGateReady.value) {
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
    if (!gates.publishGateReady.value) {
      return
    }
    publishSummaryOpen.value = true
  }

  async function confirmPublishFromSummary() {
    publishSummaryOpen.value = false
    try {
      await templatesStore.publishTemplate(templateId.value, {
        releaseVersion: gates.publishVersion.value,
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
    resetDecisionState,
  }
}
