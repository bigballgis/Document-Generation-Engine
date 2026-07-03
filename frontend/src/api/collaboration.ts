import { unwrapEnvelope } from '@/api/envelope'
import { http } from '@/api/http'
import type { ApiEnvelope } from '@/types/session'
import type {
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
