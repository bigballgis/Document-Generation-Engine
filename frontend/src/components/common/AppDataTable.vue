<script setup lang="ts">
defineOptions({
  inheritAttrs: false,
})
</script>

<template>
  <el-table
    class="app-data-table"
    stripe
    border
    size="default"
    show-overflow-tooltip
    v-bind="$attrs"
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
  }

  :deep(.el-table__body-wrapper) {
    td.el-table__cell .cell {
      line-height: 1.45;
    }
  }
}
</style>
