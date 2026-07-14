import { http } from '@/api/http'
import type { ApiEnvelope } from '@/types/session'
import type { Schema } from '@/types/openapi'

export type OutdatedClauseReferenceAuthorTask = Schema<'OutdatedClauseReferenceAuthorTaskView'>

export async function listOutdatedClauseReferenceAuthorTasks(): Promise<
  OutdatedClauseReferenceAuthorTask[]
> {
  const response = await http.get<ApiEnvelope<OutdatedClauseReferenceAuthorTask[]>>(
    '/author-workflow/outdated-clause-references',
  )
  return response.data.result ?? []
}
