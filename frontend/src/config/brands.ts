export interface BrandRegistryEntry {
  code: 'REDBC' | 'GREENBC'
  labelKey: string
  logoSlotLabel: string
  tokens: {
    primary: string
    primaryHover: string
    headerBg: string
    headerBorder: string
    navBg: string
    accentSoft: string
  }
}

export const BRAND_REGISTRY = [
  {
    code: 'REDBC',
    labelKey: 'brand.redbc',
    logoSlotLabel: 'REDBC',
    tokens: {
      primary: '#DB0011',
      primaryHover: '#AF0010',
      headerBg: '#FFFFFF',
      headerBorder: '#E2E8F0',
      navBg: '#FFFFFF',
      accentSoft: '#FCE8EB',
    },
  },
  {
    code: 'GREENBC',
    labelKey: 'brand.greenbc',
    logoSlotLabel: 'GREENBC',
    tokens: {
      primary: '#00847F',
      primaryHover: '#006A66',
      headerBg: '#FFFFFF',
      headerBorder: '#E2E8F0',
      navBg: '#FFFFFF',
      accentSoft: '#E4F5F4',
    },
  },
] as const satisfies readonly BrandRegistryEntry[]

export type BrandPreset = (typeof BRAND_REGISTRY)[number]['code']

export const DEFAULT_BRAND_PRESET: BrandPreset = 'REDBC'

const BRAND_CODES = new Set<string>(BRAND_REGISTRY.map((entry) => entry.code))

export function isBrandPreset(value: string): value is BrandPreset {
  return BRAND_CODES.has(value)
}
