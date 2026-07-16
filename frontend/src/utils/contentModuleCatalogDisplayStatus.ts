import type { ContentModuleSummary } from '@/types/contentModule'

/**
 * CE-U20 — catalog head display status aligned with ContentModuleStatusBadge:
 * DEPRECATED/STOPPED lifecycle wins; otherwise reviewState.
 */
export function contentModuleCatalogDisplayStatus(
  summary: Pick<ContentModuleSummary, 'reviewState' | 'lifecycleState'>,
): string {
  if (summary.lifecycleState === 'DEPRECATED' || summary.lifecycleState === 'STOPPED') {
    return summary.lifecycleState
  }
  return summary.reviewState
}
