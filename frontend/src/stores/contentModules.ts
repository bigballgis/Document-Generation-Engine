import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as contentModulesApi from '@/api/contentModules'
import { resolveApiErrorMessageKey, resolveStoreErrorMessageKey } from '@/api/http'
import {
  clearStoreListError,
  handleStoreListFailure,
  type AbortableRequestOptions,
} from '@/stores/storeRequestSupport'
import {
  applyUpdatedContentModule,
  toContentModuleSummary,
} from '@/stores/contentModuleStoreHelpers'
import type {
  ContentModuleDetail,
  ContentModuleLifecycleImpactSummary,
  ContentModuleLifecycleOperationPayload,
  ContentModuleReviewTransitionPayload,
  ContentModuleSummary,
  CreateContentModulePayload,
  CreateContentModuleVersionPayload,
  UpdateContentModuleVersionPayload,
} from '@/types/contentModule'

export const useContentModulesStore = defineStore('contentModules', () => {
  const modules = ref<ContentModuleSummary[]>([])
  const moduleListPage = ref(0)
  const moduleListSize = ref(20)
  const moduleListTotalElements = ref(0)
  const moduleListTotalPages = ref(0)
  const selectedModule = ref<ContentModuleDetail | null>(null)
  const lifecycleImpactPreview = ref<ContentModuleLifecycleImpactSummary | null>(null)
  const loadingList = ref(false)
  const loadingDetail = ref(false)
  const loadingImpactPreview = ref(false)
  const submitting = ref(false)
  const lastErrorMessageKey = ref<string | null>(null)
  const lastListErrorRetryable = ref(false)
  const activeGroupCode = ref('')

  async function fetchModules(
    page = moduleListPage.value,
    size = moduleListSize.value,
    options: AbortableRequestOptions & {
      search?: string
      groupCode?: string
      sort?: string
    } = {},
  ): Promise<void> {
    loadingList.value = true
    clearStoreListError(lastErrorMessageKey, lastListErrorRetryable)
    activeGroupCode.value = options.groupCode?.trim() ?? ''
    try {
      const pageView = await contentModulesApi.listContentModules(page, size, options)
      modules.value = pageView.content
      moduleListPage.value = pageView.page
      moduleListSize.value = pageView.size
      moduleListTotalElements.value = pageView.totalElements
      moduleListTotalPages.value = pageView.totalPages
    } catch (error) {
      handleStoreListFailure(
        error,
        'contentModules.error.loadList',
        lastErrorMessageKey,
        lastListErrorRetryable,
        { useStoreResolver: true },
      )
    } finally {
      loadingList.value = false
    }
  }

  async function fetchModule(moduleId: string): Promise<void> {
    loadingDetail.value = true
    lastErrorMessageKey.value = null
    try {
      selectedModule.value = await contentModulesApi.getContentModule(moduleId)
    } catch (error) {
      lastErrorMessageKey.value = resolveStoreErrorMessageKey(
        error,
        'contentModules.error.loadDetail',
      )
      throw error
    } finally {
      loadingDetail.value = false
    }
  }

  async function createModule(payload: CreateContentModulePayload): Promise<ContentModuleDetail> {
    submitting.value = true
    lastErrorMessageKey.value = null
    try {
      const created = await contentModulesApi.createContentModule(payload)
      applyUpdatedContentModule(selectedModule, modules, created)
      if (!activeGroupCode.value || activeGroupCode.value === payload.groupCode) {
        modules.value = [toContentModuleSummary(created), ...modules.value.filter((item) => item.moduleId !== created.moduleId)]
      }
      return created
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'contentModules.error.create')
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function createVersion(
    moduleId: string,
    payload: CreateContentModuleVersionPayload,
  ): Promise<ContentModuleDetail> {
    submitting.value = true
    lastErrorMessageKey.value = null
    try {
      const updated = await contentModulesApi.createContentModuleVersion(moduleId, payload)
      applyUpdatedContentModule(selectedModule, modules, updated)
      return updated
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'contentModules.error.createVersion')
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function updateDraftVersion(
    moduleId: string,
    semanticVersion: string,
    payload: UpdateContentModuleVersionPayload,
  ): Promise<ContentModuleDetail> {
    submitting.value = true
    lastErrorMessageKey.value = null
    try {
      const updated = await contentModulesApi.updateContentModuleDraftVersion(
        moduleId,
        semanticVersion,
        payload,
      )
      applyUpdatedContentModule(selectedModule, modules, updated)
      return updated
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'contentModules.error.updateVersion')
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function transitionReview(
    moduleId: string,
    payload: ContentModuleReviewTransitionPayload,
  ): Promise<void> {
    submitting.value = true
    lastErrorMessageKey.value = null
    try {
      await contentModulesApi.transitionContentModuleReview(moduleId, payload)
      await fetchModule(moduleId)
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'contentModules.error.reviewTransition')
      throw error
    } finally {
      submitting.value = false
    }
  }

  async function fetchLifecycleImpactPreview(moduleId: string): Promise<ContentModuleLifecycleImpactSummary> {
    loadingImpactPreview.value = true
    lastErrorMessageKey.value = null
    try {
      lifecycleImpactPreview.value = await contentModulesApi.previewContentModuleLifecycleImpact(moduleId)
      return lifecycleImpactPreview.value
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'contentModules.error.loadImpactPreview')
      throw error
    } finally {
      loadingImpactPreview.value = false
    }
  }

  async function applyLifecycleOperation(
    moduleId: string,
    payload: ContentModuleLifecycleOperationPayload,
  ): Promise<void> {
    submitting.value = true
    lastErrorMessageKey.value = null
    try {
      await contentModulesApi.applyContentModuleLifecycleOperation(moduleId, payload)
      await fetchModule(moduleId)
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(error, 'contentModules.error.lifecycle')
      throw error
    } finally {
      submitting.value = false
    }
  }

  function clearSelected() {
    selectedModule.value = null
    lifecycleImpactPreview.value = null
  }

  function clearListError() {
    clearStoreListError(lastErrorMessageKey, lastListErrorRetryable)
  }

  return {
    modules,
    moduleListPage,
    moduleListSize,
    moduleListTotalElements,
    moduleListTotalPages,
    selectedModule,
    lifecycleImpactPreview,
    loadingList,
    loadingDetail,
    loadingImpactPreview,
    submitting,
    lastErrorMessageKey,
    lastListErrorRetryable,
    activeGroupCode,
    fetchModules,
    fetchModule,
    createModule,
    createVersion,
    updateDraftVersion,
    transitionReview,
    fetchLifecycleImpactPreview,
    applyLifecycleOperation,
    clearSelected,
    clearListError,
  }
})
