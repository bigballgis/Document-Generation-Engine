export type BrandPreset = 'REDBC' | 'GREENBC'

export interface BrandThemeTokens {
  name: BrandPreset
  primary: string
  primaryHover: string
  headerBg: string
  logoSlotLabel: string
}

export const BRAND_THEMES: Record<BrandPreset, BrandThemeTokens> = {
  REDBC: {
    name: 'REDBC',
    primary: '#DB0011',
    primaryHover: '#AF0010',
    headerBg: '#FFFFFF',
    logoSlotLabel: 'REDBC',
  },
  GREENBC: {
    name: 'GREENBC',
    primary: '#00847F',
    primaryHover: '#006A66',
    headerBg: '#FFFFFF',
    logoSlotLabel: 'GREENBC',
  },
}

export function applyBrandTheme(preset: BrandPreset): void {
  const theme = BRAND_THEMES[preset]
  const root = document.documentElement
  root.dataset.brand = preset
  root.style.setProperty('--brand-primary', theme.primary)
  root.style.setProperty('--brand-primary-hover', theme.primaryHover)
  root.style.setProperty('--brand-header-bg', theme.headerBg)
}
