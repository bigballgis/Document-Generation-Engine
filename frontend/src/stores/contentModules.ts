import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as contentModulesApi from '@/api/contentModules'
import { resolveApiErrorMessageKey, resolveStoreErrorMessageKey } from '@/api/http'
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
  const selectedModule = ref<ContentModuleDetail | null>(null)
  const lifecycleImpactPreview = ref<ContentModuleLifecycleImpactSummary | null>(null)
  const loadingList = ref(false)
  const loadingDetail = ref(false)
  const loadingImpactPreview = ref(false)
  const submitting = ref(false)
  const lastErrorMessageKey = ref<string | null>(null)
  const activeGroupCode = ref('')

  async function fetchModules(groupCode: string): Promise<void> {
    loadingList.value = true
    lastErrorMessageKey.value = null
    activeGroupCode.value = groupCode
    try {
      modules.value = await contentModulesApi.listContentModules(groupCode)
    } catch (error) {
      lastErrorMessageKey.value = resolveStoreErrorMessageKey(
        error,
        'contentModules.error.loadList',
      )
      throw error
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
      applyUpdatedModule(created)
      if (activeGroupCode.value === payload.groupCode) {
        modules.value = [toSummary(created), ...modules.value.filter((item) => item.moduleId !== created.moduleId)]
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
      applyUpdatedModule(updated)
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
      applyUpdatedModule(updated)
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

  function applyUpdatedModule(updated: ContentModuleDetail) {
    selectedModule.value = updated
    modules.value = modules.value.map((item) =>
      item.moduleId === updated.moduleId ? toSummary(updated) : item,
    )
  }

  function toSummary(detail: ContentModuleDetail): ContentModuleSummary {
    return {
      moduleId: detail.moduleId,
      moduleCode: detail.moduleCode,
      groupCode: detail.groupCode,
      name: detail.name,
      description: detail.description,
      sharedGroupCodes: detail.sharedGroupCodes,
      createdAt: detail.versions[0]?.createdAt ?? '',
      updatedAt: detail.versions.reduce(
        (latest, version) => (version.updatedAt > latest ? version.updatedAt : latest),
        detail.versions[0]?.updatedAt ?? '',
      ),
    }
  }

  function clearSelected() {
    selectedModule.value = null
    lifecycleImpactPreview.value = null
  }

  function clearListError() {
    lastErrorMessageKey.value = null
  }

  return {
    modules,
    selectedModule,
    lifecycleImpactPreview,
    loadingList,
    loadingDetail,
    loadingImpactPreview,
    submitting,
    lastErrorMessageKey,
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
