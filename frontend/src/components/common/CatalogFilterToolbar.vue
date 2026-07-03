<script setup lang="ts">
import { Search } from '@element-plus/icons-vue'
import { useI18n } from 'vue-i18n'
import type { CatalogFilterChip } from '@/composables/useCatalogTableControls'
import type { TableColumnFilterOption } from '@/composables/useTableFilterOptions'

export interface CatalogToolbarFilter {
  key: string
  labelKey: string
  type: 'text' | 'select'
  options?: TableColumnFilterOption[]
}

export interface CatalogToolbarSortOption {
  key: string
  labelKey: string
}

const props = defineProps<{
  filters: CatalogToolbarFilter[]
  sortOptions: CatalogToolbarSortOption[]
  activeFilterChips: CatalogFilterChip[]
  hasAnyActive: boolean
}>()

const searchQuery = defineModel<string>('searchQuery', { default: '' })
const filterValues = defineModel<Record<string, string>>('filterValues', { required: true })
const activeSortKey = defineModel<string>('activeSortKey', { required: true })

const emit = defineEmits<{
  clear: []
  removeChip: [key: string]
}>()

const { t } = useI18n()
</script>

<template>
  <div class="catalog-filter-toolbar">
    <div class="catalog-filter-toolbar__row">
      <el-input
        v-model="searchQuery"
        class="catalog-filter-toolbar__search"
        clearable
        :placeholder="t('table.searchPlaceholder')"
        :prefix-icon="Search"
      />

      <template v-for="filter in props.filters" :key="filter.key">
        <el-input
          v-if="filter.type === 'text'"
          v-model="filterValues[filter.key]"
          class="catalog-filter-toolbar__control"
          clearable
          :placeholder="t(filter.labelKey)"
        />
        <el-select
          v-else
          v-model="filterValues[filter.key]"
          class="catalog-filter-toolbar__control"
          clearable
          filterable
          :placeholder="t(filter.labelKey)"
        >
          <el-option
            v-for="option in filter.options ?? []"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </template>

      <el-select
        v-model="activeSortKey"
        class="catalog-filter-toolbar__sort"
        :placeholder="t('table.sortBy')"
      >
        <el-option
          v-for="option in props.sortOptions"
          :key="option.key"
          :label="t(option.labelKey)"
          :value="option.key"
        />
      </el-select>

      <el-button v-if="props.hasAnyActive" text @click="emit('clear')">
        {{ t('table.clearAllFilters') }}
      </el-button>
    </div>

    <div v-if="props.activeFilterChips.length > 0" class="catalog-filter-toolbar__chips">
      <el-tag
        v-for="chip in props.activeFilterChips"
        :key="chip.key"
        size="small"
        closable
        @close="emit('removeChip', chip.key)"
      >
        <template v-if="chip.key === '__search__'">
          {{ t(chip.labelKey, { value: chip.value }) }}
        </template>
        <template v-else>
          {{ t(chip.labelKey) }}: {{ chip.value }}
        </template>
      </el-tag>
    </div>
  </div>
</template>

<style scoped lang="scss">
.catalog-filter-toolbar {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
  padding: var(--space-4);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  background: var(--surface-card);
  box-shadow: var(--shadow-sm);
}

.catalog-filter-toolbar__row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
}

.catalog-filter-toolbar__search {
  width: min(100%, 320px);
}

.catalog-filter-toolbar__control {
  width: min(100%, 180px);
}

.catalog-filter-toolbar__sort {
  width: min(100%, 200px);
}
</style>
