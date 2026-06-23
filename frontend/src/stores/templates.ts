import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as apiPolicyApi from '@/api/apiPolicy'
import * as templatesApi from '@/api/templates'
import { resolveApiErrorMessageKey } from '@/api/http'
import type {
  ApiCredentialCreated,
  ApiCredentialSummary,
  ApiPolicyImpactPreview,
  ApiPolicy,
  CompositionRuleInput,
  CreateTemplatePayload,
  DeleteTemplatePayload,
  LifecycleCommentPayload,
  LifecycleDecisionPayload,
  LifecycleGovernancePayload,
  LifecycleImpactPreviewRequest,
  PublishTemplatePayload,
  TemplateDetail,
  TemplateSummary,
  TestGeneratePayload,
  UpdateTemplateMetadataPayload,
  UpsertApiPolicyPayload,
  UpsertBindingPayload,
  UpsertVariablePayload,
} from '@/types/template'

export const useTemplatesStore = defineStore('templates', () => {
  const templates = ref<TemplateSummary[]>([])
  const selectedTemplate = ref<TemplateDetail | null>(null)
  const apiPolicy = ref<ApiPolicy | null>(null)
  const credentials = ref<ApiCredentialSummary[]>([])
  const lastCreatedCredential = ref<ApiCredentialCreated | null>(null)
  const lastRotatedCredential = ref<{ credentialId: string; externalId: string; secret: string } | null>(
    null,
  )
  const loadingList = ref(false)
  const loadingDetail = ref(false)
  const loadingPolicy = ref(false)
  const submitting = ref(false)
  const lastErrorMessageKey = ref<string | null>(null)

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

  async function fetchTemplates(): Promise<void> {
    loadingList.value = true
    lastErrorMessageKey.value = null
    try {
      templates.value = await templatesApi.listTemplates()
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'templates.error.loadList')
      throw error
    } finally {
      loadingList.value = false
    }
  }

  async function fetchTemplate(templateId: string): Promise<void> {
    loadingDetail.value = true
    lastErrorMessageKey.value = null
    try {
      selectedTemplate.value = await templatesApi.getTemplate(templateId)
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'templates.error.loadDetail')
      throw error
    } finally {
      loadingDetail.value = false
    }
  }

  async function fetchApiPolicy(templateId: string): Promise<void> {
    loadingPolicy.value = true
    lastErrorMessageKey.value = null
    try {
      apiPolicy.value = await apiPolicyApi.getApiPolicy(templateId)
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'templates.error.loadPolicy')
      throw error
    } finally {
      loadingPolicy.value = false
    }
  }

  async function fetchCredentials(templateId: string): Promise<void> {
    lastErrorMessageKey.value = null
    try {
      credentials.value = await apiPolicyApi.listCredentials(templateId)
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'templates.error.loadCredentials')
      throw error
    }
  }

  async function saveApiPolicy(
    templateId: string,
    payload: UpsertApiPolicyPayload,
  ): Promise<ApiPolicy> {
    submitting.value = true
    lastErrorMessageKey.value = null
    try {
      apiPolicy.value = await apiPolicyApi.upsertApiPolicy(templateId, payload)
      return apiPolicy.value
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'templates.error.savePolicy')
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function previewApiPolicyImpact(
    templateId: string,
    payload: UpsertApiPolicyPayload,
  ): Promise<ApiPolicyImpactPreview> {
    submitting.value = true
    lastErrorMessageKey.value = null
    try {
      return await apiPolicyApi.fetchApiPolicyImpactPreview(templateId, payload)
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'templates.error.previewPolicyImpact')
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function createCredential(templateId: string): Promise<ApiCredentialCreated> {
    submitting.value = true
    lastErrorMessageKey.value = null
    try {
      lastCreatedCredential.value = await apiPolicyApi.createCredential(templateId)
      lastRotatedCredential.value = null
      await fetchCredentials(templateId)
      return lastCreatedCredential.value
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'templates.error.createCredential')
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function rotateCredential(templateId: string, credentialId: string) {
    submitting.value = true
    lastErrorMessageKey.value = null
    try {
      const rotated = await apiPolicyApi.rotateCredential(templateId, credentialId)
      lastRotatedCredential.value = {
        credentialId: rotated.credentialId,
        externalId: rotated.externalId,
        secret: rotated.secret,
      }
      lastCreatedCredential.value = null
      await fetchCredentials(templateId)
      return rotated
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'templates.error.rotateCredential')
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function revokeCredential(templateId: string, credentialId: string) {
    submitting.value = true
    lastErrorMessageKey.value = null
    try {
      await apiPolicyApi.revokeCredential(templateId, credentialId)
      await fetchCredentials(templateId)
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'templates.error.revokeCredential')
      throw error
    } finally {
      submitting.value = false
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

  function applyUpdatedTemplate(updated: TemplateDetail) {
    selectedTemplate.value = updated
    templates.value = templates.value.map((item) =>
      item.id === updated.id ? toSummary(updated) : item,
    )
  }

  function toSummary(detail: TemplateDetail): TemplateSummary {
    return {
      id: detail.id,
      externalId: detail.externalId,
      groupCode: detail.groupCode,
      name: detail.name,
      lifecycleStatus: detail.lifecycleStatus,
      releaseVersion: detail.releaseVersion,
      masterId: detail.masterId,
      updatedAt: detail.updatedAt,
    }
  }

  function clearSelected() {
    selectedTemplate.value = null
    apiPolicy.value = null
    credentials.value = []
    lastCreatedCredential.value = null
    lastRotatedCredential.value = null
  }

  return {
    templates,
    selectedTemplate,
    apiPolicy,
    credentials,
    lastCreatedCredential,
    lastRotatedCredential,
    loadingList,
    loadingDetail,
    loadingPolicy,
    submitting,
    lastErrorMessageKey,
    publishedTemplates,
    templatesByGroup,
    fetchTemplates,
    fetchTemplate,
    fetchApiPolicy,
    fetchCredentials,
    saveApiPolicy,
    previewApiPolicyImpact,
    createCredential,
    rotateCredential,
    revokeCredential,
    createTemplate,
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
    clearSelected,
  }
})
