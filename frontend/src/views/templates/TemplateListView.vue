<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import PackageCatalogNotice from '@/components/catalog/PackageCatalogNotice.vue'
import AppDataTable from '@/components/common/AppDataTable.vue'
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import TemplateCreateDialog from '@/components/templates/TemplateCreateDialog.vue'
import TemplateStatusBadge from '@/components/templates/TemplateStatusBadge.vue'
import { rowSortMethod, useDataTableFilters } from '@/composables/useDataTableFilters'
import { useGroupedCatalogPagination } from '@/composables/useGroupedCatalogPagination'
import { useActivatableTableRow } from '@/composables/useActivatableTableRow'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { useLifecycleStatusFilterOptions } from '@/composables/useTableFilterOptions'
import { useCapabilities } from '@/composables/useCapabilities'
import { templateDetailPath } from '@/routing/routeKeys'
import { useTemplatesStore } from '@/stores/templates'
import type { TemplateSummary, TemplateLifecycleStatus } from '@/types/template'
import { ElMessage } from 'element-plus'

const { t, te } = useI18n()
const { formatDateTime } = useLocaleFormatters()
const lifecycleStatusFilterOptions = useLifecycleStatusFilterOptions()
const router = useRouter()
const templatesStore = useTemplatesStore()
const { authorTemplates, decideTests, decideApprovals, publishTemplates } = useCapabilities()

type WorkflowFilterKey = 'awaitingTest' | 'awaitingApproval' | 'awaitingPublish'

const activeWorkflowFilter = ref<WorkflowFilterKey | null>(null)

const createDialogOpen = ref(false)
const currentPage = ref(1)
const pageSize = 10

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
  const chip = workflowFilterChips.value.find((entry) => entry.key === activeWorkflowFilter.value)
  if (!chip) {
    return templatesStore.templates
  }
  return templatesStore.templates.filter((template) =>
    chip.statuses.includes(template.lifecycleStatus),
  )
})

const allTemplates = computed(() => catalogTemplates.value)
const { filters: columnFilters, filteredRows: filteredTemplates, hasActiveFilters, clearFilters } =
  useDataTableFilters(allTemplates, [
    { key: 'name', getValue: (row) => row.name },
    { key: 'externalId', getValue: (row) => row.externalId },
    { key: 'status', getValue: (row) => row.lifecycleStatus, matchMode: 'exact' },
    { key: 'releaseVersion', getValue: (row) => row.releaseVersion ?? '' },
    {
      key: 'releaseVersionCount',
      getValue: (row) => String(row.releaseVersionCount),
    },
    { key: 'updatedBy', getValue: (row) => row.updatedBy },
    { key: 'updatedAt', getValue: (row) => formatDateTime(row.updatedAt) },
  ])
const { paginatedGroups: groupedTemplates, totalGroups: totalTemplateGroups } = useGroupedCatalogPagination(
  filteredTemplates,
  (row) => row.groupCode,
  currentPage,
  pageSize,
)
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
  router.push(templateDetailPath(templateId))
}

const { onRowClick: activateTemplateRow } = useActivatableTableRow<TemplateSummary>((row) =>
  openTemplate(row.id),
)

function onWorkflowFilterChange(key: WorkflowFilterKey, checked: boolean) {
  activeWorkflowFilter.value = checked ? key : null
  currentPage.value = 1
}

function clearWorkflowFilter() {
  activeWorkflowFilter.value = null
  currentPage.value = 1
}

function handleCreated(templateId: string) {
  ElMessage.success(t('templates.create.success'))
  router.push(templateDetailPath(templateId))
}

const sortByLifecycleStatus = rowSortMethod<TemplateSummary>((row) => row.lifecycleStatus)
const sortByReleaseVersionCount = rowSortMethod<TemplateSummary>((row) => row.releaseVersionCount)
const sortByUpdatedAt = rowSortMethod<TemplateSummary>((row) => row.updatedAt)
</script>

