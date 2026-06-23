import { BRAND_REGISTRY, type BrandPreset } from '@/config/brands'

export interface BrandThemeTokens {
  primary: string
  primaryHover: string
  headerBg: string
  logoSlotLabel: string
}

export type { BrandPreset } from '@/config/brands'

export const BRAND_THEMES = BRAND_REGISTRY.reduce(
  (accumulator, entry) => {
    accumulator[entry.code] = {
      primary: entry.tokens.primary,
      primaryHover: entry.tokens.primaryHover,
      headerBg: entry.tokens.headerBg,
      logoSlotLabel: entry.logoSlotLabel,
    }
    return accumulator
  },
  {} as Record<BrandPreset, BrandThemeTokens>,
)

export function applyBrandTheme(preset: BrandPreset): void {
  const theme = BRAND_THEMES[preset]
  const root = document.documentElement
  root.dataset.brand = preset
  root.style.setProperty('--brand-primary', theme.primary)
  root.style.setProperty('--brand-primary-hover', theme.primaryHover)
  root.style.setProperty('--brand-header-bg', theme.headerBg)
}
