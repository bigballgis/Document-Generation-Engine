import { ref, type ComputedRef, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useTemplatesStore } from '@/stores/templates'
import type { TemplateDetail } from '@/types/template'
import type { TemplateDetailTab } from '@/views/templates/templateDetailTabs'
import type { useTemplateLifecycleGates } from '@/views/templates/useTemplateLifecycleGates'
import { useTemplateLifecycleGovernance } from '@/views/templates/useTemplateLifecycleGovernance'
import { createTemplateLifecycleDecisionSubmitters } from '@/views/templates/createTemplateLifecycleDecisionSubmitters'

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

  const submitters = createTemplateLifecycleDecisionSubmitters({
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
    ...submitters,
    handleSubmitForApproval,
    handleApprovalDecision,
    handlePublish,
    handleGovernanceAction,
    handleDeleteTemplate,
    resetDecisionState,
  }
}
