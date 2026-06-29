import { http } from '@/api/http'
import type { ApiEnvelope } from '@/types/session'
import type {
  CollaborationTimeoutConfig,
  CollaborationWorkItemSummary,
  ListCollaborationWorkItemsParams,
  UpsertCollaborationTimeoutConfigPayload,
} from '@/types/collaboration'

function unwrap<T>(envelope: ApiEnvelope<T>): T {
  if (!envelope.result) {
    throw new Error('API response missing result')
  }
  return envelope.result
}

export async function listCollaborationWorkItems(
  params?: ListCollaborationWorkItemsParams,
): Promise<CollaborationWorkItemSummary[]> {
  const response = await http.get<ApiEnvelope<CollaborationWorkItemSummary[]>>(
    '/collaboration-work-items',
    { params },
  )
  return unwrap(response.data)
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
  return unwrap(response.data)
}

export async function upsertCollaborationTimeoutConfig(
  payload: UpsertCollaborationTimeoutConfigPayload,
): Promise<CollaborationTimeoutConfig> {
  const response = await http.put<ApiEnvelope<CollaborationTimeoutConfig>>(
    '/collaboration-timeout-config',
    payload,
  )
  return unwrap(response.data)
}
