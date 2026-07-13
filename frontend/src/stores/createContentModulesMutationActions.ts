import type { Ref } from 'vue'
import * as contentModulesApi from '@/api/contentModules'
import { resolveApiErrorMessageKey } from '@/api/http'
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

export function createContentModulesMutationActions(deps: {
  modules: Ref<ContentModuleSummary[]>
  selectedModule: Ref<ContentModuleDetail | null>
  lifecycleImpactPreview: Ref<ContentModuleLifecycleImpactSummary | null>
  submitting: Ref<boolean>
  loadingImpactPreview: Ref<boolean>
  lastErrorMessageKey: Ref<string | null>
  activeGroupCode: Ref<string>
  fetchModule: (moduleId: string) => Promise<void>
}) {
  const {
    modules,
    selectedModule,
    lifecycleImpactPreview,
    submitting,
    loadingImpactPreview,
    lastErrorMessageKey,
    activeGroupCode,
    fetchModule,
  } = deps

  async function createModule(payload: CreateContentModulePayload): Promise<ContentModuleDetail> {
    submitting.value = true
    lastErrorMessageKey.value = null
    try {
      const created = await contentModulesApi.createContentModule(payload)
      applyUpdatedContentModule(selectedModule, modules, created)
      if (!activeGroupCode.value || activeGroupCode.value === payload.groupCode) {
        modules.value = [
          toContentModuleSummary(created),
          ...modules.value.filter((item) => item.moduleId !== created.moduleId),
        ]
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

  async function fetchLifecycleImpactPreview(
    moduleId: string,
  ): Promise<ContentModuleLifecycleImpactSummary> {
    loadingImpactPreview.value = true
    lastErrorMessageKey.value = null
    try {
      lifecycleImpactPreview.value =
        await contentModulesApi.previewContentModuleLifecycleImpact(moduleId)
      return lifecycleImpactPreview.value
    } catch (error) {
      lastErrorMessageKey.value = resolveApiErrorMessageKey(
        error,
        'contentModules.error.loadImpactPreview',
      )
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

  return {
    createModule,
    createVersion,
    updateDraftVersion,
    transitionReview,
    fetchLifecycleImpactPreview,
    applyLifecycleOperation,
  }
}
