import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as templatesApi from '@/api/templates'
import { resolveApiErrorMessageKey } from '@/api/http'
import {
  clearStoreListError,
  handleStoreListFailure,
  type AbortableRequestOptions,
} from '@/stores/storeRequestSupport'
import { createTemplatesAuthoringActions } from '@/stores/createTemplatesAuthoringActions'
import { createTemplatesLifecycleActions } from '@/stores/createTemplatesLifecycleActions'
import {
  applyUpdatedTemplate,
  toTemplateSummary,
} from '@/stores/templatesStoreHelpers'

/**
 * Options for {@link fetchTemplates}. Mirrors the underlying {@link listTemplates} API options so
 * the store can forward `signal` plus catalog query params (search/groupCode/lifecycleStatus/sort)
 * without narrowing the type below what callers (e.g. command palette) rely on.
 */
export type TemplateListFetchOptions = AbortableRequestOptions & {
  search?: string
  groupCode?: string
  lifecycleStatus?: string
  approvalSubState?: string
  sort?: string
}
import type {
  CreateTemplatePayload,
  DeleteTemplatePayload,
  ImportTemplatePayload,
  MasterStyleCatalog,
  TemplateDetail,
  TemplateImportResult,
  TemplateSummary,
  TestGeneratePayload,
  UpdateTemplateMetadataPayload,
} from '@/types/template'
import { useApiPolicyStore } from '@/stores/apiPolicy'
import { useTemplatePanelDataStore } from '@/stores/templatePanelData'

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

  async function fetchTemplates(
    page = templateListPage.value,
    size = templateListSize.value,
    options: TemplateListFetchOptions = {},
  ): Promise<void> {
    loadingList.value = true
    clearStoreListError(lastErrorMessageKey, lastListErrorRetryable)
    try {
      const pageView = await templatesApi.listTemplates(page, size, options)
      templates.value = pageView.content
      templateListPage.value = pageView.page
      templateListSize.value = pageView.size
      templateListTotalElements.value = pageView.totalElements
      templateListTotalPages.value = pageView.totalPages
    } catch (error) {
      handleStoreListFailure(error, 'templates.error.loadList', lastErrorMessageKey, lastListErrorRetryable)
    } finally {
      loadingList.value = false
    }
  }

  /** Full catalog merge for dashboard consumers (not list-view paging). */
  async function fetchAllTemplates(options: TemplateListFetchOptions = {}): Promise<void> {
    loadingList.value = true
    clearStoreListError(lastErrorMessageKey, lastListErrorRetryable)
    try {
      const collected = await templatesApi.listAllTemplates(options)
      templates.value = collected.content
      templateListPage.value = 0
      templateListSize.value = collected.content.length || templateListSize.value
      templateListTotalElements.value = collected.totalElements
      templateListTotalPages.value =
        collected.totalElements === 0
          ? 0
          : Math.max(1, Math.ceil(collected.totalElements / 100))
    } catch (error) {
      handleStoreListFailure(error, 'templates.error.loadList', lastErrorMessageKey, lastListErrorRetryable)
    } finally {
      loadingList.value = false
    }
  }

  async function fetchTemplate(templateId: string): Promise<void> {
    // Soft-refresh: when the same template is already selected, do not flip
    // loadingDetail. Otherwise TemplateDetailView swaps to skeleton and tears
    // down the workspace mid-save (BDD-LRP-C2-002 clear-on-save never runs).
    const softRefresh = selectedTemplate.value?.id === templateId
    if (!softRefresh) {
      loadingDetail.value = true
    }
    lastErrorMessageKey.value = null
    try {
      selectedTemplate.value = await templatesApi.getTemplate(templateId)
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'templates.error.loadDetail')
      throw error
    } finally {
      if (!softRefresh) {
        loadingDetail.value = false
      }
    }
  }

  async function createTemplate(payload: CreateTemplatePayload): Promise<TemplateDetail> {
    submitting.value = true
    lastErrorMessageKey.value = null
    try {
      const created = await templatesApi.createTemplate(payload)
      templates.value = [toTemplateSummary(created, templates.value), ...templates.value]
      selectedTemplate.value = created
      return created
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'templates.error.create')
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function importTemplate(payload: ImportTemplatePayload): Promise<TemplateImportResult> {
    submitting.value = true
    lastErrorMessageKey.value = null
    try {
      const result = await templatesApi.importTemplate(payload)
      templates.value = [
        toTemplateSummary(result.template, templates.value),
        ...templates.value.filter((item) => item.id !== result.template.id),
      ]
      selectedTemplate.value = result.template
      return result
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'templates.error.import')
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function deleteTemplate(templateId: string, payload: DeleteTemplatePayload): Promise<void> {
    submitting.value = true
    lastErrorMessageKey.value = null
    try {
      await templatesApi.deleteTemplate(templateId, payload)
      templates.value = templates.value.filter((item) => item.id !== templateId)
      if (selectedTemplate.value?.id === templateId) {
        selectedTemplate.value = null
      }
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'templates.error.delete')
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function updateTemplateMetadata(
    templateId: string,
    payload: UpdateTemplateMetadataPayload,
  ): Promise<TemplateDetail> {
    submitting.value = true
    lastErrorMessageKey.value = null
    try {
      const updated = await templatesApi.updateTemplateMetadata(templateId, payload)
      applyUpdatedTemplate(selectedTemplate, templates, updated)
      return updated
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'templates.error.updateMetadata')
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function testGenerate(templateId: string, payload: TestGeneratePayload = {}) {
    submitting.value = true
    lastErrorMessageKey.value = null
    try {
      return await templatesApi.testGenerate(templateId, payload)
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'templates.error.testGenerate')
      throw error
    } finally {
      submitting.value = false
    }
  }

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
    fetchTemplate,
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
    fetchTemplates,
    fetchAllTemplates,
    fetchTemplate,
    createTemplate,
    importTemplate,
    deleteTemplate,
    updateTemplateMetadata,
    testGenerate,
    ...lifecycleActions,
    ...authoringActions,
    clearSelected,
  }
})
