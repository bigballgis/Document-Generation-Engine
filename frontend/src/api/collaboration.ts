import { unwrapEnvelope } from '@/api/envelope'
import { http } from '@/api/http'
import type { ApiEnvelope } from '@/types/session'
import type {
  CollaborationNotificationItem,
  CollaborationNotificationUnreadCount,
  CollaborationTimeoutConfig,
  CollaborationWorkItemSummary,
  ListCollaborationWorkItemsParams,
  UpsertCollaborationTimeoutConfigPayload,
} from '@/types/collaboration'

export async function listCollaborationWorkItems(
  params?: ListCollaborationWorkItemsParams,
): Promise<CollaborationWorkItemSummary[]> {
  const response = await http.get<ApiEnvelope<CollaborationWorkItemSummary[]>>(
    '/collaboration-work-items',
    { params },
  )
  return unwrapEnvelope(response.data)
}

export async function getCollaborationNotificationUnreadCount(): Promise<CollaborationNotificationUnreadCount> {
  const response = await http.get<ApiEnvelope<CollaborationNotificationUnreadCount>>(
    '/collaboration-notifications/unread-count',
  )
  return unwrapEnvelope(response.data)
}

export async function listCollaborationNotifications(): Promise<CollaborationNotificationItem[]> {
  const response = await http.get<ApiEnvelope<CollaborationNotificationItem[]>>(
    '/collaboration-notifications',
  )
  return unwrapEnvelope(response.data)
}

export async function markCollaborationNotificationRead(
  workItemId: string,
): Promise<CollaborationNotificationUnreadCount> {
  const response = await http.post<ApiEnvelope<CollaborationNotificationUnreadCount>>(
    `/collaboration-notifications/${workItemId}/read`,
  )
  return unwrapEnvelope(response.data)
}

export async function markAllCollaborationNotificationsRead(): Promise<CollaborationNotificationUnreadCount> {
  const response = await http.post<ApiEnvelope<CollaborationNotificationUnreadCount>>(
    '/collaboration-notifications/read-all',
  )
  return unwrapEnvelope(response.data)
}

export async function getCollaborationTimeoutConfig(
  groupCode?: string,
): Promise<CollaborationTimeoutConfig> {
  const response = await http.get<ApiEnvelope<CollaborationTimeoutConfig>>(
    '/collaboration-timeout-config',
    {
      params: groupCode ? { groupCode } : undefined,
    },
  )
  return unwrapEnvelope(response.data)
}

export async function upsertCollaborationTimeoutConfig(
  payload: UpsertCollaborationTimeoutConfigPayload,
): Promise<CollaborationTimeoutConfig> {
  const response = await http.put<ApiEnvelope<CollaborationTimeoutConfig>>(
    '/collaboration-timeout-config',
    payload,
  )
  return unwrapEnvelope(response.data)
}
