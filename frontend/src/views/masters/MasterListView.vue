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
import MasterStatusBadge from '@/components/masters/MasterStatusBadge.vue'
import MasterUploadDialog from '@/components/masters/MasterUploadDialog.vue'
import { useAbortableCatalogLoader } from '@/composables/useAbortableCatalogLoader'
import { useCatalogTableControls } from '@/composables/useCatalogTableControls'
import { useActivatableTableRow } from '@/composables/useActivatableTableRow'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { useMasterStatusFilterOptions } from '@/composables/useTableFilterOptions'
import { useCapabilities } from '@/composables/useCapabilities'
import { useEntityLinkTargets } from '@/composables/useEntityLinkTargets'
import { SERVER_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import { MASTER_DETAIL_PATH_PREFIX } from '@/routing/routeKeys'
import { useMastersStore } from '@/stores/masters'
import type { MasterDocumentSummary } from '@/types/master'
import { resolveUpdatedByDisplay } from '@/utils/userDisplay'
import { ElMessage } from 'element-plus'

const { t, te } = useI18n()
const { formatDateTime } = useLocaleFormatters()
const masterStatusFilterOptions = useMasterStatusFilterOptions()
const router = useRouter()
const mastersStore = useMastersStore()
const { masterDetailLink } = useEntityLinkTargets()

const uploadDialogOpen = ref(false)
const uploadFailurePending = ref(false)
const currentPage = ref(1)
const listHydrated = ref(false)

const allMasters = computed(() => mastersStore.masters)
const {
  searchQuery,
  filters,
  activeSortKey,
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
      key: 'groupCodeAsc',
      labelKey: 'table.sort.groupAsc',
      getter: (row) => row.groupCode,
      order: 'asc',
    },
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
  defaultSortKey: 'groupCodeAsc',
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
  { key: 'groupCodeAsc', labelKey: 'table.sort.groupAsc' },
  { key: 'updatedAtDesc', labelKey: 'table.sort.updatedAtDesc' },
  { key: 'updatedAtAsc', labelKey: 'table.sort.updatedAtAsc' },
  { key: 'nameAsc', labelKey: 'table.sort.nameAsc' },
  { key: 'groupAsc', labelKey: 'table.sort.groupAsc' },
])

const showCatalogChrome = computed(
  () =>
    listHydrated.value &&
    !showListLoadError.value &&
    (mastersStore.masterListTotalElements > 0 ||
      hasAnyActive.value ||
      mastersStore.masters.length > 0),
)

function buildListQuery() {
  return {
    search: searchQuery.value.trim() || undefined,
    groupCode: filters.groupCode?.trim() || undefined,
    status: filters.status?.trim() || undefined,
    sort: activeSortKey.value || 'groupCodeAsc',
  }
}

const { manageMasters } = useCapabilities()
const canUpload = computed(() => manageMasters.value)
const errorMessage = computed(() => {
  const key = mastersStore.lastErrorMessageKey
  if (!key || uploadDialogOpen.value || uploadFailurePending.value) {
    return ''
  }
  return te(key) ? t(key) : t('masters.error.loadList')
})

/** List LoadErrorPanel must not share upload failures (dialog owns those inline). */
const showListLoadError = computed(
  () =>
    Boolean(mastersStore.lastErrorMessageKey) &&
    !mastersStore.loadingList &&
    !uploadDialogOpen.value &&
    !uploadFailurePending.value,
)

const { reload: reloadMasters, signal: abortSignal } = useAbortableCatalogLoader(async (signal) => {
  await mastersStore.fetchMasters(currentPage.value - 1, SERVER_TABLE_PAGE_SIZE, {
    signal,
    ...buildListQuery(),
  })
  listHydrated.value = true
})

watch(currentPage, async (page) => {
  const serverPage = page - 1
  if (serverPage === mastersStore.masterListPage) {
    return
  }
  try {
    await mastersStore.fetchMasters(serverPage, SERVER_TABLE_PAGE_SIZE, {
      signal: abortSignal.value,
      ...buildListQuery(),
    })
  } catch {
    // Error surfaced via store message key.
  }
})

watch(
  [searchQuery, filters, activeSortKey],
  async () => {
    if (!listHydrated.value) {
      return
    }
    if (currentPage.value !== 1) {
      currentPage.value = 1
      return
    }
    await reloadMasters()
  },
  { deep: true },
)

onMounted(async () => {
  await reloadMasters()
})

watch(uploadDialogOpen, (open) => {
  if (open) {
    return
  }
  if (uploadFailurePending.value) {
    mastersStore.lastErrorMessageKey = null
    uploadFailurePending.value = false
  }
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
    uploadFailurePending.value = false
    uploadDialogOpen.value = false
    ElMessage.success(t('masters.upload.success'))
    router.push(`${MASTER_DETAIL_PATH_PREFIX}${created.id}`)
  } catch {
    // Keep dialog open — inline translated error via serverErrorKey (LR-C10-B).
    uploadFailurePending.value = true
  }
}

function clearUploadServerError() {
  mastersStore.lastErrorMessageKey = null
  uploadFailurePending.value = false
}
</script>

<template>
  <AppPageLayout layout-variant="fluid">
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
      v-if="showListLoadError"
      :message-key="mastersStore.lastErrorMessageKey || 'masters.error.loadList'"
      :retryable="mastersStore.lastListErrorRetryable"
      @retry="reloadMasters"
    />

    <el-skeleton v-else-if="mastersStore.loadingList" :rows="6" animated />

    <template v-else-if="showCatalogChrome">
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

      <template v-if="allMasters.length > 0">
        <AppDataTable activatable :data="allMasters" @row-click="activateMasterRow">
          <el-table-column
            prop="groupCode"
            :label="t('masters.list.columns.group')"
            width="140"
          />
          <el-table-column
            :label="t('masters.list.columns.name')"
            min-width="220"
          >
            <template #default="{ row }">
              <EntityLinkCell
                :label="row.name"
                :to="masterDetailLink(row.id)"
              />
            </template>
          </el-table-column>
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
            :label="t('masters.list.columns.updatedBy')"
            min-width="120"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              {{ resolveUpdatedByDisplay(row.updatedBy, row.updatedByDisplayName) }}
            </template>
          </el-table-column>
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
          :page-size="mastersStore.masterListSize"
          :total="mastersStore.masterListTotalElements"
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
    >
      <template v-if="canUpload" #actions>
        <el-button type="primary" @click="uploadDialogOpen = true">
          {{ t('masters.upload.open') }}
        </el-button>
      </template>
    </EmptyStatePanel>

    <MasterUploadDialog
      v-model="uploadDialogOpen"
      :loading="mastersStore.submitting"
      :upload-progress="mastersStore.uploadProgress"
      :server-error-key="uploadDialogOpen ? mastersStore.lastErrorMessageKey : null"
      @submit="handleUpload"
      @clear-server-error="clearUploadServerError"
    />
  </AppPageLayout>
</template>

<style scoped lang="scss">
.page-alert {
  margin-bottom: var(--space-4);
}
</style>
