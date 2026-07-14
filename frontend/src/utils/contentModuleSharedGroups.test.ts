import { describe, expect, it } from 'vitest'
import {
  excludeOwnerFromSharedGroupCodes,
  formatSharedGroupCodesLabel,
  normalizeSharedGroupCodes,
  sharedGroupSelectionChanged,
} from '@/utils/contentModuleSharedGroups'

describe('contentModuleSharedGroups', () => {
  it('normalizes, uppercases, dedupes, and sorts codes', () => {
    expect(normalizeSharedGroupCodes([' wealth ', 'RETAIL', 'retail', ''])).toEqual([
      'RETAIL',
      'WEALTH',
    ])
  })

  it('excludes the owner group from shared selection (SGC-007)', () => {
    expect(excludeOwnerFromSharedGroupCodes(['HQ', 'RETAIL', 'hq'], 'HQ')).toEqual(['RETAIL'])
  })

  it('detects shared selection changes for settings confirm (SGC-005)', () => {
    expect(sharedGroupSelectionChanged(['RETAIL', 'WEALTH'], ['WEALTH', 'RETAIL'])).toBe(false)
    expect(sharedGroupSelectionChanged(['RETAIL', 'WEALTH'], ['RETAIL'])).toBe(true)
  })

  it('formats shared codes for summary labels', () => {
    expect(formatSharedGroupCodesLabel(['WEALTH', 'RETAIL'])).toBe('RETAIL, WEALTH')
  })
})
