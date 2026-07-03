<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import CatalogFilterToolbar from '@/components/common/CatalogFilterToolbar.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import TemplateCreateDialog from '@/components/templates/TemplateCreateDialog.vue'
import TemplateImportDialog from '@/components/templates/TemplateImportDialog.vue'
import TemplateStatusBadge from '@/components/templates/TemplateStatusBadge.vue'
import { useCatalogTableControls } from '@/composables/useCatalogTableControls'
import { useCatalogPagination } from '@/composables/useCatalogPagination'
import { useActivatableTableRow } from '@/composables/useActivatableTableRow'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { useLifecycleStatusFilterOptions } from '@/composables/useTableFilterOptions'
import { useCapabilities } from '@/composables/useCapabilities'
import { CLIENT_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import { templateDetailPath } from '@/routing/routeKeys'
import { useTemplatesStore } from '@/stores/templates'
import type { TemplateSummary, TemplateLifecycleStatus } from '@/types/template'
import { isAwaitingApproverDecision } from '@/utils/templateApproverJourney'
import { ElMessage } from 'element-plus'

const { t, te } = useI18n()
const { formatDateTime } = useLocaleFormatters()
const lifecycleStatusFilterOptions = useLifecycleStatusFilterOptions()
const router = useRouter()
const templatesStore = useTemplatesStore()
const { authorTemplates, exportTemplates, decideTests, decideApprovals, publishTemplates } =
  useCapabilities()

type WorkflowFilterKey = 'awaitingTest' | 'awaitingApproval' | 'awaitingPublish'

const activeWorkflowFilter = ref<WorkflowFilterKey | null>(null)
const createDialogOpen = ref(false)
const importDialogOpen = ref(false)
const currentPage = ref(1)

const workflowFilterChips = computed(() => {
  const chips: Array<{
    key: WorkflowFilterKey
    labelKey: string
    statuses: TemplateLifecycleStatus[]
  }> = []
  if (decideTests.value) {
    chips.push({
      key: 'awaitingTest',
      labelKey: 'templates.list.workflowFilters.awaitingTest',
      statuses: ['TESTING'],
    })
  }
  if (decideApprovals.value) {
    chips.push({
      key: 'awaitingApproval',
      labelKey: 'templates.list.workflowFilters.awaitingApproval',
      statuses: ['APPROVAL'],
    })
  }
  if (publishTemplates.value) {
    chips.push({
      key: 'awaitingPublish',
      labelKey: 'templates.list.workflowFilters.awaitingPublish',
      statuses: ['PENDING_RELEASE'],
    })
  }
  return chips
})

const catalogTemplates = computed(() => {
  if (!activeWorkflowFilter.value) {
    return templatesStore.templates
  }
  if (activeWorkflowFilter.value === 'awaitingApproval') {
    return templatesStore.templates.filter(isAwaitingApproverDecision)
  }
  const chip = workflowFilterChips.value.find((entry) => entry.key === activeWorkflowFilter.value)
  if (!chip) {
    return templatesStore.templates
  }
  return templatesStore.templates.filter((template) =>
    chip.statuses.includes(template.lifecycleStatus),
  )
})

const {
  searchQuery,
  filters,
  activeSortKey,
  sortedRows,
  hasAnyActive,
  activeFilterChips,
  clearAll,
  removeFilterChip,
} = useCatalogTableControls(catalogTemplates, {
  searchGetters: [
    (row) => row.name,
    (row) => row.externalId,
    (row) => row.groupCode,
  ],
  filters: [
    {
      key: 'groupCode',
      labelKey: 'templates.list.columns.group',
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
      key: 'externalIdAsc',
      labelKey: 'table.sort.externalIdAsc',
      getter: (row) => row.externalId,
      order: 'asc',
    },
  ],
  defaultSortKey: 'updatedAtDesc',
})

const catalogToolbarFilters = computed(() => [
  { key: 'groupCode', labelKey: 'templates.list.columns.group', type: 'text' as const },
  {
    key: 'status',
    labelKey: 'templates.list.columns.status',
    type: 'select' as const,
    options: lifecycleStatusFilterOptions.value,
  },
])

const catalogSortOptions = computed(() => [
  { key: 'updatedAtDesc', labelKey: 'table.sort.updatedAtDesc' },
  { key: 'updatedAtAsc', labelKey: 'table.sort.updatedAtAsc' },
  { key: 'nameAsc', labelKey: 'table.sort.nameAsc' },
  { key: 'externalIdAsc', labelKey: 'table.sort.externalIdAsc' },
])

const { paginatedRows: paginatedTemplates, totalRows: totalTemplateRows } = useCatalogPagination(
  sortedRows,
  currentPage,
  CLIENT_TABLE_PAGE_SIZE,
)

watch([hasAnyActive, activeWorkflowFilter], () => {
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

function clearWorkflowFilter() {
  activeWorkflowFilter.value = null
}

function onWorkflowFilterChange(key: WorkflowFilterKey, checked: boolean) {
  activeWorkflowFilter.value = checked ? key : null
}

function handleCreated(templateId: string) {
  ElMessage.success(t('templates.create.success'))
  router.push(templateDetailPath(templateId))
}

function handleImported(templateId: string) {
  ElMessage.success(t('templates.import.success'))
  router.push(templateDetailPath(templateId))
}

function openTemplate(templateId: string) {
  router.push(templateDetailPath(templateId))
}

const { onRowClick: activateTemplateRow } = useActivatableTableRow<TemplateSummary>((row) =>
  openTemplate(row.id),
)
</script>

<template>
  <AppPageLayout>
    <PageHeader
      :title="t('templates.list.title')"
      :description="t('templates.list.description')"
      :help-text="t('packageCatalog.template.noticeDescription')"
    >
      <template #actions>
        <el-button v-if="exportTemplates" @click="importDialogOpen = true">
          {{ t('templates.import.open') }}
        </el-button>
        <el-button v-if="authorTemplates" type="primary" @click="createDialogOpen = true">
          {{ t('templates.create.open') }}
        </el-button>
      </template>
    </PageHeader>

    <div v-if="workflowFilterChips.length > 0" class="workflow-filters">
      <el-check-tag
        :checked="activeWorkflowFilter === null"
        @change="(checked: boolean) => checked && clearWorkflowFilter()"
      >
        {{ t('templates.list.workflowFilters.all') }}
      </el-check-tag>
      <el-check-tag
        v-for="chip in workflowFilterChips"
        :key="chip.key"
        :checked="activeWorkflowFilter === chip.key"
        @change="(checked: boolean) => onWorkflowFilterChange(chip.key, checked)"
      >
        {{ t(chip.labelKey) }}
      </el-check-tag>
    </div>

    <el-alert
      v-if="errorMessage"
      class="page-alert"
      type="error"
      :title="errorMessage"
      show-icon
      :closable="false"
    />

    <el-skeleton v-if="templatesStore.loadingList" :rows="6" animated />

    <template v-else-if="!errorMessage && catalogTemplates.length > 0">
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
        <AppDataTable activatable :data="paginatedTemplates" @row-click="activateTemplateRow">
          <el-table-column
            prop="groupCode"
            :label="t('templates.list.columns.group')"
            width="140"
          />
          <el-table-column
            prop="name"
            :label="t('templates.list.columns.name')"
            min-width="220"
            show-overflow-tooltip
          />
          <el-table-column
            prop="externalId"
            :label="t('templates.list.columns.externalId')"
            min-width="180"
            show-overflow-tooltip
          />
          <el-table-column :label="t('templates.list.columns.status')" width="160">
            <template #default="{ row }">
              <TemplateStatusBadge
                :status="row.lifecycleStatus"
                :approval-sub-state="row.approvalSubState"
              />
            </template>
          </el-table-column>
          <el-table-column
            prop="releaseVersion"
            :label="t('templates.list.columns.releaseVersion')"
            width="140"
          >
            <template #default="{ row }">
              {{ row.releaseVersion ?? t('templates.detail.noReleaseVersion') }}
            </template>
          </el-table-column>
          <el-table-column
            prop="releaseVersionCount"
            :label="t('templates.list.columns.releaseVersionCount')"
            width="120"
          />
          <el-table-column
            prop="updatedBy"
            :label="t('templates.list.columns.updatedBy')"
            min-width="120"
            show-overflow-tooltip
          />
          <el-table-column :label="t('templates.list.columns.updatedAt')" min-width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.updatedAt) }}
            </template>
          </el-table-column>
        </AppDataTable>
        <AppTablePagination
          v-model:current-page="currentPage"
          :page-size="CLIENT_TABLE_PAGE_SIZE"
          :total="totalTemplateRows"
        />
      </template>

      <EmptyStatePanel v-else title-key="templates.list.empty" />
    </template>

    <EmptyStatePanel v-else-if="!errorMessage" title-key="templates.list.empty" />

    <TemplateCreateDialog v-model="createDialogOpen" @created="handleCreated" />
    <TemplateImportDialog v-model="importDialogOpen" @imported="handleImported" />
  </AppPageLayout>
</template>

<style scoped lang="scss">
.page-alert {
  margin-bottom: var(--space-4);
}

.workflow-filters {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}
</style>
