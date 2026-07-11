import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as templatesApi from '@/api/templates'
import { resolveApiErrorMessageKey } from '@/api/http'
import {
  clearStoreListError,
  handleStoreListFailure,
  type AbortableRequestOptions,
} from '@/stores/storeRequestSupport'

/**
 * Options for {@link fetchTemplates}. Mirrors the underlying {@link listTemplates} API options so
 * the store can forward `signal` plus catalog query params (search/groupCode/lifecycleStatus/sort)
 * without narrowing the type below what callers (e.g. command palette) rely on.
 */
export type TemplateListFetchOptions = AbortableRequestOptions & {
  search?: string
  groupCode?: string
  lifecycleStatus?: string
  sort?: string
}
import type {
  CompositionRuleInput,
  CreateTemplatePayload,
  DeleteTemplatePayload,
  ImportTemplatePayload,
  LifecycleCommentPayload,
  LifecycleDecisionPayload,
  LifecycleGovernancePayload,
  LifecycleImpactPreviewRequest,
  MasterStyleCatalog,
  PasteCleanResult,
  PublishTemplatePayload,
  TemplateDetail,
  TemplateImportResult,
  TemplateSummary,
  TestGeneratePayload,
  UpdateTemplateMetadataPayload,
  UpsertBindingPayload,
  UpsertVariablePayload,
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
      templates.value = [toSummary(created), ...templates.value]
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
      templates.value = [toSummary(result.template), ...templates.value.filter((item) => item.id !== result.template.id)]
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

  async function submitForTest(templateId: string, payload: LifecycleCommentPayload) {
    return runLifecycleAction(() => templatesApi.submitForTest(templateId, payload))
  }

  async function recordTestDecision(templateId: string, payload: LifecycleDecisionPayload) {
    return runLifecycleAction(() => templatesApi.recordTestDecision(templateId, payload))
  }

  async function submitForApproval(templateId: string, payload: LifecycleCommentPayload) {
    return runLifecycleAction(() => templatesApi.submitForApproval(templateId, payload))
  }

  async function recordApprovalDecision(templateId: string, payload: LifecycleDecisionPayload) {
    return runLifecycleAction(() => templatesApi.recordApprovalDecision(templateId, payload))
  }

  async function publishTemplate(templateId: string, payload: PublishTemplatePayload) {
    return runLifecycleAction(() => templatesApi.publishTemplate(templateId, payload))
  }

  async function stopTemplate(templateId: string, payload: LifecycleGovernancePayload) {
    return runLifecycleAction(() => templatesApi.stopTemplate(templateId, payload))
  }

  async function restoreTemplate(templateId: string, payload: LifecycleGovernancePayload) {
    return runLifecycleAction(() => templatesApi.restoreTemplate(templateId, payload))
  }

  async function deprecateTemplate(templateId: string, payload: LifecycleGovernancePayload) {
    return runLifecycleAction(() => templatesApi.deprecateTemplate(templateId, payload))
  }

  async function fetchLifecycleImpactPreview(
    templateId: string,
    payload: LifecycleImpactPreviewRequest,
  ) {
    lastErrorMessageKey.value = null
    try {
      return await templatesApi.fetchLifecycleImpactPreview(templateId, payload)
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'templates.error.lifecycle')
      throw error
    }
  }

  async function deactivateTemplateVersion(
    templateId: string,
    releaseVersion: string,
    payload: LifecycleGovernancePayload,
  ) {
    return runLifecycleAction(() =>
      templatesApi.deactivateTemplateVersion(templateId, releaseVersion, payload),
    )
  }

  async function restoreTemplateVersion(
    templateId: string,
    releaseVersion: string,
    payload: LifecycleGovernancePayload,
  ) {
    return runLifecycleAction(() =>
      templatesApi.restoreTemplateVersion(templateId, releaseVersion, payload),
    )
  }

  async function updateTemplateMetadata(
    templateId: string,
    payload: UpdateTemplateMetadataPayload,
  ): Promise<TemplateDetail> {
    submitting.value = true
    lastErrorMessageKey.value = null
    try {
      const updated = await templatesApi.updateTemplateMetadata(templateId, payload)
      applyUpdatedTemplate(updated)
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

  async function validateBindings(templateId: string) {
    return templatesApi.validateBindings(templateId)
  }

  async function validateRules(templateId: string, rules: CompositionRuleInput[]) {
    return templatesApi.validateRules(templateId, rules)
  }

  async function runLifecycleAction(action: () => Promise<TemplateDetail>) {
    submitting.value = true
    lastErrorMessageKey.value = null
    try {
      const updated = await action()
      applyUpdatedTemplate(updated)
      return updated
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'templates.error.lifecycle')
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function upsertVariable(
    templateId: string,
    variableKey: string,
    payload: UpsertVariablePayload,
  ): Promise<TemplateDetail> {
    submitting.value = true
    lastErrorMessageKey.value = null
    try {
      await templatesApi.upsertVariable(templateId, variableKey, payload)
      await fetchTemplate(templateId)
      return selectedTemplate.value!
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'templates.error.saveVariable')
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function deleteVariable(templateId: string, variableKey: string): Promise<TemplateDetail> {
    submitting.value = true
    lastErrorMessageKey.value = null
    try {
      await templatesApi.deleteVariable(templateId, variableKey)
      await fetchTemplate(templateId)
      return selectedTemplate.value!
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'templates.error.deleteVariable')
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function upsertBinding(
    templateId: string,
    anchorId: string,
    payload: UpsertBindingPayload,
  ): Promise<TemplateDetail> {
    submitting.value = true
    lastErrorMessageKey.value = null
    try {
      await templatesApi.upsertBinding(templateId, anchorId, payload)
      await fetchTemplate(templateId)
      return selectedTemplate.value!
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'templates.error.saveBinding')
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function saveRules(templateId: string, rules: CompositionRuleInput[]): Promise<TemplateDetail> {
    submitting.value = true
    lastErrorMessageKey.value = null
    try {
      await templatesApi.saveRules(templateId, rules)
      await fetchTemplate(templateId)
      return selectedTemplate.value!
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'templates.error.saveRules')
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function fetchMasterStyleCatalog(templateId: string): Promise<MasterStyleCatalog> {
    const cached = masterStyleCatalogByTemplateId.value[templateId]
    if (cached) {
      return cached
    }
    const catalog = await templatesApi.getMasterStyleCatalog(templateId)
    masterStyleCatalogByTemplateId.value = {
      ...masterStyleCatalogByTemplateId.value,
      [templateId]: catalog,
    }
    return catalog
  }

  async function pasteClean(
    templateId: string,
    payload: { sourceHtml: string; prePasteStructuredContentJson: string },
  ): Promise<PasteCleanResult> {
    return templatesApi.pasteClean(templateId, payload)
  }

  function applyUpdatedTemplate(updated: TemplateDetail) {
    selectedTemplate.value = updated
    templates.value = templates.value.map((item) =>
      item.id === updated.id ? toSummary(updated) : item,
    )
  }

  function toSummary(detail: TemplateDetail): TemplateSummary {
    const existing = templates.value.find((item) => item.id === detail.id)
    const releaseVersionCount =
      existing?.releaseVersionCount ?? (detail.releaseVersion ? 1 : 0)
    return {
      id: detail.id,
      externalId: detail.externalId,
      groupCode: detail.groupCode,
      name: detail.name,
      lifecycleStatus: detail.lifecycleStatus,
      approvalSubState: detail.approvalSubState,
      releaseVersion: detail.releaseVersion,
      releaseVersionCount,
      masterId: detail.masterId,
      updatedBy: existing?.updatedBy ?? '',
      updatedAt: detail.updatedAt,
    }
  }

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
    fetchTemplate,
    createTemplate,
    importTemplate,
    deleteTemplate,
    submitForTest,
    recordTestDecision,
    submitForApproval,
    recordApprovalDecision,
    publishTemplate,
    stopTemplate,
    restoreTemplate,
    deprecateTemplate,
    fetchLifecycleImpactPreview,
    deactivateTemplateVersion,
    restoreTemplateVersion,
    updateTemplateMetadata,
    testGenerate,
    validateBindings,
    validateRules,
    upsertVariable,
    deleteVariable,
    upsertBinding,
    saveRules,
    fetchMasterStyleCatalog,
    pasteClean,
    clearSelected,
  }
})
