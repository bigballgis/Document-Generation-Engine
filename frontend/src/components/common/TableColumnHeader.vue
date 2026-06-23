<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { TableColumnFilterOption } from '@/composables/useTableFilterOptions'

export type { TableColumnFilterOption }

const props = withDefaults(
  defineProps<{
    label: string
    filterType?: 'text' | 'select'
    options?: TableColumnFilterOption[]
  }>(),
  {
    filterType: 'text',
    options: () => [],
  },
)

const { t } = useI18n()
const filterText = defineModel<string>({ default: '' })
</script>

<template>
  <div class="table-column-header">
    <div class="table-column-header__title">{{ label }}</div>
    <el-input
      v-if="props.filterType === 'text'"
      v-model="filterText"
      size="small"
      clearable
      :placeholder="t('table.filterPlaceholder')"
      class="table-column-header__control"
    />
    <el-select
      v-else
      v-model="filterText"
      size="small"
      clearable
      filterable
      class="table-column-header__control"
      :placeholder="t('table.filterAll')"
    >
      <el-option
        v-for="option in props.options"
        :key="option.value"
        :label="option.label"
        :value="option.value"
      />
    </el-select>
  </div>
</template>

<style scoped lang="scss">
.table-column-header {
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
  width: 100%;
  min-width: 0;
  padding-right: 1.125rem;
}

.table-column-header__title {
  font-size: 0.8125rem;
  font-weight: 600;
  line-height: 1.25;
  color: var(--text-primary);
  white-space: nowrap;
}

.table-column-header__control {
  width: 100%;
}
</style>
