import { unwrapEnvelope } from '@/api/envelope'
import { http } from '@/api/http'
import type { Schema } from '@/types/openapi'
import type { ApiEnvelope } from '@/types/session'

export type DashboardSummaryView = Schema<'DashboardSummaryView'>

/** GET /dashboard/summary — authorized-group Overview bucket counts (PRR-D01c). */
export async function fetchDashboardSummary(): Promise<DashboardSummaryView> {
  const response = await http.get<ApiEnvelope<DashboardSummaryView>>('/dashboard/summary')
  return unwrapEnvelope(response.data)
}
