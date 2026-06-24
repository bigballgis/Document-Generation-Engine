<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    maxWidth?: string
  }>(),
  {
    maxWidth: undefined,
  },
)

const layoutStyle = computed(() => {
  if (!props.maxWidth) {
    return undefined
  }
  return { maxWidth: props.maxWidth }
})
</script>

<template>
  <div class="app-page-layout" :style="layoutStyle">
    <slot />
  </div>
</template>

<style scoped lang="scss">
.app-page-layout {
  padding: 2rem;
}
</style>
