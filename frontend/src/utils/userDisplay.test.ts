import { describe, expect, it } from 'vitest'
import {
  formatUserDisplayLabel,
  resolveSubmitterDisplay,
  resolveUpdatedByDisplay,
} from '@/utils/userDisplay'

describe('userDisplay utils', () => {
  describe('formatUserDisplayLabel', () => {
    it('prefers displayName over username', () => {
      expect(formatUserDisplayLabel('10000001', 'Alice Author')).toBe('Alice Author')
    })

    it('falls back to username when displayName is empty', () => {
      expect(formatUserDisplayLabel('10000001', '')).toBe('10000001')
      expect(formatUserDisplayLabel('10000001', '   ')).toBe('10000001')
      expect(formatUserDisplayLabel('10000001')).toBe('10000001')
    })

    it('returns em dash when both values are empty', () => {
      expect(formatUserDisplayLabel('', '')).toBe('—')
    })
  })

  describe('resolveUpdatedByDisplay', () => {
    it('prefers updatedByDisplayName from API', () => {
      expect(resolveUpdatedByDisplay('10000001', 'Bob Builder')).toBe('Bob Builder')
    })

    it('falls back to username', () => {
      expect(resolveUpdatedByDisplay('10000001')).toBe('10000001')
    })
  })

  describe('resolveSubmitterDisplay', () => {
    it('prefers submitterDisplayName from API', () => {
      expect(resolveSubmitterDisplay('10000003', 'Carol Tester')).toBe('Carol Tester')
    })

    it('falls back to submitter user id', () => {
      expect(resolveSubmitterDisplay('10000003')).toBe('10000003')
    })

    it('returns em dash when submitter is missing', () => {
      expect(resolveSubmitterDisplay()).toBe('—')
      expect(resolveSubmitterDisplay('', '')).toBe('—')
    })
  })
})
