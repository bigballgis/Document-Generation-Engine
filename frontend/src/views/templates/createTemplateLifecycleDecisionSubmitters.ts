import type { ComputedRef, Ref } from 'vue'
import type { Router } from 'vue-router'
import { ElMessage } from 'element-plus'
import { templatePackageHubPath } from '@/routing/routeKeys'
import type { useTemplatesStore } from '@/stores/templates'
import { deriveApprovalStage } from '@/utils/approvalMatrix'
import type { TemplateDetailTab } from '@/views/templates/templateDetailTabs'
import type { useTemplateLifecycleGates } from '@/views/templates/useTemplateLifecycleGates'

type LifecycleDecisionDialogMode =
  | 'test-fail'
  | 'test-pass'
  | 'approval-reject'
  | 'approval-approve'

export function createTemplateLifecycleDecisionSubmitters(deps: {
  t: (key: string) => string
  router: Router
  templatesStore: ReturnType<typeof useTemplatesStore>
  templateId: ComputedRef<string>
  errorMessage: ComputedRef<string>
  isDevEditor: ComputedRef<boolean>
  loadTemplate: () => Promise<void>
  activeDetailTab: Ref<TemplateDetailTab>
  gates: ReturnType<typeof useTemplateLifecycleGates>
  decisionDialogMode: Ref<LifecycleDecisionDialogMode>
  decisionDialogOpen: Ref<boolean>
  lifecycleComment: Ref<string>
  publishSummaryOpen: Ref<boolean>
  submitSummaryOpen: Ref<boolean>
}) {
  const {
    t,
    router,
    templatesStore,
    templateId,
    errorMessage,
    isDevEditor,
    loadTemplate,
    activeDetailTab,
    gates,
    decisionDialogMode,
    decisionDialogOpen,
    lifecycleComment,
    publishSummaryOpen,
    submitSummaryOpen,
  } = deps

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
        const approvalStage = deriveApprovalStage(
          templatesStore.selectedTemplate?.approvalSubState,
        )
        await templatesStore.recordApprovalDecision(templateId.value, {
          decision: 'REJECTED',
          reasonCategory: payload.reasonCategory,
          impactSummary: payload.impactSummary,
          commentSummary: payload.commentSummary,
          remediationTestRecordId: payload.remediationTestRecordId,
          remediationChangeDiffRef: payload.remediationChangeDiffRef,
          remediationChecklistCode: payload.remediationChecklistCode,
          ...(approvalStage ? { approvalStage } : {}),
        })
        ElMessage.success(t('templates.lifecycle.approvalDecisionSuccess'))
      } else if (mode === 'approval-approve') {
        const approvalStage = deriveApprovalStage(
          templatesStore.selectedTemplate?.approvalSubState,
        )
        await templatesStore.recordApprovalDecision(templateId.value, {
          decision: 'APPROVED',
          commentSummary: payload.commentSummary,
          fidelityViewedConfirmed: payload.fidelityViewedConfirmed,
          keyEvidenceConfirmed: payload.keyEvidenceConfirmed,
          exceptionIntervention: payload.exceptionIntervention,
          exceptionReason: payload.exceptionReason,
          secondaryConfirmed: payload.secondaryConfirmed,
          ...(approvalStage ? { approvalStage } : {}),
        })
        ElMessage.success(t('templates.lifecycle.approvalDecisionSuccess'))
      }
      decisionDialogOpen.value = false
      lifecycleComment.value = ''
    } catch {
      ElMessage.error(errorMessage.value || t('templates.error.lifecycle'))
    }
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
    submitLifecycleDecision,
    confirmSubmitFromSummary,
    confirmPublishFromSummary,
  }
}
