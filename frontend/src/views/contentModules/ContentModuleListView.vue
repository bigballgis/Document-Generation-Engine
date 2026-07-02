<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import PackageCatalogNotice from '@/components/catalog/PackageCatalogNotice.vue'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import ContentModuleCreateDialog from '@/components/contentModules/ContentModuleCreateDialog.vue'
import { rowSortMethod, useDataTableFilters } from '@/composables/useDataTableFilters'
import { useCatalogPagination } from '@/composables/useCatalogPagination'
import { useActivatableTableRow } from '@/composables/useActivatableTableRow'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { useCapabilities } from '@/composables/useCapabilities'
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

const createDialogOpen = ref(false)
const currentPage = ref(1)

const allModules = computed(() => contentModulesStore.modules)
const { filters: columnFilters, filteredRows: filteredModules, hasActiveFilters, clearFilters } =
  useDataTableFilters(allModules, [
    { key: 'groupCode', getValue: (row) => row.groupCode },
    { key: 'moduleCode', getValue: (row) => row.moduleCode },
    { key: 'name', getValue: (row) => row.name },
    { key: 'updatedAt', getValue: (row) => formatDateTime(row.updatedAt) },
  ])
const { paginatedRows: paginatedModules, totalRows: totalModuleRows } = useCatalogPagination(
  filteredModules,
  currentPage,
  CLIENT_TABLE_PAGE_SIZE,
)

const canCreate = computed(() => authorContentModules.value)
const errorMessage = computed(() => {
  const key = contentModulesStore.lastErrorMessageKey
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('contentModules.error.loadList')
})

onMounted(async () => {
  await reloadModules()
})

async function reloadModules() {
  try {
    await contentModulesStore.fetchModules()
  } catch {
    // Error surfaced via store message key.
  }
}

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

const sortByGroupCode = rowSortMethod<ContentModuleSummary>((row) => row.groupCode)
const sortByModuleCode = rowSortMethod<ContentModuleSummary>((row) => row.moduleCode)
const sortByUpdatedAt = rowSortMethod<ContentModuleSummary>((row) => row.updatedAt)
</script>

<template>
  <AppPageLayout>
    <PageHeader
      :title="t('contentModules.list.title')"
      :description="t('contentModules.list.description')"
    >
      <template #actions>
        <el-button v-if="canCreate" type="primary" @click="createDialogOpen = true">
          {{ t('contentModules.create.open') }}
        </el-button>
      </template>
    </PageHeader>

    <PackageCatalogNotice kind="contentModule" />

    <el-alert
      v-if="errorMessage"
      class="page-alert"
      type="error"
      :title="errorMessage"
      show-icon
      :closable="false"
    >
      <el-button size="small" type="primary" @click="reloadModules">
        {{ t('common.retry') }}
      </el-button>
    </el-alert>

    <el-skeleton v-if="contentModulesStore.loadingList" :rows="6" animated />

    <template v-else-if="!errorMessage && filteredModules.length > 0">
      <div v-if="hasActiveFilters" class="table-toolbar">
        <el-button size="small" text @click="clearFilters">{{ t('table.clearFilters') }}</el-button>
      </div>
      <AppDataTable activatable :data="paginatedModules" @row-click="activateModuleRow">
        <el-table-column prop="groupCode" sortable :sort-method="sortByGroupCode" width="140">
          <template #header>
            <TableColumnHeader
              :label="t('contentModules.list.columns.group')"
              v-model="columnFilters.groupCode"
            />
          </template>
        </el-table-column>
        <el-table-column prop="moduleCode" sortable :sort-method="sortByModuleCode" min-width="180">
          <template #header>
            <TableColumnHeader
              :label="t('contentModules.list.columns.moduleCode')"
              v-model="columnFilters.moduleCode"
            />
          </template>
        </el-table-column>
        <el-table-column prop="name" min-width="220">
          <template #header>
            <TableColumnHeader
              :label="t('contentModules.list.columns.name')"
              v-model="columnFilters.name"
            />
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" sortable :sort-method="sortByUpdatedAt" width="200">
          <template #header>
            <TableColumnHeader
              :label="t('contentModules.list.columns.updatedAt')"
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
        :total="totalModuleRows"
      />
    </template>

    <EmptyStatePanel
      v-else-if="!contentModulesStore.loadingList && !errorMessage"
      title-key="contentModules.list.empty"
      description-key="contentModules.list.emptyDescription"
    />

    <ContentModuleCreateDialog v-model="createDialogOpen" @created="handleCreated" />
  </AppPageLayout>
</template>

<style scoped lang="scss">
.page-alert {
  margin-bottom: var(--space-4);
}

.table-toolbar {
  margin-bottom: var(--space-3);
}
</style>
