import { onMounted, ref, watch, type Ref } from 'vue'
import {
  fetchContentModuleLocaleVariantSiblings,
  fetchTemplateLocaleVariantSiblings,
  type LocaleVariantSibling,
} from '@/api/localeVariantFamily'

type TemplateFamilySource = {
  id: string
  groupCode: string
  localeVariantFamilyId?: string | null
}

type ContentModuleFamilySource = {
  moduleId: string
  groupCode: string
  localeVariantFamilyId?: string | null
}

export function useTemplateLocaleVariantSiblings(
  template: Ref<TemplateFamilySource | null | undefined>,
) {
  const siblings = ref<LocaleVariantSibling[]>([])
  const loading = ref(false)

  async function reload() {
    const current = template.value
    if (!current?.localeVariantFamilyId) {
      siblings.value = []
      return
    }
    loading.value = true
    try {
      siblings.value = await fetchTemplateLocaleVariantSiblings({
        templateId: current.id,
        groupCode: current.groupCode,
        localeVariantFamilyId: current.localeVariantFamilyId,
      })
    } catch {
      siblings.value = []
    } finally {
      loading.value = false
    }
  }

  watch(
    () => [
      template.value?.id,
      template.value?.groupCode,
      template.value?.localeVariantFamilyId,
    ],
    () => {
      void reload()
    },
  )

  onMounted(() => {
    void reload()
  })

  return { siblings, loading, reload }
}

export function useContentModuleLocaleVariantSiblings(
  detail: Ref<ContentModuleFamilySource | null | undefined>,
) {
  const siblings = ref<LocaleVariantSibling[]>([])
  const loading = ref(false)

  async function reload() {
    const current = detail.value
    if (!current?.localeVariantFamilyId) {
      siblings.value = []
      return
    }
    loading.value = true
    try {
      siblings.value = await fetchContentModuleLocaleVariantSiblings({
        moduleId: current.moduleId,
        groupCode: current.groupCode,
        localeVariantFamilyId: current.localeVariantFamilyId,
      })
    } catch {
      siblings.value = []
    } finally {
      loading.value = false
    }
  }

  watch(
    () => [
      detail.value?.moduleId,
      detail.value?.groupCode,
      detail.value?.localeVariantFamilyId,
    ],
    () => {
      void reload()
    },
  )

  onMounted(() => {
    void reload()
  })

  return { siblings, loading, reload }
}
