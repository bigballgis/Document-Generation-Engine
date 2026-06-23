<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

defineProps<{
  label: string
}>()

const filterText = defineModel<string>({ default: '' })
</script>

<template>
  <div class="table-column-header">
    <span class="table-column-header__label">{{ label }}</span>
    <el-input
      v-model="filterText"
      size="small"
      clearable
      :placeholder="t('table.filterPlaceholder')"
      class="table-column-header__filter"
    />
  </div>
</template>

<style scoped lang="scss">
.table-column-header {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  padding-top: 0.15rem;
}

.table-column-header__label {
  font-size: 0.8125rem;
  font-weight: 600;
  line-height: 1.2;
}

.table-column-header__filter {
  width: 100%;
}
</style>
