import { computed, ref, watch, type ComputedRef } from 'vue'
import { useI18n } from 'vue-i18n'
import { useCapabilities } from '@/composables/useCapabilities'
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
  PublishGateChecklist,
  TemplateDetail,
} from '@/types/template'

export interface UseTemplateLifecycleGatesOptions {
  templateId: ComputedRef<string>
  template: ComputedRef<TemplateDetail | null>
}

export function useTemplateLifecycleGates(options: UseTemplateLifecycleGatesOptions) {
  const { templateId, template } = options

  const { t, te } = useI18n()
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

  const publishBumpLevel = ref<SemverBumpLevel>('patch')
  const publishVersion = ref('1.0.0')
  const publishedReleaseVersions = ref<string[]>([])
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

  const suggestedVersions = computed(() =>
    suggestNextVersions(template.value?.releaseVersion ?? null),
  )

  const publishVersionConflict = computed(() =>
    conflictsWithExisting(publishVersion.value, publishedReleaseVersions.value),
  )

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

  function resetGateState() {
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

  return {
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
    resetGateState,
  }
}
