import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { BrandPreset } from '@/theme/tokens'
import { applyBrandTheme } from '@/theme/tokens'

export const useAppStore = defineStore('app', () => {
  const brand = ref<BrandPreset>('REDBC')

  function setBrand(preset: BrandPreset) {
    brand.value = preset
    applyBrandTheme(preset)
  }

  setBrand(brand.value)

  return { brand, setBrand }
})
