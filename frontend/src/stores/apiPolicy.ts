import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as apiPolicyApi from '@/api/apiPolicy'
import { resolveApiError, resolveApiErrorMessageKey } from '@/api/http'
import type {
  ApiAccessAlert,
  ApiCredentialCreated,
  ApiCredentialSummary,
  ApiPolicyImpactPreview,
  ApiPolicy,
  UpsertApiPolicyPayload,
} from '@/types/template'
import type { ApiPolicyDomain, ApiPolicyDomainFormMap, InvocationRetentionDomainForm } from '@/types/apiPolicyDomain'

export interface ApiPolicyRotatedCredential {
  credentialId: string
  externalId: string
  secret: string
}

export interface ApiPolicyEntry {
  policy: ApiPolicy | null
  credentials: ApiCredentialSummary[]
  lastCreatedCredential: ApiCredentialCreated | null
  lastRotatedCredential: ApiPolicyRotatedCredential | null
  loadingPolicy: boolean
  submitting: boolean
  lastErrorMessageKey: string | null
}

function createEmptyEntry(): ApiPolicyEntry {
  return {
    policy: null,
    credentials: [],
    lastCreatedCredential: null,
    lastRotatedCredential: null,
    loadingPolicy: false,
    submitting: false,
    lastErrorMessageKey: null,
  }
}

