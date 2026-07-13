import { http } from '@/api/http'
import type { Schema } from '@/types/openapi'

export type RouteAccessDeniedRequest = Schema<'RouteAccessDeniedRequest'>

/**
 * Reports a client-side route-guard denial for durable SECURITY_ROUTE_ACCESS_DENIED
 * persistence (LR-D7). Success is 204. Callers must treat failures as non-blocking.
 */
export async function reportRouteAccessDenied(
  payload: RouteAccessDeniedRequest,
): Promise<void> {
  await http.post('/security-audit/route-access-denied', payload)
}
