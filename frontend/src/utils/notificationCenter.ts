import type { RouteLocationRaw } from 'vue-router'
import type { CollaborationWorkItemQueue } from '@/types/collaboration'

/** Default unread-count poll interval (C7-C7). Tests may inject a shorter interval. */
export const NOTIFICATION_POLL_INTERVAL_MS = 30_000

/** Badge display: hide at 0; exact 1–99; cap at 99+ (C7-C12). */
export function formatUnreadBadgeLabel(unreadCount: number): string | null {
  if (unreadCount <= 0) {
    return null
  }
  if (unreadCount >= 100) {
    return '99+'
  }
  return String(unreadCount)
}

/** Deep-link to Dashboard task hub partition (C7-C11). */
export function buildNotificationTaskHubLocation(
  queue: CollaborationWorkItemQueue,
): RouteLocationRaw {
  return {
    path: '/dashboard',
    query: { queue },
    hash: '#tasks-section',
  }
}
