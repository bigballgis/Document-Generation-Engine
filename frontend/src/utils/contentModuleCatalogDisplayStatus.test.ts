import { describe, expect, it } from 'vitest'
import { contentModuleCatalogDisplayStatus } from '@/utils/contentModuleCatalogDisplayStatus'

describe('contentModuleCatalogDisplayStatus', () => {
  it('prefers DEPRECATED lifecycle over reviewState', () => {
    expect(
      contentModuleCatalogDisplayStatus({
        reviewState: 'APPROVED',
        lifecycleState: 'DEPRECATED',
      }),
    ).toBe('DEPRECATED')
  })

  it('prefers STOPPED lifecycle over reviewState', () => {
    expect(
      contentModuleCatalogDisplayStatus({
        reviewState: 'APPROVED',
        lifecycleState: 'STOPPED',
      }),
    ).toBe('STOPPED')
  })

  it('uses reviewState when lifecycle is ACTIVE or absent', () => {
    expect(
      contentModuleCatalogDisplayStatus({
        reviewState: 'DRAFT',
        lifecycleState: undefined,
      }),
    ).toBe('DRAFT')
    expect(
      contentModuleCatalogDisplayStatus({
        reviewState: 'APPROVED',
        lifecycleState: 'ACTIVE',
      }),
    ).toBe('APPROVED')
  })
})
