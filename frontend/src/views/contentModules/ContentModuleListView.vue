<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import CatalogFilterToolbar from '@/components/common/CatalogFilterToolbar.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import EntityLinkCell from '@/components/common/EntityLinkCell.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import ContentModuleCreateDialog from '@/components/contentModules/ContentModuleCreateDialog.vue'
import { useAbortableCatalogLoader } from '@/composables/useAbortableCatalogLoader'
import { useCatalogTableControls } from '@/composables/useCatalogTableControls'
import { useCatalogPagination } from '@/composables/useCatalogPagination'
import { useActivatableTableRow } from '@/composables/useActivatableTableRow'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { useCapabilities } from '@/composables/useCapabilities'
import { useEntityLinkTargets } from '@/composables/useEntityLinkTargets'
import { CLIENT_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import { contentModuleDetailPath } from '@/routing/routeKeys'
import { useContentModulesStore } from '@/stores/contentModules'
import type { ContentModuleSummary } from '@/types/contentModule'
import { ElMessage } from 'element-plus'

const { t, te } = useI18n()
const { formatDateTime } = useLocaleFormatters()
const router = useRouter()
const contentModulesStore = useContentModulesStore()
const { authorContentModules } = useCapabilities()
const { contentModuleDetailLink } = useEntityLinkTargets()

const createDialogOpen = ref(false)
const currentPage = ref(1)

const allModules = computed(() => contentModulesStore.modules)
const {
  searchQuery,
  filters,
  activeSortKey,
  sortedRows,
  hasAnyActive,
  activeFilterChips,
  clearAll,
  removeFilterChip,
} = useCatalogTableControls(allModules, {
  searchGetters: [
    (row) => row.name,
    (row) => row.moduleCode,
    (row) => row.groupCode,
  ],
  filters: [
    {
      key: 'groupCode',
      labelKey: 'contentModules.list.columns.group',
      getValue: (row) => row.groupCode,
    },
  ],
  sortOptions: [
    {
      key: 'updatedAtDesc',
      labelKey: 'table.sort.updatedAtDesc',
      getter: (row) => row.updatedAt,
      order: 'desc',
    },
    {
      key: 'updatedAtAsc',
      labelKey: 'table.sort.updatedAtAsc',
      getter: (row) => row.updatedAt,
      order: 'asc',
    },
    {
      key: 'nameAsc',
      labelKey: 'table.sort.nameAsc',
      getter: (row) => row.name,
      order: 'asc',
    },
    {
      key: 'moduleCodeAsc',
      labelKey: 'table.sort.moduleCodeAsc',
      getter: (row) => row.moduleCode,
      order: 'asc',
    },
  ],
  defaultSortKey: 'updatedAtDesc',
})

const catalogToolbarFilters = computed(() => [
  {
    key: 'groupCode',
    labelKey: 'contentModules.list.columns.group',
    type: 'text' as const,
  },
])

const catalogSortOptions = computed(() => [
  { key: 'updatedAtDesc', labelKey: 'table.sort.updatedAtDesc' },
  { key: 'updatedAtAsc', labelKey: 'table.sort.updatedAtAsc' },
  { key: 'nameAsc', labelKey: 'table.sort.nameAsc' },
  { key: 'moduleCodeAsc', labelKey: 'table.sort.moduleCodeAsc' },
])

const { paginatedRows: paginatedModules, totalRows: totalModuleRows } = useCatalogPagination(
  sortedRows,
  currentPage,
  CLIENT_TABLE_PAGE_SIZE,
)

watch(hasAnyActive, () => {
  currentPage.value = 1
})

const canCreate = computed(() => authorContentModules.value)
const errorMessage = computed(() => {
  const key = contentModulesStore.lastErrorMessageKey
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('contentModules.error.loadList')
})

const { reload: reloadModules } = useAbortableCatalogLoader((signal) =>
  contentModulesStore.fetchModules(undefined, { signal }),
)

onMounted(async () => {
  await reloadModules()
})

function openModule(moduleId: string) {
  router.push(contentModuleDetailPath(moduleId))
}

const { onRowClick: activateModuleRow } = useActivatableTableRow<ContentModuleSummary>((row) =>
  openModule(row.moduleId),
)

function handleCreated(moduleId: string) {
  ElMessage.success(t('contentModules.create.success'))
  router.push(contentModuleDetailPath(moduleId))
}
</script>

<template>
  <AppPageLayout layout-variant="fluid">
    <PageHeader
      :title="t('contentModules.list.title')"
      :description="t('contentModules.list.description')"
      :help-text="t('packageCatalog.contentModule.noticeDescription')"
    >
      <template #actions>
        <el-button v-if="canCreate" type="primary" @click="createDialogOpen = true">
          {{ t('contentModules.create.open') }}
        </el-button>
      </template>
    </PageHeader>

    <LoadErrorPanel
      v-if="contentModulesStore.lastErrorMessageKey && !contentModulesStore.loadingList"
      :message-key="contentModulesStore.lastErrorMessageKey"
      :retryable="contentModulesStore.lastListErrorRetryable"
      @retry="reloadModules"
    />

    <el-skeleton v-else-if="contentModulesStore.loadingList" :rows="6" animated />

    <template v-else-if="allModules.length > 0">
      <CatalogFilterToolbar
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

      <template v-if="sortedRows.length > 0">
        <AppDataTable activatable :data="paginatedModules" @row-click="activateModuleRow">
          <el-table-column
            prop="groupCode"
            :label="t('contentModules.list.columns.group')"
            width="140"
          />
          <el-table-column
            prop="moduleCode"
            :label="t('contentModules.list.columns.moduleCode')"
            min-width="180"
            show-overflow-tooltip
          />
          <el-table-column
            :label="t('contentModules.list.columns.name')"
            min-width="220"
          >
            <template #default="{ row }">
              <EntityLinkCell
                :label="row.name"
                :subtitle="row.moduleCode"
                :to="contentModuleDetailLink(row.moduleId)"
              />
            </template>
          </el-table-column>
          <el-table-column :label="t('contentModules.list.columns.updatedAt')" width="200">
            <template #default="{ row }">
              {{ formatDateTime(row.updatedAt) }}
            </template>
          </el-table-column>
        </AppDataTable>
        <AppTablePagination
          v-model:current-page="currentPage"
          :page-size="CLIENT_TABLE_PAGE_SIZE"
          :total="totalModuleRows"
        />
      </template>

      <EmptyStatePanel
        v-else
        title-key="contentModules.list.empty"
        description-key="contentModules.list.emptyDescription"
      />
    </template>

    <EmptyStatePanel
      v-else-if="!contentModulesStore.loadingList && !errorMessage"
      title-key="contentModules.list.empty"
      description-key="contentModules.list.emptyDescription"
    >
      <template v-if="canCreate" #actions>
        <el-button type="primary" @click="createDialogOpen = true">
          {{ t('contentModules.create.open') }}
        </el-button>
      </template>
    </EmptyStatePanel>

    <ContentModuleCreateDialog v-model="createDialogOpen" @created="handleCreated" />
  </AppPageLayout>
</template>

<style scoped lang="scss">
.page-alert {
  margin-bottom: var(--space-4);
}
</style>
