<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    maxWidth?: string
  }>(),
  {
    maxWidth: '1440px',
  },
)

const layoutStyle = computed(() => ({
  maxWidth: props.maxWidth,
}))
</script>

<template>
  <div class="app-page-layout" :style="layoutStyle">
    <slot />
  </div>
</template>

<style scoped lang="scss">
.app-page-layout {
  width: 100%;
  margin: 0 auto;
  padding: var(--space-8);
  background: var(--surface-page);
  box-sizing: border-box;
}
</style>
