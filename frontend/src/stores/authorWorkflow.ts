import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as authorWorkflowApi from '@/api/authorWorkflow'
import type { OutdatedClauseReferenceAuthorTask } from '@/api/authorWorkflow'

export const useAuthorWorkflowStore = defineStore('authorWorkflow', () => {
  const outdatedClauseTasks = ref<OutdatedClauseReferenceAuthorTask[]>([])
  const loadingOutdatedClauseTasks = ref(false)
  const outdatedClauseTasksError = ref(false)

  async function fetchOutdatedClauseReferenceTasks(): Promise<void> {
    loadingOutdatedClauseTasks.value = true
    outdatedClauseTasksError.value = false
    try {
      outdatedClauseTasks.value = await authorWorkflowApi.listOutdatedClauseReferenceAuthorTasks()
    } catch {
      outdatedClauseTasks.value = []
      outdatedClauseTasksError.value = true
    } finally {
      loadingOutdatedClauseTasks.value = false
    }
  }

  function clearOutdatedClauseReferenceTasks(): void {
    outdatedClauseTasks.value = []
    outdatedClauseTasksError.value = false
  }

  return {
    outdatedClauseTasks,
    loadingOutdatedClauseTasks,
    outdatedClauseTasksError,
    fetchOutdatedClauseReferenceTasks,
    clearOutdatedClauseReferenceTasks,
  }
})
