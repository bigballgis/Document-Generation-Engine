import { http } from '@/api/http'
import type { ApiEnvelope } from '@/types/session'
import type { Schema } from '@/types/openapi'
import type { AnnualReviewDueAuthorTask } from '@/types/template'

export type OutdatedClauseReferenceAuthorTask = Schema<'OutdatedClauseReferenceAuthorTaskView'>

export type { AnnualReviewDueAuthorTask }

export async function listOutdatedClauseReferenceAuthorTasks(): Promise<
  OutdatedClauseReferenceAuthorTask[]
> {
  const response = await http.get<ApiEnvelope<OutdatedClauseReferenceAuthorTask[]>>(
    '/author-workflow/outdated-clause-references',
  )
  return response.data.result ?? []
}

/** CE-G05 — templates with nextReviewDue ≤ todayUtc for authorTemplates callers. */
export async function listAnnualReviewDueAuthorTasks(): Promise<AnnualReviewDueAuthorTask[]> {
  const response = await http.get<ApiEnvelope<AnnualReviewDueAuthorTask[]>>(
    '/author-workflow/annual-review-due-tasks',
  )
  return response.data.result ?? []
}
