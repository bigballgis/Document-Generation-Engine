<script setup lang="ts">
const currentPage = defineModel<number>('currentPage', { required: true })

defineProps<{
  pageSize: number
  total: number
}>()
</script>

<template>
  <el-pagination
    v-if="total > pageSize"
    v-model:current-page="currentPage"
    class="list-pagination"
    layout="total, prev, pager, next"
    :page-size="pageSize"
    :total="total"
  />
</template>

<style scoped lang="scss">
.list-pagination {
  margin-top: 1rem;
  justify-content: flex-end;
}
</style>
