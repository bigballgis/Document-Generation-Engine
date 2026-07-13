import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as contentModulesApi from '@/api/contentModules'
import { resolveStoreErrorMessageKey } from '@/api/http'
import {
  clearStoreListError,
  handleStoreListFailure,
  type AbortableRequestOptions,
} from '@/stores/storeRequestSupport'
import { createContentModulesMutationActions } from '@/stores/createContentModulesMutationActions'
import type {
  ContentModuleDetail,
  ContentModuleLifecycleImpactSummary,
  ContentModuleSummary,
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

  const mutationActions = createContentModulesMutationActions({
    modules,
    selectedModule,
    lifecycleImpactPreview,
    submitting,
    loadingImpactPreview,
    lastErrorMessageKey,
    activeGroupCode,
    fetchModule,
  })

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
    ...mutationActions,
    clearSelected,
    clearListError,
  }
})
