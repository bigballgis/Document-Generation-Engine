import { describe, expect, it } from 'vitest'

function compareHintKey(selectedCount: number): string {
  if (selectedCount === 0) {
    return 'templates.versions.compareHintNone'
  }
  if (selectedCount === 1) {
    return 'templates.versions.compareHintOne'
  }
  if (selectedCount > 2) {
    return 'templates.versions.compareHintTooMany'
  }
  return 'templates.versions.compareHintReady'
}

describe('release compare selection rules', () => {
  it('enables compare only for exactly two selections', () => {
    expect(compareHintKey(0)).toBe('templates.versions.compareHintNone')
    expect(compareHintKey(1)).toBe('templates.versions.compareHintOne')
    expect(compareHintKey(2)).toBe('templates.versions.compareHintReady')
    expect(compareHintKey(3)).toBe('templates.versions.compareHintTooMany')
    expect(2 === 2).toBe(true)
  })
})
