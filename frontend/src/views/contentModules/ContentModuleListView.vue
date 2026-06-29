<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import PackageCatalogNotice from '@/components/catalog/PackageCatalogNotice.vue'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import ScopedGroupSelect from '@/components/common/ScopedGroupSelect.vue'
import ContentModuleCreateDialog from '@/components/contentModules/ContentModuleCreateDialog.vue'
import { rowSortMethod, useDataTableFilters } from '@/composables/useDataTableFilters'
import { useCatalogPagination } from '@/composables/useCatalogPagination'
import { useActivatableTableRow } from '@/composables/useActivatableTableRow'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { useCapabilities } from '@/composables/useCapabilities'
import { useScopedGroupOptions } from '@/composables/useScopedGroupOptions'
import { CLIENT_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import { contentModuleDetailPath } from '@/routing/routeKeys'
import { useContentModulesStore } from '@/stores/contentModules'
import { useSessionStore } from '@/stores/session'
import type { ContentModuleSummary } from '@/types/contentModule'
import { ElMessage } from 'element-plus'

const { t, te } = useI18n()
const { formatDateTime } = useLocaleFormatters()
const router = useRouter()
const contentModulesStore = useContentModulesStore()
const sessionStore = useSessionStore()
const { authorContentModules } = useCapabilities()
const { resolveDefaultGroupCode, ensureGroupCatalog } = useScopedGroupOptions()

const selectedGroupCode = ref('')
const groupSelectRef = ref<InstanceType<typeof ScopedGroupSelect> | null>(null)
const createDialogOpen = ref(false)
const currentPage = ref(1)

const allModules = computed(() => contentModulesStore.modules)
const { filters: columnFilters, filteredRows: filteredModules, hasActiveFilters, clearFilters } =
  useDataTableFilters(allModules, [
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
  await groupSelectRef.value?.prepare()
  selectedGroupCode.value = resolveDefaultGroupCode('')
  if (selectedGroupCode.value) {
    await reloadModules()
  }
})

watch(selectedGroupCode, async (groupCode) => {
  if (!groupCode) {
    contentModulesStore.modules = []
    return
  }
  currentPage.value = 1
  await reloadModules()
})

async function reloadModules() {
  if (!selectedGroupCode.value) {
    return
  }
  try {
    await ensureGroupCatalog()
    await contentModulesStore.fetchModules(selectedGroupCode.value)
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

const sortByModuleCode = rowSortMethod<ContentModuleSummary>((row) => row.moduleCode)
const sortByUpdatedAt = rowSortMethod<ContentModuleSummary>((row) => row.updatedAt)
</script>

<template>
  <div class="content-modules-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">{{ sessionStore.session?.displayName }}</p>
        <h1>{{ t('contentModules.list.title') }}</h1>
        <p>{{ t('contentModules.list.description') }}</p>
      </div>
      <el-button v-if="canCreate" type="primary" @click="createDialogOpen = true">
        {{ t('contentModules.create.open') }}
      </el-button>
    </header>

    <PackageCatalogNotice kind="contentModule" />

    <section class="group-filter">
      <el-form-item :label="t('contentModules.list.groupFilter')" label-position="left" class="group-filter-item">
        <ScopedGroupSelect
          ref="groupSelectRef"
          v-model="selectedGroupCode"
          :placeholder="t('contentModules.list.groupFilterPlaceholder')"
        />
      </el-form-item>
    </section>

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

    <EmptyStatePanel
      v-else-if="!selectedGroupCode"
      title-key="contentModules.list.selectGroupTitle"
      description-key="contentModules.list.selectGroupDescription"
    />

    <template v-else-if="!errorMessage && filteredModules.length > 0">
      <div v-if="hasActiveFilters" class="table-toolbar">
        <el-button size="small" text @click="clearFilters">{{ t('table.clearFilters') }}</el-button>
      </div>
      <AppDataTable activatable :data="paginatedModules" @row-click="activateModuleRow">
        <el-table-column prop="moduleCode" sortable :sort-method="sortByModuleCode" min-width="180">
          <template #header>
            <TableColumnHeader
              :label="t('contentModules.list.columns.moduleCode')"
              filter-key="moduleCode"
              v-model:filter="columnFilters.moduleCode"
            />
          </template>
        </el-table-column>
        <el-table-column prop="name" min-width="220">
          <template #header>
            <TableColumnHeader
              :label="t('contentModules.list.columns.name')"
              filter-key="name"
              v-model:filter="columnFilters.name"
            />
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" sortable :sort-method="sortByUpdatedAt" width="200">
          <template #header>
            <TableColumnHeader
              :label="t('contentModules.list.columns.updatedAt')"
              filter-key="updatedAt"
              v-model:filter="columnFilters.updatedAt"
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
      v-else-if="selectedGroupCode && !contentModulesStore.loadingList && !errorMessage"
      title-key="contentModules.list.empty"
      description-key="contentModules.list.emptyDescription"
    />

    <ContentModuleCreateDialog v-model="createDialogOpen" @created="handleCreated" />
  </div>
</template>

<style scoped lang="scss">
.content-modules-page {
  padding: 1.5rem 2rem 2rem;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.25rem;

  h1 {
    margin: 0.25rem 0;
    font-size: 1.5rem;
    font-weight: 650;
  }

  p {
    margin: 0;
    color: var(--text-muted);
  }
}

.eyebrow {
  margin: 0;
  font-size: 0.8125rem;
  color: var(--text-muted);
}

.group-filter {
  margin-bottom: 1rem;
}

.group-filter-item {
  margin-bottom: 0;

  :deep(.el-form-item__label) {
    font-weight: 600;
    padding-right: 0.75rem;
  }
}

.page-alert {
  margin-bottom: 1rem;
}

.table-toolbar {
  margin-bottom: 0.5rem;
}
</style>
