import { BRAND_REGISTRY, type BrandPreset } from '@/config/brands'

export interface BrandThemeTokens {
  primary: string
  primaryHover: string
  headerBg: string
  headerBorder: string
  navBg: string
  accentSoft: string
  logoSlotLabel: string
}

export type { BrandPreset } from '@/config/brands'

export const BRAND_THEMES = BRAND_REGISTRY.reduce(
  (accumulator, entry) => {
    accumulator[entry.code] = {
      primary: entry.tokens.primary,
      primaryHover: entry.tokens.primaryHover,
      headerBg: entry.tokens.headerBg,
      headerBorder: entry.tokens.headerBorder,
      navBg: entry.tokens.navBg,
      accentSoft: entry.tokens.accentSoft,
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
  root.style.setProperty('--brand-header-border', theme.headerBorder)
  root.style.setProperty('--nav-surface-bg', theme.navBg)
  root.style.setProperty('--brand-accent-soft', theme.accentSoft)
}
