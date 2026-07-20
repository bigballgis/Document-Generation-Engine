<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    maxWidth?: string
    layoutVariant?: 'contained' | 'fluid'
    /** `panel` = full-width white workspace surface; `canvas` = page gray (legacy). */
    contentSurface?: 'canvas' | 'panel'
  }>(),
  {
    maxWidth: '1440px',
    // SYS-NORM Wave 1: management pages are fluid system-wide; contained is opt-in.
    layoutVariant: 'fluid',
    contentSurface: 'panel',
  },
)

const layoutClass = computed(() => [
  props.layoutVariant === 'fluid'
    ? 'app-page-layout--fluid'
    : 'app-page-layout--contained',
  props.contentSurface === 'panel'
    ? 'app-page-layout--panel'
    : 'app-page-layout--canvas',
])

const innerStyle = computed(() => {
  if (props.layoutVariant === 'fluid') {
    return undefined
  }
  return {
    maxWidth: props.maxWidth,
  }
})
</script>

<template>
  <div class="app-page-layout" :class="layoutClass">
    <div v-if="layoutVariant === 'contained'" class="app-page-layout__inner" :style="innerStyle">
      <slot />
    </div>
    <slot v-else />
  </div>
</template>

<style scoped lang="scss">
.app-page-layout {
  width: 100%;
  flex: 1 1 auto;
  min-height: 100%;
  box-sizing: border-box;

  &--contained,
  &--fluid {
    padding: var(--space-8);
  }

  &--panel {
    background: var(--surface-card);
  }

  &--canvas {
    background: var(--surface-page);
  }

  &__inner {
    width: 100%;
    margin: 0 auto;
  }
}
</style>
