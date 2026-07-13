import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as apiPolicyApi from '@/api/apiPolicy'
import { resolveApiError, resolveApiErrorMessageKey } from '@/api/http'
import type { ApiAccessAlert } from '@/types/template'
import {
  createEmptyApiPolicyEntry,
  type ApiPolicyEntry,
} from '@/stores/apiPolicyStoreTypes'
import { createApiPolicyMutationActions } from '@/stores/createApiPolicyMutationActions'

export type { ApiPolicyEntry } from '@/stores/apiPolicyStoreTypes'

export const useApiPolicyStore = defineStore('apiPolicy', () => {
  const entries = ref<Record<string, ApiPolicyEntry>>({})
  const activeTemplateId = ref<string | null>(null)
  const alerts = ref<ApiAccessAlert[]>([])
  const loadingAlerts = ref(false)
  const alertsErrorMessageKey = ref<string | null>(null)
  const alertsErrorRetryable = ref(false)

  function entryFor(templateId: string): ApiPolicyEntry {
    if (!entries.value[templateId]) {
      entries.value = { ...entries.value, [templateId]: createEmptyApiPolicyEntry() }
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

  const mutationActions = createApiPolicyMutationActions({
    entryFor,
    fetchCredentials,
  })

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
    ...mutationActions,
  }
})
