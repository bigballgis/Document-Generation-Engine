import { ref, type ComputedRef, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { templatePackageHubPath } from '@/routing/routeKeys'
import { useTemplatesStore } from '@/stores/templates'
import type { TemplateDetail } from '@/types/template'
import type { TemplateDetailTab } from '@/views/templates/templateDetailTabs'
import type { useTemplateLifecycleGates } from '@/views/templates/useTemplateLifecycleGates'
import { useTemplateLifecycleGovernance } from '@/views/templates/useTemplateLifecycleGovernance'

type LifecycleDecisionDialogMode =
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

  const { t } = useI18n()
  const router = useRouter()
  const templatesStore = useTemplatesStore()

  const lifecycleComment = ref('')
  const decisionDialogOpen = ref(false)
  const decisionDialogMode = ref<LifecycleDecisionDialogMode>('test-fail')
  const publishSummaryOpen = ref(false)
  const submitSummaryOpen = ref(false)
  const submitForTestDialogOpen = ref(false)
  const lifecycleCommentDialogOpen = ref(false)

  const { handleGovernanceAction, handleDeleteTemplate } = useTemplateLifecycleGovernance({
    templateId,
    errorMessage,
  })

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
          fidelityViewedConfirmed: payload.fidelityViewedConfirmed,
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

  async function confirmPublishFromSummary(payload?: { fidelityViewedConfirmed?: boolean }) {
    publishSummaryOpen.value = false
    try {
      await templatesStore.publishTemplate(templateId.value, {
        releaseVersion: gates.publishVersion.value,
        fidelityViewedConfirmed: payload?.fidelityViewedConfirmed,
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
