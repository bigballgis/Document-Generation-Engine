<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<template>
  <el-empty :description="title">
    <p v-if="description" class="empty-description">{{ description }}</p>
  </el-empty>
</template>

<style scoped lang="scss">
.empty-description {
  margin: 0.5rem 0 0;
  color: var(--text-muted);
}
</style>
