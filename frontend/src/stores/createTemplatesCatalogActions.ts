import type { Ref } from 'vue'
import * as templatesApi from '@/api/templates'
import { resolveApiErrorMessageKey } from '@/api/http'
import {
  clearStoreListError,
  handleStoreListFailure,
} from '@/stores/storeRequestSupport'
import {
  applyUpdatedTemplate,
  toTemplateSummary,
} from '@/stores/templatesStoreHelpers'
import type {
  CreateTemplatePayload,
  DeleteTemplatePayload,
  ImportTemplatePayload,
  TemplateDetail,
  TemplateImportDryRunResult,
  TemplateImportResult,
  TemplateSummary,
  TestGeneratePayload,
  UpdateTemplateMetadataPayload,
} from '@/types/template'
import type { TemplateListFetchOptions } from '@/stores/templateListFetchOptions'

export function createTemplatesCatalogActions(deps: {
  templates: Ref<TemplateSummary[]>
  templateListPage: Ref<number>
  templateListSize: Ref<number>
  templateListTotalElements: Ref<number>
  templateListTotalPages: Ref<number>
  selectedTemplate: Ref<TemplateDetail | null>
  loadingList: Ref<boolean>
  loadingDetail: Ref<boolean>
  submitting: Ref<boolean>
  lastErrorMessageKey: Ref<string | null>
  lastListErrorRetryable: Ref<boolean>
}) {
  const {
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
  } = deps

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

  async function dryRunImportTemplate(
    payload: ImportTemplatePayload,
  ): Promise<TemplateImportDryRunResult> {
    submitting.value = true
    lastErrorMessageKey.value = null
    try {
      return await templatesApi.importTemplate({ ...payload, dryRun: true })
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'templates.error.import')
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function importTemplate(payload: ImportTemplatePayload): Promise<TemplateImportResult> {
    submitting.value = true
    lastErrorMessageKey.value = null
    try {
      const result = await templatesApi.importTemplate({ ...payload, dryRun: false })
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

  return {
    fetchTemplates,
    fetchAllTemplates,
    fetchTemplate,
    createTemplate,
    dryRunImportTemplate,
    importTemplate,
    deleteTemplate,
    updateTemplateMetadata,
    testGenerate,
  }
}
