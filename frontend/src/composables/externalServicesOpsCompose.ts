import type { ManagementInvocationSummary } from '@/types/template'
import type { TemplateSummary } from '@/types/template'

/** Max packages sampled when composing cross-package invocation views (no aggregation API). */
export const CROSS_PACKAGE_SAMPLE_CAP = 15

/** Invocations fetched per package when composing (page 0 only). */
export const CROSS_PACKAGE_INVOCATIONS_PER_PACKAGE = 20

export type CrossPackageInvocationRow = ManagementInvocationSummary & {
  templateId: string
  templateName: string
  templateExternalId: string
  groupCode: string
}

export type ExternalServicesOpsSummary = {
  sampledPackageCount: number
  sampledInvocationCount: number
  failedCount: number
  succeededCount: number
  /** null when sample is empty — do not invent a rate. */
  failureRatePercent: number | null
  compositionCapped: boolean
}

const FAILED_STATUSES = new Set(['FAILED', 'FAILURE', 'PARTIAL_SUCCEEDED'])
const SUCCEEDED_STATUSES = new Set(['SUCCEEDED', 'SUCCESS'])

export function isFailedInvocationStatus(status: string): boolean {
  return FAILED_STATUSES.has(status.trim().toUpperCase())
}

export function isSucceededInvocationStatus(status: string): boolean {
  return SUCCEEDED_STATUSES.has(status.trim().toUpperCase())
}

export function selectPackagesForComposition(
  packages: TemplateSummary[],
  cap = CROSS_PACKAGE_SAMPLE_CAP,
): { selected: TemplateSummary[]; compositionCapped: boolean } {
  if (packages.length <= cap) {
    return { selected: packages, compositionCapped: false }
  }
  return { selected: packages.slice(0, cap), compositionCapped: true }
}

export function attachPackageIdentity(
  template: TemplateSummary,
  rows: ManagementInvocationSummary[],
): CrossPackageInvocationRow[] {
  return rows.map((row) => ({
    ...row,
    templateId: template.id,
    templateName: template.name,
    templateExternalId: template.externalId,
    groupCode: template.groupCode,
  }))
}

export function mergeAndSortCrossPackageRows(
  batches: CrossPackageInvocationRow[],
): CrossPackageInvocationRow[] {
  return [...batches].sort((a, b) => {
    const aTime = Date.parse(a.createdAt)
    const bTime = Date.parse(b.createdAt)
    if (Number.isNaN(aTime) && Number.isNaN(bTime)) {
      return 0
    }
    if (Number.isNaN(aTime)) {
      return 1
    }
    if (Number.isNaN(bTime)) {
      return -1
    }
    return bTime - aTime
  })
}

export function computeOpsSummaryFromRows(
  rows: CrossPackageInvocationRow[],
  sampledPackageCount: number,
  compositionCapped: boolean,
): ExternalServicesOpsSummary {
  const sampledInvocationCount = rows.length
  let failedCount = 0
  let succeededCount = 0
  for (const row of rows) {
    if (isFailedInvocationStatus(row.status)) {
      failedCount += 1
    }
    if (isSucceededInvocationStatus(row.status)) {
      succeededCount += 1
    }
  }
  const failureRatePercent =
    sampledInvocationCount === 0
      ? null
      : Math.round((failedCount / sampledInvocationCount) * 1000) / 10

  return {
    sampledPackageCount,
    sampledInvocationCount,
    failedCount,
    succeededCount,
    failureRatePercent,
    compositionCapped,
  }
}

export function filterRowsByRequestId(
  rows: CrossPackageInvocationRow[],
  requestId: string | undefined,
): CrossPackageInvocationRow[] {
  const needle = requestId?.trim().toLowerCase()
  if (!needle) {
    return rows
  }
  return rows.filter((row) => row.requestId.toLowerCase().includes(needle))
}

export function paginateRows<T>(
  rows: T[],
  page: number,
  pageSize: number,
): { content: T[]; totalElements: number } {
  const safePage = Math.max(0, page)
  const start = safePage * pageSize
  return {
    content: rows.slice(start, start + pageSize),
    totalElements: rows.length,
  }
}
