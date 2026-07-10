<script setup lang="ts">
import { computed } from 'vue'
import { Filter } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import type { TableColumnFilterOption } from '@/composables/useTableFilterOptions'

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

const hasActiveFilter = computed(() => Boolean(filterText.value?.trim()))
</script>

<template>
  <div class="table-column-filter-header">
    <span class="table-column-filter-header__label">{{ label }}</span>
    <el-popover
      trigger="click"
      placement="bottom-start"
      :width="220"
      popper-class="table-column-filter-popover"
    >
      <template #reference>
        <button
          type="button"
          class="table-column-filter-header__trigger"
          :class="{ 'table-column-filter-header__trigger--active': hasActiveFilter }"
          :aria-label="t('table.columnFilter.open', { column: label })"
        >
          <el-icon><Filter /></el-icon>
        </button>
      </template>
      <div class="table-column-filter-header__panel">
        <el-input
          v-if="props.filterType === 'text'"
          v-model="filterText"
          size="small"
          clearable
          :placeholder="t('table.filterPlaceholder')"
          :aria-label="t('table.columnFilter.open', { column: label })"
        />
        <el-select
          v-else
          v-model="filterText"
          size="small"
          clearable
          filterable
          class="table-column-filter-header__select"
          :placeholder="t('table.filterAll')"
          :aria-label="t('table.columnFilter.open', { column: label })"
        >
          <el-option
            v-for="option in props.options"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </div>
    </el-popover>
  </div>
</template>

<style scoped lang="scss">
.table-column-filter-header {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  max-width: 100%;
  min-width: 0;
}

.table-column-filter-header__label {
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.table-column-filter-header__trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.5rem;
  height: 1.5rem;
  padding: 0;
  border: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;

  &:hover,
  &:focus-visible {
    color: var(--brand-primary);
    background: color-mix(in srgb, var(--brand-primary) 8%, var(--surface-card));
  }

  &--active {
    color: var(--brand-primary);
  }

  &:focus-visible {
    outline: var(--focus-ring-width) solid var(--focus-ring-color);
    outline-offset: 1px;
  }
}

.table-column-filter-header__panel {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.table-column-filter-header__select {
  width: 100%;
}
</style>
