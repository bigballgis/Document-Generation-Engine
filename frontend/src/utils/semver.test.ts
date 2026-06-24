import { describe, expect, it } from 'vitest'
import {
  bumpMajor,
  bumpMinor,
  bumpPatch,
  conflictsWithExisting,
  formatSemver,
  isValidSemver,
  parseSemver,
  suggestNextVersions,
} from '@/utils/semver'

describe('semver', () => {
  it('validates and parses release versions', () => {
    expect(isValidSemver('1.0.0')).toBe(true)
    expect(isValidSemver('1.0')).toBe(false)
    expect(parseSemver('2.3.4')).toEqual({ major: 2, minor: 3, patch: 4 })
  })

  it('suggests next major, minor, and patch versions', () => {
    expect(suggestNextVersions('1.2.3')).toEqual({
      major: '2.0.0',
      minor: '1.3.0',
      patch: '1.2.4',
    })
    expect(suggestNextVersions(null)).toEqual({
      major: '1.0.0',
      minor: '1.0.0',
      patch: '1.0.0',
    })
  })

  it('bumps individual semver parts', () => {
    const current = { major: 1, minor: 2, patch: 3 }
    expect(formatSemver(bumpMajor(current))).toBe('2.0.0')
    expect(formatSemver(bumpMinor(current))).toBe('1.3.0')
    expect(formatSemver(bumpPatch(current))).toBe('1.2.4')
  })

  it('detects conflicts with existing release versions', () => {
    expect(conflictsWithExisting('1.0.0', ['1.0.0', '1.1.0'])).toBe(true)
    expect(conflictsWithExisting('1.2.0', ['1.0.0', '1.1.0'])).toBe(false)
    expect(conflictsWithExisting('invalid', ['1.0.0'])).toBe(true)
  })
})
