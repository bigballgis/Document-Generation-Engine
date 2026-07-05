/**
 * LR-C7: in-app notification center store. Holds the unread collaboration to-do count and
 * exposes a bell-toggle for the notification panel. In-app only — no email/IM, no push (v1
 * boundary per LRP §0.1).
 *
 * The store pulls from the existing collaboration store (`useCollaborationStore`) — it does
 * not introduce a new backend channel. The "notification" is the collaboration to-do count
 * plus a per-item deep link into the owning template/master.
 */
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

export const useNotificationStore = defineStore('notifications', () => {
  const unreadCount = ref(0)
  const panelOpen = ref(false)
  const lastFetchedAt = ref<number | null>(null)

  const hasUnread = computed(() => unreadCount.value > 0)

  function setUnreadCount(count: number) {
    unreadCount.value = Math.max(0, count)
    lastFetchedAt.value = Date.now()
  }

  function togglePanel() {
    panelOpen.value = !panelOpen.value
  }

  function openPanel() {
    panelOpen.value = true
  }

  function closePanel() {
    panelOpen.value = false
  }

  function markAllRead() {
    unreadCount.value = 0
  }

  return {
    unreadCount,
    panelOpen,
    lastFetchedAt,
    hasUnread,
    setUnreadCount,
    togglePanel,
    openPanel,
    closePanel,
    markAllRead,
  }
})
