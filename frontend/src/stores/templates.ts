import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as apiPolicyApi from '@/api/apiPolicy'
import * as templatesApi from '@/api/templates'
import { isApiError } from '@/api/http'
import type {
  ApiCredentialCreated,
  ApiCredentialSummary,
  ApiPolicy,
  LifecycleCommentPayload,
  LifecycleDecisionPayload,
  PublishTemplatePayload,
  TemplateDetail,
  TemplateSummary,
  TestGeneratePayload,
  UpsertApiPolicyPayload,
} from '@/types/template'

export const useTemplatesStore = defineStore('templates', () => {
  const templates = ref<TemplateSummary[]>([])
  const selectedTemplate = ref<TemplateDetail | null>(null)
  const apiPolicy = ref<ApiPolicy | null>(null)
  const credentials = ref<ApiCredentialSummary[]>([])
  const lastCreatedCredential = ref<ApiCredentialCreated | null>(null)
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

  function resolveErrorMessageKey(error: unknown, fallbackKey: string): string {
    if (isApiError(error) && error.response?.data.error?.messageKey) {
      return error.response.data.error.messageKey
    }
    return fallbackKey
  }

  async function fetchTemplates(): Promise<void> {
    loadingList.value = true
    lastErrorMessageKey.value = null
    try {
      templates.value = await templatesApi.listTemplates()
    } catch (error) {
      lastErrorMessageKey.value = resolveErrorMessageKey(error, 'templates.error.loadList')
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
      lastErrorMessageKey.value = resolveErrorMessageKey(error, 'templates.error.loadDetail')
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
      lastErrorMessageKey.value = resolveErrorMessageKey(error, 'templates.error.loadPolicy')
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
      lastErrorMessageKey.value = resolveErrorMessageKey(error, 'templates.error.loadCredentials')
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
      lastErrorMessageKey.value = resolveErrorMessageKey(error, 'templates.error.savePolicy')
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
      await fetchCredentials(templateId)
      return lastCreatedCredential.value
    } catch (error) {
      lastErrorMessageKey.value = resolveErrorMessageKey(error, 'templates.error.createCredential')
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

  async function testGenerate(templateId: string, payload: TestGeneratePayload = {}) {
    submitting.value = true
    lastErrorMessageKey.value = null
    try {
      return await templatesApi.testGenerate(templateId, payload)
    } catch (error) {
      lastErrorMessageKey.value = resolveErrorMessageKey(error, 'templates.error.testGenerate')
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function runLifecycleAction(action: () => Promise<TemplateDetail>) {
    submitting.value = true
    lastErrorMessageKey.value = null
    try {
      const updated = await action()
      applyUpdatedTemplate(updated)
      return updated
    } catch (error) {
      lastErrorMessageKey.value = resolveErrorMessageKey(error, 'templates.error.lifecycle')
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
  }

  return {
    templates,
    selectedTemplate,
    apiPolicy,
    credentials,
    lastCreatedCredential,
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
    createCredential,
    submitForTest,
    recordTestDecision,
    submitForApproval,
    recordApprovalDecision,
    publishTemplate,
    testGenerate,
    clearSelected,
  }
})
