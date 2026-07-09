import { computed, type ComputedRef } from 'vue'
import { useCapabilities } from '@/composables/useCapabilities'
import { isTemplateExportEligible } from '@/utils/templateExportEligibility'
import type { TemplateDetail } from '@/types/template'

export interface UseTemplateDetailVisibilityOptions {
  isDevEditor: ComputedRef<boolean>
  template: ComputedRef<TemplateDetail | null>
}

export function useTemplateDetailVisibility(options: UseTemplateDetailVisibilityOptions) {
  const { isDevEditor, template } = options
  const { authorTemplates, decideTests, exportTemplates, editTemplateMetadata } = useCapabilities()

  const showMetadataEdit = computed(() => {
    const status = template.value?.lifecycleStatus
    if (!status || !editTemplateMetadata.value) {
      return false
    }
    return status !== 'PUBLISHED' && status !== 'STOPPED' && status !== 'DEPRECATED'
  })

  const showExportActions = computed(
    () =>
      exportTemplates.value &&
      Boolean(template.value) &&
      isTemplateExportEligible(template.value!.lifecycleStatus),
  )

  const showAuthoringSection = computed(() => {
    const status = template.value?.lifecycleStatus
    if (
      !status ||
      status === 'PUBLISHED' ||
      status === 'STOPPED' ||
      status === 'DEPRECATED'
    ) {
      return false
    }
    if (authorTemplates.value) {
      return true
    }
    return (
      isDevEditor.value &&
      decideTests.value &&
      (status === 'DRAFT' || status === 'TESTING')
    )
  })

  const canEditContentModuleReferences = computed(
    () => authorTemplates.value && template.value?.lifecycleStatus === 'DRAFT',
  )

  const showTestGenerate = computed(
    () =>
      authorTemplates.value &&
      (template.value?.lifecycleStatus === 'DRAFT' ||
        template.value?.lifecycleStatus === 'TESTING'),
  )

  return {
    showMetadataEdit,
    showExportActions,
    showAuthoringSection,
    canEditContentModuleReferences,
    showTestGenerate,
  }
}
