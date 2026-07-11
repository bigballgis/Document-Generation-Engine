import type { Schema } from '@/types/openapi'

export type CollaborationWorkItemQueue = Schema<'CollaborationWorkItemQueue'>

export type CollaborationWorkItemTriggerType = Schema<'CollaborationWorkItemTriggerType'>

export type CollaborationTimeoutScopeType = Schema<'CollaborationTimeoutScopeType'>

export type CollaborationWorkItemSummary = Schema<'CollaborationWorkItemSummaryView'>

export type CollaborationNotificationItem = Schema<'CollaborationNotificationItemView'>

export type CollaborationNotificationUnreadCount =
  Schema<'CollaborationNotificationUnreadCountView'>

export type CollaborationTimeoutConfig = Schema<'CollaborationTimeoutConfigView'>

export type UpsertCollaborationTimeoutConfigPayload = Schema<'UpsertCollaborationTimeoutConfigRequest'>

/** Not yet modeled in `openapi-v1.yaml` (client-side query params). */
export interface ListCollaborationWorkItemsParams {
  groupCode?: string
  queue?: CollaborationWorkItemQueue
}
