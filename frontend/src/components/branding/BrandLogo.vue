<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { BRAND_REGISTRY, type BrandPreset } from '@/config/brands'
import greenbcLogo from '@/assets/brands/greenbc-logo.svg'
import redbcLogo from '@/assets/brands/redbc-logo.svg'

const props = withDefaults(
  defineProps<{
    brand: BrandPreset
    size?: number
    showWordmark?: boolean
  }>(),
  {
    size: 40,
    showWordmark: false,
  },
)

const { t } = useI18n()

const logoSrc = computed(() => (props.brand === 'REDBC' ? redbcLogo : greenbcLogo))

const wordmarkLabel = computed(() => {
  const entry = BRAND_REGISTRY.find((item) => item.code === props.brand)
  return entry ? t(entry.labelKey) : props.brand
})
</script>

<template>
  <div class="brand-logo" :class="`brand-${brand}`">
    <img
      class="brand-logo__mark"
      :src="logoSrc"
      :width="size"
      :height="size"
      alt=""
    />
    <span v-if="showWordmark" class="brand-logo__wordmark">{{ wordmarkLabel }}</span>
  </div>
</template>

<style scoped lang="scss">
.brand-logo {
  display: inline-flex;
  align-items: center;
  gap: 0.65rem;
}

.brand-logo__mark {
  display: block;
  flex-shrink: 0;
  border-radius: 10px;
  box-shadow:
    0 1px 2px rgba(15, 23, 42, 0.08),
    0 4px 12px color-mix(in srgb, var(--brand-primary) 18%, transparent);
}

.brand-logo__wordmark {
  font-size: 0.8125rem;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: var(--brand-primary);
}
</style>
