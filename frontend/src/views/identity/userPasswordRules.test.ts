import { describe, expect, it } from 'vitest'

import { createPasswordValidator } from '@/views/identity/userPasswordRules'

describe('userPasswordRules', () => {
  const t = (key: string) => key

  it('accepts passwords within configured length bounds', () => {
    const validator = createPasswordValidator(t)
    let callbackError: Error | undefined

    validator({}, 'Sup3rSecret!42', (error) => {
      callbackError = error
    })

    expect(callbackError).toBeUndefined()
  })

  it('rejects short passwords', () => {
    const validator = createPasswordValidator(t)
    let callbackError: Error | undefined

    validator({}, 'short', (error) => {
      callbackError = error
    })

    expect(callbackError?.message).toBe('identity.users.validation.passwordLength')
  })
})
