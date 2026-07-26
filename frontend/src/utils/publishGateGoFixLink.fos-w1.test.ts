import { describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import en from '@/i18n/locales/en'
import { resolvePublishGateGoFixQuery } from '@/utils/publishGateGoFixLink'

describe('FOS-W1-2 publish-gate labels and go-fix', () => {
  it('maps nesting cycle go-fix to content modules design tab', () => {
    const q = resolvePublishGateGoFixQuery('CONTENT_MODULE_NESTING_CYCLE')
    expect(q).toEqual({ workspaceTab: 'design', designTab: 'contentModules' })
  })

  it('resolves human label for nesting cycle messageKey', () => {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const key = 'api.publishGate.contentModuleNestingCycle.blocked'
    expect(i18n.global.te(key)).toBe(true)
    const label = String(i18n.global.t(key))
    expect(label).not.toMatch(/=/)
    expect(label.toLowerCase()).toContain('cycle')
  })
})
