import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { createTemplatesAuthoringActions } from '@/stores/createTemplatesAuthoringActions'
import { createTemplatesLifecycleActions } from '@/stores/createTemplatesLifecycleActions'
import { createTemplatesCatalogActions } from '@/stores/createTemplatesCatalogActions'
import type {
  MasterStyleCatalog,
  TemplateDetail,
  TemplateSummary,
} from '@/types/template'
import { useApiPolicyStore } from '@/stores/apiPolicy'
import { useTemplatePanelDataStore } from '@/stores/templatePanelData'

export type { TemplateListFetchOptions } from '@/stores/templateListFetchOptions'

export const useTemplatesStore = defineStore('templates', () => {
  const templates = ref<TemplateSummary[]>([])
  const templateListPage = ref(0)
  const templateListSize = ref(20)
  const templateListTotalElements = ref(0)
  const templateListTotalPages = ref(0)
  const selectedTemplate = ref<TemplateDetail | null>(null)
  const loadingList = ref(false)
  const loadingDetail = ref(false)
  const submitting = ref(false)
  const lastErrorMessageKey = ref<string | null>(null)
  const lastListErrorRetryable = ref(false)
  const masterStyleCatalogByTemplateId = ref<Record<string, MasterStyleCatalog>>({})

  const publishedTemplates = computed(() =>
    templates.value.filter((item) => item.lifecycleStatus === 'PUBLISHED'),
  )

  const templatesByGroup = computed(() => {
    const grouped = new Map<string, TemplateSummary[]>()
    for (const template of templates.value) {
      const existing = grouped.get(template.groupCode) ?? []
      existing.push(template)
      grouped.set(template.groupCode, existing)
    }
    return grouped
  })

  const catalogActions = createTemplatesCatalogActions({
    templates,
    templateListPage,
    templateListSize,
    templateListTotalElements,
    templateListTotalPages,
    selectedTemplate,
    loadingList,
    loadingDetail,
    submitting,
    lastErrorMessageKey,
    lastListErrorRetryable,
  })

  const lifecycleActions = createTemplatesLifecycleActions({
    submitting,
    lastErrorMessageKey,
    selectedTemplate,
    templates,
  })

  const authoringActions = createTemplatesAuthoringActions({
    submitting,
    lastErrorMessageKey,
    selectedTemplate,
    masterStyleCatalogByTemplateId,
    fetchTemplate: catalogActions.fetchTemplate,
  })

  function clearSelected(templateId?: string) {
    selectedTemplate.value = null
    const apiPolicyStore = useApiPolicyStore()
    const panelDataStore = useTemplatePanelDataStore()
    if (templateId) {
      apiPolicyStore.clearTemplate(templateId)
      panelDataStore.clearTemplate(templateId)
      const nextCatalogs = { ...masterStyleCatalogByTemplateId.value }
      delete nextCatalogs[templateId]
      masterStyleCatalogByTemplateId.value = nextCatalogs
    } else if (apiPolicyStore.activeTemplateId) {
      const activeId = apiPolicyStore.activeTemplateId
      apiPolicyStore.clearTemplate(activeId)
      panelDataStore.clearTemplate(activeId)
      const nextCatalogs = { ...masterStyleCatalogByTemplateId.value }
      delete nextCatalogs[activeId]
      masterStyleCatalogByTemplateId.value = nextCatalogs
    }
  }

  return {
    templates,
    templateListPage,
    templateListSize,
    templateListTotalElements,
    templateListTotalPages,
    selectedTemplate,
    loadingList,
    loadingDetail,
    submitting,
    lastErrorMessageKey,
    lastListErrorRetryable,
    publishedTemplates,
    templatesByGroup,
    ...catalogActions,
    ...lifecycleActions,
    ...authoringActions,
    clearSelected,
  }
})
