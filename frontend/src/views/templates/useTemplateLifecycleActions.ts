import { type ComputedRef, type Ref } from 'vue'
import type { TemplateDetail } from '@/types/template'
import type { TemplateDetailTab } from '@/views/templates/templateDetailTabs'
import { useTemplateLifecycleGates } from '@/views/templates/useTemplateLifecycleGates'
import {
  useTemplateLifecycleDecisions,
  type GovernanceAction,
  type LifecycleDecisionDialogMode,
} from '@/views/templates/useTemplateLifecycleDecisions'

export type { GovernanceAction, LifecycleDecisionDialogMode }

export interface UseTemplateLifecycleActionsOptions {
  templateId: ComputedRef<string>
  template: ComputedRef<TemplateDetail | null>
  isDevEditor: ComputedRef<boolean>
  errorMessage: ComputedRef<string>
  loadTemplate: () => Promise<void>
  activeDetailTab: Ref<TemplateDetailTab>
}

export function useTemplateLifecycleActions(options: UseTemplateLifecycleActionsOptions) {
  const gates = useTemplateLifecycleGates({
    templateId: options.templateId,
    template: options.template,
  })

  const decisions = useTemplateLifecycleDecisions({
    templateId: options.templateId,
    template: options.template,
    isDevEditor: options.isDevEditor,
    errorMessage: options.errorMessage,
    loadTemplate: options.loadTemplate,
    activeDetailTab: options.activeDetailTab,
    gates,
  })

  function resetLifecycleTransientState() {
    decisions.resetDecisionState()
  }

  return {
    // Gate state
    publishBumpLevel: gates.publishBumpLevel,
    publishVersion: gates.publishVersion,
    bindingGateResult: gates.bindingGateResult,
    publishGateChecklist: gates.publishGateChecklist,
    submitGateChecklist: gates.submitGateChecklist,
    publishCoverageSummary: gates.publishCoverageSummary,
    submitCoverageSummary: gates.submitCoverageSummary,
    publishChangeDiffSummary: gates.publishChangeDiffSummary,
    submitChangeDiffSummary: gates.submitChangeDiffSummary,
    loadingPublishGate: gates.loadingPublishGate,
    loadingSubmitGate: gates.loadingSubmitGate,
    publishGateLoadError: gates.publishGateLoadError,
    submitGateLoadError: gates.submitGateLoadError,
    // Visibility
    showLifecycleSection: gates.showLifecycleSection,
    showGovernanceSection: gates.showGovernanceSection,
    showDraftActions: gates.showDraftActions,
    showTestingDecisionActions: gates.showTestingDecisionActions,
    showSubmitForApproval: gates.showSubmitForApproval,
    showApprovalDecisionActions: gates.showApprovalDecisionActions,
    showPublishActions: gates.showPublishActions,
    showStopAction: gates.showStopAction,
    showRestoreAction: gates.showRestoreAction,
    showDeprecateAction: gates.showDeprecateAction,
    showDeleteTemplateAction: gates.showDeleteTemplateAction,
    publishGateItems: gates.publishGateItems,
    publishGateReady: gates.publishGateReady,
    publishBumpOptions: gates.publishBumpOptions,
    publishVersionConflict: gates.publishVersionConflict,
    submitGateItems: gates.submitGateItems,
    submitGateReady: gates.submitGateReady,
    authorJourneyPrimaryCtaDisabled: gates.authorJourneyPrimaryCtaDisabled,
    // Decision state
    lifecycleComment: decisions.lifecycleComment,
    lifecycleCommentDialogOpen: decisions.lifecycleCommentDialogOpen,
    decisionDialogOpen: decisions.decisionDialogOpen,
    decisionDialogMode: decisions.decisionDialogMode,
    publishSummaryOpen: decisions.publishSummaryOpen,
    submitSummaryOpen: decisions.submitSummaryOpen,
    submitForTestDialogOpen: decisions.submitForTestDialogOpen,
    // Handlers
    loadPublishGateData: gates.loadPublishGateData,
    loadSubmitGateData: gates.loadSubmitGateData,
    resetLifecycleTransientState,
    handleSubmitForTest: decisions.handleSubmitForTest,
    handleTestDecision: decisions.handleTestDecision,
    openApprovalRejectDialog: decisions.openApprovalRejectDialog,
    submitLifecycleDecision: decisions.submitLifecycleDecision,
    handleSubmitForApproval: decisions.handleSubmitForApproval,
    confirmSubmitFromSummary: decisions.confirmSubmitFromSummary,
    handleApprovalDecision: decisions.handleApprovalDecision,
    handlePublish: decisions.handlePublish,
    confirmPublishFromSummary: decisions.confirmPublishFromSummary,
    handleGovernanceAction: decisions.handleGovernanceAction,
    handleDeleteTemplate: decisions.handleDeleteTemplate,
  }
}
