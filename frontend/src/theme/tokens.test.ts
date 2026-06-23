import { describe, expect, it } from 'vitest'
import { applyBrandTheme, BRAND_THEMES } from '@/theme/tokens'

describe('brand theme tokens', () => {
  it('applies REDBC primary color to document root', () => {
    applyBrandTheme('REDBC')
    expect(document.documentElement.dataset.brand).toBe('REDBC')
    expect(getComputedStyle(document.documentElement).getPropertyValue('--brand-primary').trim()).toBe(
      BRAND_THEMES.REDBC.primary,
    )
  })

  it('applies GREENBC primary color to document root', () => {
    applyBrandTheme('GREENBC')
    expect(document.documentElement.dataset.brand).toBe('GREENBC')
    expect(getComputedStyle(document.documentElement).getPropertyValue('--brand-primary').trim()).toBe(
      BRAND_THEMES.GREENBC.primary,
    )
  })
})
