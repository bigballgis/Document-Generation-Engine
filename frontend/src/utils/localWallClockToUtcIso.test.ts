import { describe, expect, it } from 'vitest'
import { localWallClockToUtcIso } from '@/utils/localWallClockToUtcIso'

describe('localWallClockToUtcIso (FOS-W5-1)', () => {
  it('converts local wall-clock digits to UTC ISO that round-trips to the same local instant', () => {
    const result = localWallClockToUtcIso('2026-07-26T15:00:00')
    expect(result).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/)
    const back = new Date(result!)
    expect(back.getFullYear()).toBe(2026)
    expect(back.getMonth()).toBe(6)
    expect(back.getDate()).toBe(26)
    expect(back.getHours()).toBe(15)
    expect(back.getMinutes()).toBe(0)
    expect(back.getSeconds()).toBe(0)
  })

  it('strips legacy literal Z and still treats digits as local wall-clock', () => {
    const withLie = localWallClockToUtcIso('2026-07-26T15:00:00Z')
    const honest = localWallClockToUtcIso('2026-07-26T15:00:00')
    expect(withLie).toBe(honest)
  })

  it('returns null for empty input', () => {
    expect(localWallClockToUtcIso('')).toBeNull()
    expect(localWallClockToUtcIso(null)).toBeNull()
  })
})