export const useApiPolicyStore = defineStore('apiPolicy', () => {
  const entries = ref<Record<string, ApiPolicyEntry>>({})
  const activeTemplateId = ref<string | null>(null)
  const alerts = ref<ApiAccessAlert[]>([])
  const loadingAlerts = ref(false)
  const alertsErrorMessageKey = ref<string | null>(null)
  const alertsErrorRetryable = ref(false)

  function entryFor(templateId: string): ApiPolicyEntry {
    if (!entries.value[templateId]) {
      entries.value = { ...entries.value, [templateId]: createEmptyEntry() }
    }
    return entries.value[templateId]!
  }

  function setActiveTemplate(templateId: string) {
    activeTemplateId.value = templateId
    entryFor(templateId)
  }

  function clearTemplate(templateId: string) {
    const next = { ...entries.value }
    delete next[templateId]
    entries.value = next
    if (activeTemplateId.value === templateId) {
      activeTemplateId.value = null
    }
  }

  const apiPolicy = computed(() => {
    const id = activeTemplateId.value
    return id ? (entries.value[id]?.policy ?? null) : null
  })

  const credentials = computed(() => {
    const id = activeTemplateId.value
    return id ? (entries.value[id]?.credentials ?? []) : []
  })

  const loadingPolicy = computed(() => {
    const id = activeTemplateId.value
    return id ? (entries.value[id]?.loadingPolicy ?? false) : false
  })

  const submitting = computed(() => {
    const id = activeTemplateId.value
    return id ? (entries.value[id]?.submitting ?? false) : false
  })

  const lastErrorMessageKey = computed(() => {
    const id = activeTemplateId.value
    return id ? (entries.value[id]?.lastErrorMessageKey ?? null) : null
  })

  const lastCreatedCredential = computed(() => {
    const id = activeTemplateId.value
    return id ? (entries.value[id]?.lastCreatedCredential ?? null) : null
  })

  const lastRotatedCredential = computed(() => {
    const id = activeTemplateId.value
    return id ? (entries.value[id]?.lastRotatedCredential ?? null) : null
  })

  async function fetchPolicy(templateId: string): Promise<void> {
    const entry = entryFor(templateId)
    entry.loadingPolicy = true
    entry.lastErrorMessageKey = null
    try {
      entry.policy = await apiPolicyApi.getApiPolicy(templateId)
    } catch (error) {
      entry.lastErrorMessageKey = resolveApiErrorMessageKey(error, 'templates.error.loadPolicy')
      throw error
    } finally {
      entry.loadingPolicy = false
    }
  }

  async function fetchCredentials(templateId: string): Promise<void> {
    const entry = entryFor(templateId)
    entry.lastErrorMessageKey = null
    try {
      entry.credentials = await apiPolicyApi.listCredentials(templateId)
    } catch (error) {
      entry.lastErrorMessageKey = resolveApiErrorMessageKey(error, 'templates.error.loadCredentials')
      throw error
    }
  }

  async function fetchAlerts(): Promise<void> {
    loadingAlerts.value = true
    alertsErrorMessageKey.value = null
    alertsErrorRetryable.value = false
    try {
      alerts.value = await apiPolicyApi.fetchAlerts()
    } catch (error) {
      alerts.value = []
      alertsErrorMessageKey.value = resolveApiErrorMessageKey(error, 'apiPolicy.home.alerts.loadFailed')
      alertsErrorRetryable.value = resolveApiError(error)?.error.retryable ?? false
      throw error
    } finally {
      loadingAlerts.value = false
    }
  }

  async function savePolicy(
    templateId: string,
    payload: UpsertApiPolicyPayload,
  ): Promise<ApiPolicy> {
    const entry = entryFor(templateId)
    entry.submitting = true
    entry.lastErrorMessageKey = null
    try {
      entry.policy = await apiPolicyApi.upsertApiPolicy(templateId, payload)
      return entry.policy
    } catch (error) {
      entry.lastErrorMessageKey = resolveApiErrorMessageKey(error, 'templates.error.savePolicy')
      throw error
    } finally {
      entry.submitting = false
    }
  }

  async function previewImpact(
    templateId: string,
    payload: UpsertApiPolicyPayload,
  ): Promise<ApiPolicyImpactPreview> {
    const entry = entryFor(templateId)
    entry.submitting = true
    entry.lastErrorMessageKey = null
    try {
      return await apiPolicyApi.fetchApiPolicyImpactPreview(templateId, payload)
    } catch (error) {
      entry.lastErrorMessageKey = resolveApiErrorMessageKey(error, 'templates.error.previewPolicyImpact')
      throw error
    } finally {
      entry.submitting = false
    }
  }

  async function savePolicyDomain<D extends ApiPolicyDomain>(
    templateId: string,
    domain: D,
    payload: ApiPolicyDomainFormMap[D],
    confirmed = true,
  ): Promise<ApiPolicy> {
    const entry = entryFor(templateId)
    entry.submitting = true
    entry.lastErrorMessageKey = null
    try {
      entry.policy = await apiPolicyApi.saveApiPolicyDomain(templateId, domain, payload, confirmed)
      return entry.policy
    } catch (error) {
      entry.lastErrorMessageKey = resolveApiErrorMessageKey(error, 'templates.error.savePolicy')
      throw error
    } finally {
      entry.submitting = false
    }
  }

  async function saveInvocationRetentionDomain(
    templateId: string,
    payload: InvocationRetentionDomainForm,
    confirmed = true,
  ): Promise<ApiPolicy> {
    const entry = entryFor(templateId)
    entry.submitting = true
    entry.lastErrorMessageKey = null
    try {
      entry.policy = await apiPolicyApi.saveInvocationRetentionDomain(templateId, payload, confirmed)
      return entry.policy
    } catch (error) {
      entry.lastErrorMessageKey = resolveApiErrorMessageKey(error, 'templates.error.savePolicy')
      throw error
    } finally {
      entry.submitting = false
    }
  }

  async function createCredential(templateId: string): Promise<ApiCredentialCreated> {
    const entry = entryFor(templateId)
    entry.submitting = true
    entry.lastErrorMessageKey = null
    try {
      entry.lastCreatedCredential = await apiPolicyApi.createCredential(templateId)
      entry.lastRotatedCredential = null
      await fetchCredentials(templateId)
      return entry.lastCreatedCredential
    } catch (error) {
      entry.lastErrorMessageKey = resolveApiErrorMessageKey(error, 'templates.error.createCredential')
      throw error
    } finally {
      entry.submitting = false
    }
  }

  async function rotateCredential(templateId: string, credentialId: string) {
    const entry = entryFor(templateId)
    entry.submitting = true
    entry.lastErrorMessageKey = null
    try {
      const rotated = await apiPolicyApi.rotateCredential(templateId, credentialId)
      entry.lastRotatedCredential = {
        credentialId: rotated.credentialId,
        externalId: rotated.externalId,
        secret: rotated.secret,
      }
      entry.lastCreatedCredential = null
      await fetchCredentials(templateId)
      return rotated
    } catch (error) {
      entry.lastErrorMessageKey = resolveApiErrorMessageKey(error, 'templates.error.rotateCredential')
      throw error
    } finally {
      entry.submitting = false
    }
  }

  async function revokeCredential(templateId: string, credentialId: string) {
    const entry = entryFor(templateId)
    entry.submitting = true
    entry.lastErrorMessageKey = null
    try {
      await apiPolicyApi.revokeCredential(templateId, credentialId)
      await fetchCredentials(templateId)
    } catch (error) {
      entry.lastErrorMessageKey = resolveApiErrorMessageKey(error, 'templates.error.revokeCredential')
      throw error
    } finally {
      entry.submitting = false
    }
  }

  return {
    entries,
    activeTemplateId,
    alerts,
    loadingAlerts,
    alertsErrorMessageKey,
    alertsErrorRetryable,
    apiPolicy,
    credentials,
    loadingPolicy,
    submitting,
    lastErrorMessageKey,
    lastCreatedCredential,
    lastRotatedCredential,
    setActiveTemplate,
    clearTemplate,
    entryFor,
    fetchPolicy,
    fetchCredentials,
    fetchAlerts,
    savePolicy,
    previewImpact,
    savePolicyDomain,
    saveInvocationRetentionDomain,
    createCredential,
    rotateCredential,
    revokeCredential,
  }
})
