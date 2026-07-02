import { describe, expect, it } from 'vitest'
import { BRAND_REGISTRY } from '@/config/brands'

describe('brand registry', () => {
  it('keeps primary colors unchanged and uses neutral shell surfaces', () => {
    const redbc = BRAND_REGISTRY.find((entry) => entry.code === 'REDBC')
    const greenbc = BRAND_REGISTRY.find((entry) => entry.code === 'GREENBC')

    expect(redbc?.tokens.primary).toBe('#DB0011')
    expect(greenbc?.tokens.primary).toBe('#00847F')

    for (const entry of BRAND_REGISTRY) {
      expect(entry.tokens.headerBg).toBe('#FFFFFF')
      expect(entry.tokens.headerBorder).toBe('#E2E8F0')
      expect(entry.tokens.navBg).toBe('#FFFFFF')
    }
  })
})
