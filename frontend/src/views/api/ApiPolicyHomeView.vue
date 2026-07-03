<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import AppDataTable from '@/components/common/AppDataTable.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import CatalogFilterToolbar from '@/components/common/CatalogFilterToolbar.vue'
import TemplateStatusBadge from '@/components/templates/TemplateStatusBadge.vue'
import { useActivatableTableRow } from '@/composables/useActivatableTableRow'
import { useCatalogTableControls } from '@/composables/useCatalogTableControls'
import { useCatalogPagination } from '@/composables/useCatalogPagination'
import { useLifecycleStatusFilterOptions } from '@/composables/useTableFilterOptions'
import { CLIENT_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import { apiPolicyDetailPath } from '@/routing/routeKeys'
import { useTemplatesStore } from '@/stores/templates'
import type { TemplateSummary } from '@/types/template'

const { t, te } = useI18n()
const lifecycleStatusFilterOptions = useLifecycleStatusFilterOptions()
const router = useRouter()
const templatesStore = useTemplatesStore()
const currentPage = ref(1)

const publishedTemplates = computed(() => templatesStore.publishedTemplates)
const {
  searchQuery,
  filters,
  activeSortKey,
  sortedRows,
  hasAnyActive,
  activeFilterChips,
  clearAll,
  removeFilterChip,
} = useCatalogTableControls(publishedTemplates, {
  searchGetters: [
    (row) => row.name,
    (row) => row.externalId,
    (row) => row.groupCode,
  ],
  filters: [
    {
      key: 'groupCode',
      labelKey: 'apiPolicy.home.groupCode',
      getValue: (row) => row.groupCode,
    },
    {
      key: 'status',
      labelKey: 'templates.list.columns.status',
      getValue: (row) => row.lifecycleStatus,
      matchMode: 'exact',
    },
  ],
  sortOptions: [
    {
      key: 'nameAsc',
      labelKey: 'table.sort.nameAsc',
      getter: (row) => row.name,
      order: 'asc',
    },
    {
      key: 'externalIdAsc',
      labelKey: 'table.sort.externalIdAsc',
      getter: (row) => row.externalId,
      order: 'asc',
    },
  ],
  defaultSortKey: 'nameAsc',
})

const catalogToolbarFilters = computed(() => [
  { key: 'groupCode', labelKey: 'apiPolicy.home.groupCode', type: 'text' as const },
  {
    key: 'status',
    labelKey: 'templates.list.columns.status',
    type: 'select' as const,
    options: lifecycleStatusFilterOptions.value,
  },
])

const catalogSortOptions = computed(() => [
  { key: 'nameAsc', labelKey: 'table.sort.nameAsc' },
  { key: 'externalIdAsc', labelKey: 'table.sort.externalIdAsc' },
])

const { paginatedRows: paginatedTemplates, totalRows: totalTemplateRows } = useCatalogPagination(
  sortedRows,
  currentPage,
  CLIENT_TABLE_PAGE_SIZE,
)

watch(hasAnyActive, () => {
  currentPage.value = 1
})

const errorMessage = computed(() => {
  const key = templatesStore.lastErrorMessageKey
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('templates.error.loadList')
})

onMounted(async () => {
  try {
    await templatesStore.fetchTemplates()
  } catch {
    // Error surfaced via store message key.
  }
})

function openTemplate(templateId: string) {
  router.push(apiPolicyDetailPath(templateId))
}

const { onRowClick: activateTemplateRow } = useActivatableTableRow<TemplateSummary>((row) =>
  openTemplate(row.id),
)
</script>

<template>
  <AppPageLayout>
    <PageHeader
      :title="t('apiPolicy.home.title')"
      :description="t('apiPolicy.home.description')"
    />

    <el-alert
      v-if="errorMessage"
      class="page-alert"
      type="error"
      :title="errorMessage"
      show-icon
      :closable="false"
    />

    <el-skeleton v-if="templatesStore.loadingList" :rows="5" animated />

    <template v-else>
      <CatalogFilterToolbar
        v-if="publishedTemplates.length > 0"
        v-model:search-query="searchQuery"
        v-model:filter-values="filters"
        v-model:active-sort-key="activeSortKey"
        :filters="catalogToolbarFilters"
        :sort-options="catalogSortOptions"
        :active-filter-chips="activeFilterChips"
        :has-any-active="hasAnyActive"
        @clear="clearAll"
        @remove-chip="removeFilterChip"
      />

      <AppDataTable activatable :data="paginatedTemplates" @row-click="activateTemplateRow">
        <template #empty>
          <EmptyStatePanel title-key="apiPolicy.home.empty" />
        </template>
        <el-table-column prop="name" :label="t('templates.list.columns.name')" min-width="220" />
        <el-table-column
          prop="externalId"
          :label="t('templates.list.columns.externalId')"
          min-width="180"
        />
        <el-table-column
          prop="groupCode"
          :label="t('apiPolicy.home.groupCode')"
          width="140"
        />
        <el-table-column :label="t('templates.list.columns.status')" width="140">
          <template #default="{ row }">
            <TemplateStatusBadge :status="row.lifecycleStatus" />
          </template>
        </el-table-column>
        <el-table-column
          prop="releaseVersion"
          :label="t('templates.list.columns.releaseVersion')"
          width="140"
        />
      </AppDataTable>
      <AppTablePagination
        v-if="sortedRows.length > 0"
        v-model:current-page="currentPage"
        :page-size="CLIENT_TABLE_PAGE_SIZE"
        :total="totalTemplateRows"
      />
    </template>
  </AppPageLayout>
</template>

<style scoped lang="scss">
.page-alert {
  margin-bottom: var(--space-4);
}
</style>
