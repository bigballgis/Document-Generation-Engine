import type { Ref } from 'vue'
import * as templatesApi from '@/api/templates'
import { resolveApiErrorMessageKey } from '@/api/http'
import type {
  CompositionRuleInput,
  MasterStyleCatalog,
  PasteCleanResult,
  TemplateDetail,
  UpsertBindingPayload,
  UpsertVariablePayload,
} from '@/types/template'

export function createTemplatesAuthoringActions(deps: {
  submitting: Ref<boolean>
  lastErrorMessageKey: Ref<string | null>
  selectedTemplate: Ref<TemplateDetail | null>
  masterStyleCatalogByTemplateId: Ref<Record<string, MasterStyleCatalog>>
  fetchTemplate: (templateId: string) => Promise<void>
}) {
  const {
    submitting,
    lastErrorMessageKey,
    selectedTemplate,
    masterStyleCatalogByTemplateId,
    fetchTemplate,
  } = deps

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

  async function validateBindings(templateId: string) {
    return templatesApi.validateBindings(templateId)
  }

  async function validateRules(templateId: string, rules: CompositionRuleInput[]) {
    return templatesApi.validateRules(templateId, rules)
  }

  return {
    upsertVariable,
    deleteVariable,
    upsertBinding,
    saveRules,
    fetchMasterStyleCatalog,
    pasteClean,
    validateBindings,
    validateRules,
  }
}
