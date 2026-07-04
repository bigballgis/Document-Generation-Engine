<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import CatalogFilterToolbar from '@/components/common/CatalogFilterToolbar.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import MasterStatusBadge from '@/components/masters/MasterStatusBadge.vue'
import MasterUploadDialog from '@/components/masters/MasterUploadDialog.vue'
import { useAbortableCatalogLoader } from '@/composables/useAbortableCatalogLoader'
import { useCatalogTableControls } from '@/composables/useCatalogTableControls'
import { useCatalogPagination } from '@/composables/useCatalogPagination'
import { useActivatableTableRow } from '@/composables/useActivatableTableRow'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { useMasterStatusFilterOptions } from '@/composables/useTableFilterOptions'
import { useCapabilities } from '@/composables/useCapabilities'
import { CLIENT_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import { MASTER_DETAIL_PATH_PREFIX } from '@/routing/routeKeys'
import { useMastersStore } from '@/stores/masters'
import type { MasterDocumentSummary } from '@/types/master'
import { ElMessage } from 'element-plus'

const { t, te } = useI18n()
const { formatDateTime } = useLocaleFormatters()
const masterStatusFilterOptions = useMasterStatusFilterOptions()
const router = useRouter()
const mastersStore = useMastersStore()

const uploadDialogOpen = ref(false)
const currentPage = ref(1)

const allMasters = computed(() => mastersStore.masters)
const {
  searchQuery,
  filters,
  activeSortKey,
  sortedRows,
  hasAnyActive,
  activeFilterChips,
  clearAll,
  removeFilterChip,
} = useCatalogTableControls(allMasters, {
  searchGetters: [(row) => row.name, (row) => row.groupCode],
  filters: [
    {
      key: 'groupCode',
      labelKey: 'masters.list.columns.group',
      getValue: (row) => row.groupCode,
    },
    {
      key: 'status',
      labelKey: 'masters.list.columns.status',
      getValue: (row) => row.status,
      matchMode: 'exact',
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
      key: 'groupAsc',
      labelKey: 'table.sort.groupAsc',
      getter: (row) => row.groupCode,
      order: 'asc',
    },
  ],
  defaultSortKey: 'updatedAtDesc',
})

const catalogToolbarFilters = computed(() => [
  {
    key: 'groupCode',
    labelKey: 'masters.list.columns.group',
    type: 'text' as const,
  },
  {
    key: 'status',
    labelKey: 'masters.list.columns.status',
    type: 'select' as const,
    options: masterStatusFilterOptions.value,
  },
])

const catalogSortOptions = computed(() => [
  { key: 'updatedAtDesc', labelKey: 'table.sort.updatedAtDesc' },
  { key: 'updatedAtAsc', labelKey: 'table.sort.updatedAtAsc' },
  { key: 'nameAsc', labelKey: 'table.sort.nameAsc' },
  { key: 'groupAsc', labelKey: 'table.sort.groupAsc' },
])

const { paginatedRows: paginatedMasters, totalRows: totalMasterRows } = useCatalogPagination(
  sortedRows,
  currentPage,
  CLIENT_TABLE_PAGE_SIZE,
)

watch(hasAnyActive, () => {
  currentPage.value = 1
})

const { manageMasters } = useCapabilities()
const canUpload = computed(() => manageMasters.value)
const errorMessage = computed(() => {
  const key = mastersStore.lastErrorMessageKey
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('masters.error.loadList')
})

const { reload: reloadMasters } = useAbortableCatalogLoader((signal) =>
  mastersStore.fetchMasters({ signal }),
)

onMounted(async () => {
  await reloadMasters()
})

function openMaster(masterId: string) {
  router.push(`${MASTER_DETAIL_PATH_PREFIX}${masterId}`)
}

const { onRowClick: activateMasterRow } = useActivatableTableRow<MasterDocumentSummary>((row) =>
  openMaster(row.id),
)

async function handleUpload(payload: {
  groupCode: string
  name: string
  description: string
  file: File
}) {
  try {
    const created = await mastersStore.uploadMaster(
      {
        groupCode: payload.groupCode,
        name: payload.name,
        description: payload.description || undefined,
      },
      payload.file,
    )
    uploadDialogOpen.value = false
    ElMessage.success(t('masters.upload.success'))
    router.push(`${MASTER_DETAIL_PATH_PREFIX}${created.id}`)
  } catch {
    ElMessage.error(errorMessage.value || t('masters.error.upload'))
  }
}
</script>

<template>
  <AppPageLayout>
    <PageHeader
      :title="t('masters.list.title')"
      :description="t('masters.list.description')"
      :help-text="t('packageCatalog.master.noticeDescription')"
    >
      <template #actions>
        <el-button v-if="canUpload" type="primary" @click="uploadDialogOpen = true">
          {{ t('masters.upload.open') }}
        </el-button>
      </template>
    </PageHeader>

    <LoadErrorPanel
      v-if="mastersStore.lastErrorMessageKey && !mastersStore.loadingList"
      :message-key="mastersStore.lastErrorMessageKey"
      :retryable="mastersStore.lastListErrorRetryable"
      @retry="reloadMasters"
    />

    <el-skeleton v-else-if="mastersStore.loadingList" :rows="6" animated />

    <template v-else-if="allMasters.length > 0">
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
        <AppDataTable activatable :data="paginatedMasters" @row-click="activateMasterRow">
          <el-table-column
            prop="groupCode"
            :label="t('masters.list.columns.group')"
            width="140"
          />
          <el-table-column
            prop="name"
            :label="t('masters.list.columns.name')"
            min-width="220"
            show-overflow-tooltip
          />
          <el-table-column :label="t('masters.list.columns.status')" width="160">
            <template #default="{ row }">
              <MasterStatusBadge :status="row.status" />
            </template>
          </el-table-column>
          <el-table-column
            prop="anchorCount"
            :label="t('masters.list.columns.anchors')"
            width="100"
          />
          <el-table-column
            prop="updatedBy"
            :label="t('masters.list.columns.updatedBy')"
            min-width="120"
            show-overflow-tooltip
          />
          <el-table-column
            :label="t('masters.list.columns.updatedAt')"
            min-width="180"
          >
            <template #default="{ row }">
              {{ formatDateTime(row.updatedAt) }}
            </template>
          </el-table-column>
        </AppDataTable>
        <AppTablePagination
          v-model:current-page="currentPage"
          :page-size="CLIENT_TABLE_PAGE_SIZE"
          :total="totalMasterRows"
        />
      </template>

      <EmptyStatePanel
        v-else
        title-key="masters.list.empty"
      />
    </template>

    <EmptyStatePanel
      v-else-if="!errorMessage"
      title-key="masters.list.empty"
    />

    <MasterUploadDialog v-model="uploadDialogOpen" @submit="handleUpload" />
  </AppPageLayout>
</template>

<style scoped lang="scss">
.page-alert {
  margin-bottom: var(--space-4);
}
</style>
