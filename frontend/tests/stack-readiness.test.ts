import { afterEach, describe, expect, it } from 'vitest'
import { mustFailWhenStackMissing } from '../e2e/helpers/stack-readiness'

describe('mustFailWhenStackMissing (FOS-W12-3)', () => {
  const keys = ['CI', 'E2E_REQUIRE_STACK', 'E2E_ALLOW_STACK_SKIP'] as const
  const previous: Record<string, string | undefined> = {}

  afterEach(() => {
    for (const key of keys) {
      if (previous[key] === undefined) {
        delete process.env[key]
      } else {
        process.env[key] = previous[key]
      }
    }
  })

  function capture() {
    for (const key of keys) {
      previous[key] = process.env[key]
      delete process.env[key]
    }
  }

  it('fails closed in CI', () => {
    capture()
    process.env.CI = 'true'
    expect(mustFailWhenStackMissing()).toBe(true)
  })

  it('allows local skip when E2E_ALLOW_STACK_SKIP=1 even in CI', () => {
    capture()
    process.env.CI = 'true'
    process.env.E2E_ALLOW_STACK_SKIP = '1'
    expect(mustFailWhenStackMissing()).toBe(false)
  })

  it('defaults to skip-friendly outside CI', () => {
    capture()
    expect(mustFailWhenStackMissing()).toBe(false)
  })
})
