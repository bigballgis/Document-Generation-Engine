import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  APP_LOCALE_STORAGE_KEY,
  ensureLocaleMessages,
  resolveAppLocale,
  type AppLocale,
} from '@/i18n/localeRegistry'
import { i18n } from '@/i18n'
import type { BrandPreset } from '@/theme/tokens'
import { applyBrandTheme } from '@/theme/tokens'
import {
  DEFAULT_BRAND_PRESET,
  isBrandPreset,
} from '@/config/brands'

const APP_BRAND_STORAGE_KEY = 'docgen.app.brand'

export const useAppStore = defineStore('app', () => {
  const initialBrand = localStorage.getItem(APP_BRAND_STORAGE_KEY)
  const brand = ref<BrandPreset>(
    initialBrand && isBrandPreset(initialBrand) ? initialBrand : DEFAULT_BRAND_PRESET,
  )
  const locale = ref<AppLocale>(resolveAppLocale(localStorage.getItem(APP_LOCALE_STORAGE_KEY)))

  function setBrand(preset: BrandPreset) {
    brand.value = preset
    applyBrandTheme(preset)
    localStorage.setItem(APP_BRAND_STORAGE_KEY, preset)
  }

  async function setLocale(nextLocale: AppLocale) {
    await ensureLocaleMessages(i18n, nextLocale)
    locale.value = nextLocale
    ;(i18n.global.locale as unknown as { value: string }).value = nextLocale
    document.documentElement.lang = nextLocale
    localStorage.setItem(APP_LOCALE_STORAGE_KEY, nextLocale)
  }

  async function initializePreferences() {
    setBrand(brand.value)
    await setLocale(locale.value)
  }

  return { brand, locale, setBrand, setLocale, initializePreferences }
})
