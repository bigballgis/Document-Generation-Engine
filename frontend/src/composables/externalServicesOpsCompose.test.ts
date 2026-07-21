import { describe, expect, it } from 'vitest'
import {
  attachPackageIdentity,
  computeOpsSummaryFromRows,
  CROSS_PACKAGE_SAMPLE_CAP,
  filterRowsByRequestId,
  mergeAndSortCrossPackageRows,
  paginateRows,
  selectPackagesForComposition,
} from '@/composables/externalServicesOpsCompose'
import type { ManagementInvocationSummary, TemplateSummary } from '@/types/template'

function template(overrides: Partial<TemplateSummary> & { id: string }): TemplateSummary {
  return {
    externalId: overrides.externalId ?? `EXT-${overrides.id}`,
    groupCode: overrides.groupCode ?? 'RETAIL',
    name: overrides.name ?? `Package ${overrides.id}`,
    masterId: 'master-1',
    lifecycleStatus: 'PUBLISHED',
    releaseVersion: '1.0.0',
    releaseVersionCount: 1,
    updatedBy: 'admin',
    updatedAt: '2026-07-20T00:00:00Z',
    ...overrides,
  }
}

function inv(
  overrides: Partial<ManagementInvocationSummary> & { invocationId: string },
): ManagementInvocationSummary {
  return {
    invocationKind: 'SINGLE',
    status: 'SUCCEEDED',
    requestId: `req-${overrides.invocationId}`,
    resolvedReleaseVersion: '1.0.0',
    routeType: 'DEFAULT',
    createdAt: '2026-07-20T12:00:00Z',
    accessAccountSummary: 'acct',
    ...overrides,
  }
}

describe('externalServicesOpsCompose (BDD-SYS-NORM-W3)', () => {
  it('caps package sample honestly when composition limit is exceeded', () => {
    const packages = Array.from({ length: CROSS_PACKAGE_SAMPLE_CAP + 3 }, (_, i) =>
      template({ id: `tpl-${i}` }),
    )
    const { selected, compositionCapped } = selectPackagesForComposition(packages)
    expect(selected).toHaveLength(CROSS_PACKAGE_SAMPLE_CAP)
    expect(compositionCapped).toBe(true)
  })

  it('computes failure rate from sampled rows without inventing SLOs', () => {
    const tpl = template({ id: 'tpl-1' })
    const rows = attachPackageIdentity(tpl, [
      inv({ invocationId: 'a', status: 'SUCCEEDED' }),
      inv({ invocationId: 'b', status: 'FAILED' }),
      inv({ invocationId: 'c', status: 'PARTIAL_SUCCEEDED' }),
      inv({ invocationId: 'd', status: 'PROCESSING' }),
    ])
    const summary = computeOpsSummaryFromRows(rows, 1, false)
    expect(summary.sampledInvocationCount).toBe(4)
    expect(summary.failedCount).toBe(2)
    expect(summary.succeededCount).toBe(1)
    expect(summary.failureRatePercent).toBe(50)
  })

  it('returns null failure rate for empty sample (honest empty)', () => {
    const summary = computeOpsSummaryFromRows([], 0, false)
    expect(summary.failureRatePercent).toBeNull()
    expect(summary.sampledInvocationCount).toBe(0)
  })

  it('merges rows newest-first across packages', () => {
    const a = attachPackageIdentity(template({ id: 'a', name: 'A' }), [
      inv({ invocationId: 'old', createdAt: '2026-07-19T00:00:00Z' }),
    ])
    const b = attachPackageIdentity(template({ id: 'b', name: 'B' }), [
      inv({ invocationId: 'new', createdAt: '2026-07-21T00:00:00Z' }),
    ])
    const merged = mergeAndSortCrossPackageRows([...a, ...b])
    expect(merged.map((r) => r.invocationId)).toEqual(['new', 'old'])
  })

  it('filters by requestId substring and paginates client-side', () => {
    const tpl = template({ id: 'tpl-1' })
    const rows = attachPackageIdentity(tpl, [
      inv({ invocationId: '1', requestId: 'REQ-AAA' }),
      inv({ invocationId: '2', requestId: 'REQ-BBB' }),
    ])
    const filtered = filterRowsByRequestId(rows, 'bbb')
    expect(filtered).toHaveLength(1)
    expect(paginateRows(filtered, 0, 10).totalElements).toBe(1)
  })
})