<template>
  <div class="templates-page">
    <header class="page-header">
      <div>
        <h1>{{ t('templates.list.title') }}</h1>
        <p>{{ t('templates.list.description') }}</p>
      </div>
      <el-button v-if="authorTemplates" type="primary" @click="createDialogOpen = true">
        {{ t('templates.create.open') }}
      </el-button>
    </header>

    <PackageCatalogNotice kind="template" />

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

    <template v-else-if="groupedTemplates.length > 0">
      <div v-if="hasActiveFilters" class="table-toolbar">
        <el-button size="small" text @click="clearFilters">{{ t('table.clearFilters') }}</el-button>
      </div>
      <section v-for="[groupCode, items] in groupedTemplates" :key="groupCode" class="group-section">
        <h2>{{ t('templates.list.groupSection', { groupCode }) }}</h2>
        <AppDataTable
          activatable
          :data="items"
          @row-click="activateTemplateRow"
        >
          <el-table-column prop="name" sortable min-width="220">
            <template #header>
              <TableColumnHeader
                :label="t('templates.list.columns.name')"
                v-model="columnFilters.name"
              />
            </template>
          </el-table-column>
          <el-table-column prop="externalId" sortable min-width="180">
            <template #header>
              <TableColumnHeader
                :label="t('templates.list.columns.externalId')"
                v-model="columnFilters.externalId"
              />
            </template>
          </el-table-column>
          <el-table-column
            sortable
            :sort-method="sortByLifecycleStatus"
            width="160"
          >
            <template #header>
              <TableColumnHeader
                :label="t('templates.list.columns.status')"
                v-model="columnFilters.status"
                filter-type="select"
                :options="lifecycleStatusFilterOptions"
              />
            </template>
            <template #default="{ row }">
              <TemplateStatusBadge :status="row.lifecycleStatus" />
            </template>
          </el-table-column>
          <el-table-column prop="releaseVersion" sortable width="140">
            <template #header>
              <TableColumnHeader
                :label="t('templates.list.columns.releaseVersion')"
                v-model="columnFilters.releaseVersion"
              />
            </template>
            <template #default="{ row }">
              {{ row.releaseVersion ?? t('templates.detail.noReleaseVersion') }}
            </template>
          </el-table-column>
          <el-table-column
            sortable
            width="120"
            :sort-method="sortByReleaseVersionCount"
          >
            <template #header>
              <TableColumnHeader
                :label="t('templates.list.columns.releaseVersionCount')"
                v-model="columnFilters.releaseVersionCount"
              />
            </template>
            <template #default="{ row }">
              {{ row.releaseVersionCount }}
            </template>
          </el-table-column>
          <el-table-column prop="updatedBy" sortable min-width="120">
            <template #header>
              <TableColumnHeader
                :label="t('templates.list.columns.updatedBy')"
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
                :label="t('templates.list.columns.updatedAt')"
                v-model="columnFilters.updatedAt"
              />
            </template>
            <template #default="{ row }">
              {{ formatDateTime(row.updatedAt) }}
            </template>
          </el-table-column>
        </AppDataTable>
      </section>
    </template>

    <el-empty v-else :description="t('templates.list.empty')" />

    <el-pagination
      v-if="totalTemplateGroups > pageSize"
      v-model:current-page="currentPage"
      class="list-pagination"
      layout="prev, pager, next"
      :page-size="pageSize"
      :total="totalTemplateGroups"
    />

    <TemplateCreateDialog v-model="createDialogOpen" @created="handleCreated" />
  </div>
</template>

<style scoped lang="scss">
.templates-page {
  padding: 2rem;
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

.page-alert {
  margin-bottom: 1rem;
}

.workflow-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.table-toolbar {
  margin-bottom: 0.75rem;
}

.group-section {
  margin-bottom: 2rem;

  h2 {
    margin: 0 0 0.75rem;
    font-size: 1.125rem;
  }
}

.list-pagination {
  margin-top: 1rem;
  justify-content: flex-end;
}
</style>
