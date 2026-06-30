import { describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import en from '@/i18n/locales/en'
import { formatMasterRevisionLineLabel } from '@/utils/masterRevisionLineLabel'

describe('formatMasterRevisionLineLabel', () => {
  const i18n = createI18n({
    legacy: false,
    locale: 'en',
    messages: { en },
  })
  const t = i18n.global.t

  it('prefers revision sequence when present', () => {
    expect(formatMasterRevisionLineLabel(t, 'HISTORICAL', 1)).toBe('Revision 1')
  })

  it('maps CURRENT and HISTORICAL labels without sequence', () => {
    expect(formatMasterRevisionLineLabel(t, 'CURRENT')).toBe('Current revision')
    expect(formatMasterRevisionLineLabel(t, 'HISTORICAL')).toBe('Historical revision')
  })
})
