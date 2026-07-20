import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as legalEntitiesApi from '@/api/legalEntities'
import { resolveStoreErrorMessageKey } from '@/api/http'
import {
  clearStoreListError,
  handleStoreListFailure,
  type AbortableRequestOptions,
} from '@/stores/storeRequestSupport'
import type {
  CreateLegalEntityPayload,
  DocumentBrandStatus,
  GroupDefaultLegalEntityView,
  LegalEntityView,
  PutGroupDefaultLegalEntityPayload,
  UpdateLegalEntityPayload,
} from '@/types/documentBrand'

export const useLegalEntitiesStore = defineStore('legalEntities', () => {
  const entities = ref<LegalEntityView[]>([])
  const groupCode = ref('')
  const statusFilter = ref<DocumentBrandStatus | ''>('')
  const defaultLegalEntityCode = ref<string | null>(null)
  const loadingList = ref(false)
  const loadingDefault = ref(false)
  const submitting = ref(false)
  const lastErrorMessageKey = ref<string | null>(null)
  const lastListErrorRetryable = ref(false)
  const lastMutationErrorMessageKey = ref<string | null>(null)

  async function fetchEntities(
    nextGroupCode: string,
    options: AbortableRequestOptions & { status?: DocumentBrandStatus | '' } = {},
  ): Promise<void> {
    loadingList.value = true
    clearStoreListError(lastErrorMessageKey, lastListErrorRetryable)
    const status = options.status !== undefined ? options.status : statusFilter.value
    try {
      const pageView = await legalEntitiesApi.listLegalEntities(nextGroupCode, {
        status: status || undefined,
        signal: options.signal,
      })
      entities.value = pageView.content
      groupCode.value = nextGroupCode
      if (options.status !== undefined) {
        statusFilter.value = options.status
      }
    } catch (error) {
      handleStoreListFailure(
        error,
        'legalEntities.error.loadList',
        lastErrorMessageKey,
        lastListErrorRetryable,
        { useStoreResolver: true },
      )
    } finally {
      loadingList.value = false
    }
  }

  async function fetchDefault(
    nextGroupCode: string,
    options: AbortableRequestOptions = {},
  ): Promise<void> {
    loadingDefault.value = true
    try {
      const view = await legalEntitiesApi.getGroupDefaultLegalEntity(nextGroupCode, options.signal)
      defaultLegalEntityCode.value = view.defaultLegalEntityCode
      groupCode.value = nextGroupCode
    } catch (error) {
      lastMutationErrorMessageKey.value = resolveStoreErrorMessageKey(
        error,
        'legalEntities.error.loadDefault',
      )
      throw error
    } finally {
      loadingDefault.value = false
    }
  }

  async function createEntity(payload: CreateLegalEntityPayload): Promise<LegalEntityView> {
    submitting.value = true
    lastMutationErrorMessageKey.value = null
    try {
      return await legalEntitiesApi.createLegalEntity(payload)
    } catch (error) {
      lastMutationErrorMessageKey.value = resolveStoreErrorMessageKey(
        error,
        'legalEntities.error.create',
      )
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function updateEntity(
    legalEntityCode: string,
    payload: UpdateLegalEntityPayload,
  ): Promise<LegalEntityView> {
    submitting.value = true
    lastMutationErrorMessageKey.value = null
    try {
      return await legalEntitiesApi.updateLegalEntity(legalEntityCode, payload)
    } catch (error) {
      lastMutationErrorMessageKey.value = resolveStoreErrorMessageKey(
        error,
        'legalEntities.error.update',
      )
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function putDefault(
    nextGroupCode: string,
    payload: PutGroupDefaultLegalEntityPayload,
  ): Promise<GroupDefaultLegalEntityView> {
    submitting.value = true
    lastMutationErrorMessageKey.value = null
    try {
      const view = await legalEntitiesApi.putGroupDefaultLegalEntity(nextGroupCode, payload)
      defaultLegalEntityCode.value = view.defaultLegalEntityCode
      return view
    } catch (error) {
      lastMutationErrorMessageKey.value = resolveStoreErrorMessageKey(
        error,
        'legalEntities.error.saveDefault',
      )
      throw error
    } finally {
      submitting.value = false
    }
  }

  return {
    entities,
    groupCode,
    statusFilter,
    defaultLegalEntityCode,
    loadingList,
    loadingDefault,
    submitting,
    lastErrorMessageKey,
    lastListErrorRetryable,
    lastMutationErrorMessageKey,
    fetchEntities,
    fetchDefault,
    createEntity,
    updateEntity,
    putDefault,
  }
})
