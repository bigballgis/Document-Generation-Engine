import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as collaborationApi from '@/api/collaboration'
import { resolveApiErrorMessageKey } from '@/api/http'
import type {
  CollaborationWorkItemSummary,
  ListCollaborationWorkItemsParams,
} from '@/types/collaboration'

export const useCollaborationStore = defineStore('collaboration', () => {
  const workItems = ref<CollaborationWorkItemSummary[]>([])
  const loadingWorkItems = ref(false)
  const workItemsErrorMessageKey = ref<string | null>(null)

  async function fetchWorkItems(params?: ListCollaborationWorkItemsParams): Promise<void> {
    loadingWorkItems.value = true
    workItemsErrorMessageKey.value = null
    try {
      workItems.value = await collaborationApi.listCollaborationWorkItems(params)
    } catch (error) {
      workItemsErrorMessageKey.value = resolveApiErrorMessageKey(
        error,
        'collaboration.workItems.error.load',
      )
      throw error
    } finally {
      loadingWorkItems.value = false
    }
  }

  function clearWorkItems(): void {
    workItems.value = []
    workItemsErrorMessageKey.value = null
  }

  return {
    workItems,
    loadingWorkItems,
    workItemsErrorMessageKey,
    fetchWorkItems,
    clearWorkItems,
  }
})
