import { ref, watch, type ComputedRef, type Ref } from 'vue'
import { useApiPolicyStore } from '@/stores/apiPolicy'
import { useTemplatesStore } from '@/stores/templates'
import * as templatesApi from '@/api/templates'
import type { SemverBumpLevel } from '@/utils/semver'
import { resolvePublishGateLoadErrorKey } from '@/utils/templateBindingGateDisplay'
import type {
  BindingValidationResult,
  ChangeDiffSummary,
  CoverageSummary,
  PublishGateChecklist,
} from '@/types/template'

export interface UseTemplateLifecycleGateDataOptions {
  templateId: ComputedRef<string>
  suggestedVersions: ComputedRef<{ major: string; minor: string; patch: string }>
  publishBumpLevel: Ref<SemverBumpLevel>
  publishVersion: Ref<string>
  showPublishActions: ComputedRef<boolean>
  showSubmitForApproval: ComputedRef<boolean>
}

export function useTemplateLifecycleGateData(options: UseTemplateLifecycleGateDataOptions) {
  const {
    templateId,
    suggestedVersions,
    publishBumpLevel,
    publishVersion,
    showPublishActions,
    showSubmitForApproval,
  } = options

  const templatesStore = useTemplatesStore()
  const apiPolicyStore = useApiPolicyStore()

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
    publishedReleaseVersions,
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
    loadPublishGateData,
    loadSubmitGateData,
    resetGateState,
  }
}
