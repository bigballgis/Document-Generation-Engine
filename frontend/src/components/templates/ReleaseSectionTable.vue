<script setup lang="ts">
import AppDataTable from '@/components/common/AppDataTable.vue'

defineProps<{
  title: string
  columns: Array<{ prop: string; label: string }>
  data: unknown[]
  emptyText: string
}>()
</script>

<template>
  <el-card shadow="never" class="release-section-card">
    <template #header>
      <span>{{ title }}</span>
    </template>
    <AppDataTable :data="data">
      <template #empty>
        <el-empty :description="emptyText" />
      </template>
      <el-table-column
        v-for="col in columns"
        :key="col.prop"
        :prop="col.prop"
        :label="col.label"
      />
    </AppDataTable>
  </el-card>
</template>

<style scoped lang="scss">
.release-section-card {
  margin-bottom: 1rem;
}
</style>
