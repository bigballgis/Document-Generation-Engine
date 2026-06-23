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
}
</style>
