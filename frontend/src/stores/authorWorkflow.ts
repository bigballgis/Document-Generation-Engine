import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as authorWorkflowApi from '@/api/authorWorkflow'
import type {
  AnnualReviewDueAuthorTask,
  OutdatedClauseReferenceAuthorTask,
} from '@/api/authorWorkflow'

export const useAuthorWorkflowStore = defineStore('authorWorkflow', () => {
  const outdatedClauseTasks = ref<OutdatedClauseReferenceAuthorTask[]>([])
  const loadingOutdatedClauseTasks = ref(false)
  const outdatedClauseTasksError = ref(false)

  const annualReviewDueTasks = ref<AnnualReviewDueAuthorTask[]>([])
  const loadingAnnualReviewDueTasks = ref(false)
  const annualReviewDueTasksError = ref(false)

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

  async function fetchAnnualReviewDueTasks(): Promise<void> {
    loadingAnnualReviewDueTasks.value = true
    annualReviewDueTasksError.value = false
    try {
      annualReviewDueTasks.value = await authorWorkflowApi.listAnnualReviewDueAuthorTasks()
    } catch {
      annualReviewDueTasks.value = []
      annualReviewDueTasksError.value = true
    } finally {
      loadingAnnualReviewDueTasks.value = false
    }
  }

  function clearAnnualReviewDueTasks(): void {
    annualReviewDueTasks.value = []
    annualReviewDueTasksError.value = false
  }

  return {
    outdatedClauseTasks,
    loadingOutdatedClauseTasks,
    outdatedClauseTasksError,
    fetchOutdatedClauseReferenceTasks,
    clearOutdatedClauseReferenceTasks,
    annualReviewDueTasks,
    loadingAnnualReviewDueTasks,
    annualReviewDueTasksError,
    fetchAnnualReviewDueTasks,
    clearAnnualReviewDueTasks,
  }
})
