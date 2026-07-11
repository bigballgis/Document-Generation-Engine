import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as collaborationApi from '@/api/collaboration'
import { resolveApiErrorMessageKey } from '@/api/http'
import type { CollaborationNotificationItem } from '@/types/collaboration'

export const useCollaborationNotificationsStore = defineStore('collaborationNotifications', () => {
  const unreadCount = ref(0)
  const items = ref<CollaborationNotificationItem[]>([])
  const loadingUnread = ref(false)
  const loadingList = ref(false)
  const marking = ref(false)
  const unreadErrorMessageKey = ref<string | null>(null)
  const listErrorMessageKey = ref<string | null>(null)
  const actionErrorMessageKey = ref<string | null>(null)

  const hasUnread = computed(() => unreadCount.value > 0)

  async function fetchUnreadCount(): Promise<void> {
    loadingUnread.value = true
    unreadErrorMessageKey.value = null
    try {
      const result = await collaborationApi.getCollaborationNotificationUnreadCount()
      unreadCount.value = result.unreadCount
    } catch (error) {
      unreadErrorMessageKey.value = resolveApiErrorMessageKey(
        error,
        'collaboration.notifications.error.loadUnread',
      )
      throw error
    } finally {
      loadingUnread.value = false
    }
  }

  async function fetchList(): Promise<void> {
    loadingList.value = true
    listErrorMessageKey.value = null
    try {
      items.value = await collaborationApi.listCollaborationNotifications()
    } catch (error) {
      listErrorMessageKey.value = resolveApiErrorMessageKey(
        error,
        'collaboration.notifications.error.loadList',
      )
      throw error
    } finally {
      loadingList.value = false
    }
  }

  async function markRead(workItemId: string): Promise<void> {
    marking.value = true
    actionErrorMessageKey.value = null
    try {
      const result = await collaborationApi.markCollaborationNotificationRead(workItemId)
      unreadCount.value = result.unreadCount
      items.value = items.value.map((item) =>
        item.workItemId === workItemId ? { ...item, read: true } : item,
      )
    } catch (error) {
      actionErrorMessageKey.value = resolveApiErrorMessageKey(
        error,
        'collaboration.notifications.error.markRead',
      )
      throw error
    } finally {
      marking.value = false
    }
  }

  async function markAllRead(): Promise<void> {
    marking.value = true
    actionErrorMessageKey.value = null
    try {
      const result = await collaborationApi.markAllCollaborationNotificationsRead()
      unreadCount.value = result.unreadCount
      items.value = items.value.map((item) => ({ ...item, read: true }))
    } catch (error) {
      actionErrorMessageKey.value = resolveApiErrorMessageKey(
        error,
        'collaboration.notifications.error.markAll',
      )
      throw error
    } finally {
      marking.value = false
    }
  }

  function clear(): void {
    unreadCount.value = 0
    items.value = []
    unreadErrorMessageKey.value = null
    listErrorMessageKey.value = null
    actionErrorMessageKey.value = null
  }

  return {
    unreadCount,
    items,
    loadingUnread,
    loadingList,
    marking,
    unreadErrorMessageKey,
    listErrorMessageKey,
    actionErrorMessageKey,
    hasUnread,
    fetchUnreadCount,
    fetchList,
    markRead,
    markAllRead,
    clear,
  }
})
