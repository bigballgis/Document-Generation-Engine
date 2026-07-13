import { describe, expect, it } from 'vitest'
import {
  hasConfiguredAdGroups,
  parseAdGroupsConfiguredFromSummary,
} from '@/utils/apiAccessDiagnostics'

describe('apiAccessDiagnostics', () => {
  it('parses adGroupsConfigured=false from publish-gate summary', () => {
    expect(
      parseAdGroupsConfiguredFromSummary('skeletonPresent=true,adGroupsConfigured=false'),
    ).toBe(false)
  })

  it('parses adGroupsConfigured=true from publish-gate summary', () => {
    expect(
      parseAdGroupsConfiguredFromSummary('skeletonPresent=true,adGroupsConfigured=true'),
    ).toBe(true)
  })

  it('returns null when summary omits the diagnostic', () => {
    expect(parseAdGroupsConfiguredFromSummary('skeletonPresent=true')).toBeNull()
    expect(parseAdGroupsConfiguredFromSummary(null)).toBeNull()
  })

  it('detects configured AD groups on policy', () => {
    expect(hasConfiguredAdGroups({ allowedAdGroups: ['RETAIL-API'] })).toBe(true)
    expect(hasConfiguredAdGroups({ allowedAdGroups: [] })).toBe(false)
    expect(hasConfiguredAdGroups({ allowedAdGroups: ['  '] })).toBe(false)
    expect(hasConfiguredAdGroups(null)).toBe(false)
  })
})
