import { computed, ref, type ComputedRef } from 'vue'
import { useI18n } from 'vue-i18n'
import { useCapabilities } from '@/composables/useCapabilities'
import { conflictsWithExisting, suggestNextVersions, type SemverBumpLevel } from '@/utils/semver'
import {
  isPublishGateReady,
  isSubmitGateReady,
  mapPublishGateChecklistItems,
} from '@/utils/templateLifecycleDecisionForm'
import type { TemplateDetail } from '@/types/template'
import { useTemplateLifecycleGateData } from '@/views/templates/useTemplateLifecycleGateData'
import {
  createPublishBumpOptions,
  createTemplateLifecycleVisibility,
} from '@/views/templates/createTemplateLifecycleVisibility'

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

  const visibility = createTemplateLifecycleVisibility({
    template,
    authorTemplates,
    decideTests,
    decideApprovals,
    publishTemplates,
    stopTemplates,
    restoreOrDeprecateTemplates,
    deleteTemplates,
  })

  const suggestedVersions = computed(() =>
    suggestNextVersions(template.value?.releaseVersion ?? null),
  )

  const gateData = useTemplateLifecycleGateData({
    templateId,
    suggestedVersions,
    publishBumpLevel,
    publishVersion,
    showPublishActions: visibility.showPublishActions,
    showSubmitForApproval: visibility.showSubmitForApproval,
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
      visibility.showSubmitForApproval.value &&
      (gateData.loadingSubmitGate.value ||
        !submitGateReady.value ||
        Boolean(gateData.submitGateLoadError.value)),
  )

  const publishBumpOptions = createPublishBumpOptions({ t, suggestedVersions })

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
    ...visibility,
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
