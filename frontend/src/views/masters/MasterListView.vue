<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import PackageCatalogNotice from '@/components/catalog/PackageCatalogNotice.vue'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import MasterStatusBadge from '@/components/masters/MasterStatusBadge.vue'
import MasterUploadDialog from '@/components/masters/MasterUploadDialog.vue'
import { rowSortMethod, useDataTableFilters } from '@/composables/useDataTableFilters'
import { useCatalogPagination } from '@/composables/useCatalogPagination'
import { useActivatableTableRow } from '@/composables/useActivatableTableRow'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { useMasterStatusFilterOptions } from '@/composables/useTableFilterOptions'
import { useCapabilities } from '@/composables/useCapabilities'
import { CLIENT_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import { MASTER_DETAIL_PATH_PREFIX } from '@/routing/routeKeys'
import { useMastersStore } from '@/stores/masters'
import { useSessionStore } from '@/stores/session'
import type { MasterDocumentSummary } from '@/types/master'
import { ElMessage } from 'element-plus'

const { t, te } = useI18n()
const { formatDateTime } = useLocaleFormatters()
const masterStatusFilterOptions = useMasterStatusFilterOptions()
const router = useRouter()
const mastersStore = useMastersStore()
const sessionStore = useSessionStore()

const uploadDialogOpen = ref(false)
const currentPage = ref(1)

const allMasters = computed(() => mastersStore.masters)
const { filters: columnFilters, filteredRows: filteredMasters, hasActiveFilters, clearFilters } =
  useDataTableFilters(allMasters, [
    { key: 'groupCode', getValue: (row) => row.groupCode },
    { key: 'name', getValue: (row) => row.name },
    { key: 'status', getValue: (row) => row.status, matchMode: 'exact' },
    { key: 'anchorCount', getValue: (row) => String(row.anchorCount) },
    { key: 'updatedBy', getValue: (row) => row.updatedBy },
    { key: 'updatedAt', getValue: (row) => formatDateTime(row.updatedAt) },
  ])
const { paginatedRows: paginatedMasters, totalRows: totalMasterRows } = useCatalogPagination(
  filteredMasters,
  currentPage,
  CLIENT_TABLE_PAGE_SIZE,
)
const { manageMasters } = useCapabilities()
const canUpload = computed(() => manageMasters.value)
const errorMessage = computed(() => {
  const key = mastersStore.lastErrorMessageKey
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('masters.error.loadList')
})

onMounted(async () => {
  await reloadMasters()
})

async function reloadMasters() {
  try {
    await mastersStore.fetchMasters()
  } catch {
    // Error surfaced via store message key.
  }
}

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

const sortByGroupCode = rowSortMethod<MasterDocumentSummary>((row) => row.groupCode)
const sortByStatus = rowSortMethod<MasterDocumentSummary>((row) => row.status)
const sortByAnchorCount = rowSortMethod<MasterDocumentSummary>((row) => row.anchorCount)
const sortByUpdatedAt = rowSortMethod<MasterDocumentSummary>((row) => row.updatedAt)
</script>

<template>
  <div class="masters-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">{{ sessionStore.session?.displayName }}</p>
        <h1>{{ t('masters.list.title') }}</h1>
        <p>{{ t('masters.list.description') }}</p>
      </div>
      <el-button v-if="canUpload" type="primary" @click="uploadDialogOpen = true">
        {{ t('masters.upload.open') }}
      </el-button>
    </header>

    <PackageCatalogNotice kind="master" />

    <el-alert
      v-if="errorMessage"
      class="page-alert"
      type="error"
      :title="errorMessage"
      show-icon
      :closable="false"
    >
      <el-button size="small" type="primary" @click="reloadMasters">
        {{ t('common.retry') }}
      </el-button>
    </el-alert>

    <el-skeleton v-if="mastersStore.loadingList" :rows="6" animated />

    <template v-else-if="!errorMessage && filteredMasters.length > 0">
      <div v-if="hasActiveFilters" class="table-toolbar">
        <el-button size="small" text @click="clearFilters">{{ t('table.clearFilters') }}</el-button>
      </div>
      <AppDataTable
        activatable
        :data="paginatedMasters"
        @row-click="activateMasterRow"
      >
        <el-table-column
          prop="groupCode"
          sortable
          width="140"
          :sort-method="sortByGroupCode"
        >
          <template #header>
            <TableColumnHeader
              :label="t('masters.list.columns.group')"
              v-model="columnFilters.groupCode"
            />
          </template>
        </el-table-column>
        <el-table-column prop="name" sortable min-width="220">
          <template #header>
            <TableColumnHeader
              :label="t('masters.list.columns.name')"
              v-model="columnFilters.name"
            />
          </template>
        </el-table-column>
        <el-table-column
          sortable
          :sort-method="sortByStatus"
          width="160"
        >
          <template #header>
            <TableColumnHeader
              :label="t('masters.list.columns.status')"
              v-model="columnFilters.status"
              filter-type="select"
              :options="masterStatusFilterOptions"
            />
          </template>
          <template #default="{ row }">
            <MasterStatusBadge :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column
          prop="anchorCount"
          sortable
          width="100"
          :sort-method="sortByAnchorCount"
        >
          <template #header>
            <TableColumnHeader
              :label="t('masters.list.columns.anchors')"
              v-model="columnFilters.anchorCount"
            />
          </template>
        </el-table-column>
        <el-table-column prop="updatedBy" sortable min-width="120">
          <template #header>
            <TableColumnHeader
              :label="t('masters.list.columns.updatedBy')"
              v-model="columnFilters.updatedBy"
            />
          </template>
        </el-table-column>
        <el-table-column
          sortable
          :sort-method="sortByUpdatedAt"
          min-width="180"
        >
          <template #header>
            <TableColumnHeader
              :label="t('masters.list.columns.updatedAt')"
              v-model="columnFilters.updatedAt"
            />
          </template>
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

    <el-empty v-else-if="!errorMessage" :description="t('masters.list.empty')" />

    <MasterUploadDialog
      v-model="uploadDialogOpen"
      @submit="handleUpload"
    />
  </div>
</template>

<style scoped lang="scss">
.masters-page {
  min-height: 100vh;
  padding: 2rem;
  background: var(--surface-bg);
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.5rem;

  h1 {
    margin: 0.25rem 0;
    font-size: 1.75rem;
  }

  p {
    margin: 0;
    color: var(--text-muted);
  }
}

.eyebrow {
  display: inline-flex;
  padding: 0.35rem 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  font-weight: 600;
  color: var(--brand-primary);
}

.page-alert {
  margin-bottom: 1rem;
}

.table-toolbar {
  margin-bottom: 0.75rem;
}
</style>
