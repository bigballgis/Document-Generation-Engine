<script setup lang="ts">
defineOptions({
  inheritAttrs: false,
})
</script>

<template>
  <el-select filterable v-bind="$attrs">
    <slot />
  </el-select>
</template>
