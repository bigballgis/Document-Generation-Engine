import { computed, type ComputedRef } from 'vue'
import type { SemverBumpLevel } from '@/utils/semver'
import { resolveWorkflowBannerActionKind } from '@/utils/templateWorkflowBannerContext'
import type { TemplateDetail } from '@/types/template'

export function createTemplateLifecycleVisibility(deps: {
  template: ComputedRef<TemplateDetail | null>
  authorTemplates: ComputedRef<boolean>
  decideTests: ComputedRef<boolean>
  decideApprovals: ComputedRef<boolean>
  publishTemplates: ComputedRef<boolean>
  stopTemplates: ComputedRef<boolean>
  restoreOrDeprecateTemplates: ComputedRef<boolean>
  deleteTemplates: ComputedRef<boolean>
}) {
  const {
    template,
    authorTemplates,
    decideTests,
    decideApprovals,
    publishTemplates,
    stopTemplates,
    restoreOrDeprecateTemplates,
    deleteTemplates,
  } = deps

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

  return {
    showDraftActions,
    showTestingDecisionActions,
    showSubmitForApproval,
    showApprovalDecisionActions,
    showPublishActions,
    showStopAction,
    showRestoreAction,
    showDeprecateAction,
    showGovernanceSection,
    showDeleteTemplateAction,
    showLifecycleSection,
  }
}

export function createPublishBumpOptions(deps: {
  t: (key: string) => string
  suggestedVersions: ComputedRef<{ major: string; minor: string; patch: string }>
}) {
  return computed(() => [
    {
      level: 'major' as SemverBumpLevel,
      label: deps.t('templates.lifecycle.bumpMajor'),
      version: deps.suggestedVersions.value.major,
    },
    {
      level: 'minor' as SemverBumpLevel,
      label: deps.t('templates.lifecycle.bumpMinor'),
      version: deps.suggestedVersions.value.minor,
    },
    {
      level: 'patch' as SemverBumpLevel,
      label: deps.t('templates.lifecycle.bumpPatch'),
      version: deps.suggestedVersions.value.patch,
    },
  ])
}
