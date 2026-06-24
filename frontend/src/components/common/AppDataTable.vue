<script setup lang="ts">
import { computed, useAttrs } from 'vue'

defineOptions({
  inheritAttrs: false,
})

const props = withDefaults(
  defineProps<{
    activatable?: boolean
  }>(),
  {
    activatable: false,
  },
)

const attrs = useAttrs()

const rowClassName = computed(() => {
  const external = attrs['row-class-name']
  if (typeof external === 'function') {
    return external
  }
  if (props.activatable) {
    return () => 'app-data-table__activatable-row'
  }
  return external
})
</script>

<template>
  <el-table
    class="app-data-table"
    :class="{ 'app-data-table--activatable': activatable }"
    stripe
    border
    size="default"
    show-overflow-tooltip
    :row-class-name="rowClassName"
    v-bind="attrs"
  >
    <slot />
  </el-table>
</template>

<style scoped lang="scss">
.app-data-table {
  width: 100%;

  :deep(.el-table__header-wrapper) {
    th.el-table__cell {
      vertical-align: top;
      background: color-mix(in srgb, var(--surface-bg) 70%, white);

      .cell {
        position: relative;
        display: block;
        line-height: 1.25;
        padding-top: 0.5rem;
        padding-bottom: 0.5rem;
        white-space: normal;
        word-break: normal;
      }

      .caret-wrapper {
        position: absolute;
        top: 0.55rem;
        right: 0.25rem;
      }
    }

    th.el-table__cell:focus-visible {
      outline: 2px solid var(--color-primary);
      outline-offset: 2px;
    }
  }

  :deep(.el-table__body-wrapper) {
    td.el-table__cell .cell {
      line-height: 1.45;
    }
  }
}

.app-data-table--activatable {
  :deep(.app-data-table__activatable-row) {
    cursor: pointer;
  }

  :deep(.app-data-table__activatable-row:focus-within) {
    outline: 2px solid var(--color-primary);
    outline-offset: -2px;
  }
}
</style>
