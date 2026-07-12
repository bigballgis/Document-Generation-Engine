import { computed, ref, type ComputedRef } from 'vue'
import { useI18n } from 'vue-i18n'
import { useCapabilities } from '@/composables/useCapabilities'
import { conflictsWithExisting, suggestNextVersions, type SemverBumpLevel } from '@/utils/semver'
import { resolveWorkflowBannerActionKind } from '@/utils/templateWorkflowBannerContext'
import {
  isPublishGateReady,
  isSubmitGateReady,
  mapPublishGateChecklistItems,
} from '@/utils/templateLifecycleDecisionForm'
import type { TemplateDetail } from '@/types/template'
import { useTemplateLifecycleGateData } from '@/views/templates/useTemplateLifecycleGateData'

export interface UseTemplateLifecycleGatesOptions {
  templateId: ComputedRef<string>
  template: ComputedRef<TemplateDetail | null>
}

export function useTemplateLifecycleGates(options: UseTemplateLifecycleGatesOptions) {
  const { templateId, template } = options

  const { t, te } = useI18n()
  const {
    authorTemplates,
    decideTests,
    decideApprovals,
    publishTemplates,
    stopTemplates,
    restoreOrDeprecateTemplates,
    deleteTemplates,
  } = useCapabilities()

  const publishBumpLevel = ref<SemverBumpLevel>('patch')
  const publishVersion = ref('1.0.0')

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

  const suggestedVersions = computed(() =>
    suggestNextVersions(template.value?.releaseVersion ?? null),
  )

  const gateData = useTemplateLifecycleGateData({
    templateId,
    suggestedVersions,
    publishBumpLevel,
    publishVersion,
    showPublishActions,
    showSubmitForApproval,
  })

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
    const apiItems = gateData.publishGateChecklist.value
      ? mapPublishGateChecklistItems(gateData.publishGateChecklist.value.items, resolvePublishGateItemLabel)
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

  const publishVersionConflict = computed(() =>
    conflictsWithExisting(publishVersion.value, gateData.publishedReleaseVersions.value),
  )

  const publishGateReady = computed(() =>
    isPublishGateReady({
      checklistReady: Boolean(gateData.publishGateChecklist.value?.ready),
      releaseVersion: publishVersion.value,
      versionConflict: publishVersionConflict.value,
    }),
  )

  const submitGateItems = computed(() => {
    if (!gateData.submitGateChecklist.value) {
      return []
    }
    return mapPublishGateChecklistItems(
      gateData.submitGateChecklist.value.items,
      resolvePublishGateItemLabel,
    )
  })

  const submitGateReady = computed(() =>
    isSubmitGateReady({
      checklistReady: Boolean(gateData.submitGateChecklist.value?.ready),
    }),
  )

  const authorJourneyPrimaryCtaDisabled = computed(
    () =>
      showSubmitForApproval.value &&
      (gateData.loadingSubmitGate.value ||
        !submitGateReady.value ||
        Boolean(gateData.submitGateLoadError.value)),
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

  return {
    publishBumpLevel,
    publishVersion,
    bindingGateResult: gateData.bindingGateResult,
    publishGateChecklist: gateData.publishGateChecklist,
    submitGateChecklist: gateData.submitGateChecklist,
    publishCoverageSummary: gateData.publishCoverageSummary,
    submitCoverageSummary: gateData.submitCoverageSummary,
    publishChangeDiffSummary: gateData.publishChangeDiffSummary,
    submitChangeDiffSummary: gateData.submitChangeDiffSummary,
    loadingPublishGate: gateData.loadingPublishGate,
    loadingSubmitGate: gateData.loadingSubmitGate,
    publishGateLoadError: gateData.publishGateLoadError,
    submitGateLoadError: gateData.submitGateLoadError,
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
    loadPublishGateData: gateData.loadPublishGateData,
    loadSubmitGateData: gateData.loadSubmitGateData,
    resetGateState: gateData.resetGateState,
  }
}
