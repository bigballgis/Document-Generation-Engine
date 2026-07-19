import type { AbortableRequestOptions } from '@/stores/storeRequestSupport'

/**
 * Options for {@link fetchTemplates}. Mirrors the underlying {@link listTemplates} API options so
 * the store can forward `signal` plus catalog query params (search/groupCode/lifecycleStatus/sort)
 * without narrowing the type below what callers (e.g. command palette) rely on.
 */
export type TemplateListFetchOptions = AbortableRequestOptions & {
  search?: string
  groupCode?: string
  lifecycleStatus?: string
  approvalSubState?: string
  /** IBL-E1 — optional exact BCP-47 locale filter. */
  locale?: string
  sort?: string
}
