<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import { rowSortMethod, useDataTableFilters } from '@/composables/useDataTableFilters'
import { useCatalogPagination } from '@/composables/useCatalogPagination'
import { CLIENT_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import type { TaskPartition, WorkflowTask } from '@/composables/useWorkflowTasks'
import {
  formatCollaborationAgeSeconds,
} from '@/utils/collaborationWorkItems'
import { resolveSubmitterDisplay } from '@/utils/userDisplay'
import {
  isAgeOverdueForQueue,
  resolveEffectiveTimeoutConfig,
  resolveThresholdHoursForQueue,
} from '@/utils/collaborationTimeoutThreshold'
import type { CollaborationTimeoutConfig } from '@/types/collaboration'

const props = defineProps<{
  partition: TaskPartition
  globalTimeoutConfig: CollaborationTimeoutConfig | null
  groupTimeoutConfigs: Record<string, CollaborationTimeoutConfig | null>
}>()

const emit = defineEmits<{
  open: [path: string]
}>()

const { t } = useI18n()

const isCollaboration = props.partition.kind === 'collaboration'

const filterDefs = isCollaboration
  ? [
      { key: 'action', getValue: (row: WorkflowTask) => t(row.titleKey) },
      { key: 'item', getValue: (row: WorkflowTask) => row.entityName },
      { key: 'group', getValue: (row: WorkflowTask) => row.groupCode ?? '' },
      {
        key: 'trigger',
        getValue: (row: WorkflowTask) =>
          row.triggerType ? t(`collaboration.workItem.trigger.${row.triggerType}.description`) : '',
      },
      { key: 'summary', getValue: (row: WorkflowTask) => row.summaryText ?? '' },
      {
        key: 'age',
        getValue: (row: WorkflowTask) =>
          row.ageSeconds !== undefined ? formatCollaborationAgeSeconds(row.ageSeconds) : '',
      },
      { key: 'submitter', getValue: (row: WorkflowTask) => resolveSubmitterDisplay(row.submitterUserId, row.submitterDisplayName) },
    ]
  : [
      { key: 'action', getValue: (row: WorkflowTask) => t(row.titleKey) },
      { key: 'item', getValue: (row: WorkflowTask) => row.entityName },
      { key: 'group', getValue: (row: WorkflowTask) => row.groupCode ?? '' },
      { key: 'hint', getValue: (row: WorkflowTask) => t(row.descriptionKey) },
    ]

const partitionTasks = computed(() => props.partition.tasks)

const { filters: columnFilters, filteredRows } = useDataTableFilters(partitionTasks, filterDefs)

const currentPage = ref(1)
const { paginatedRows, totalRows } = useCatalogPagination(
  filteredRows,
  currentPage,
  CLIENT_TABLE_PAGE_SIZE,
)

const sortTasksByTitle = rowSortMethod<WorkflowTask>((row) => t(row.titleKey))
const sortTasksByGroup = rowSortMethod<WorkflowTask>((row) => row.groupCode ?? '')
const sortTasksByHint = rowSortMethod<WorkflowTask>((row) => t(row.descriptionKey))
const sortTasksByTrigger = rowSortMethod<WorkflowTask>((row) =>
  row.triggerType ? t(`collaboration.workItem.trigger.${row.triggerType}.description`) : '',
)
const sortTasksBySummary = rowSortMethod<WorkflowTask>((row) => row.summaryText ?? '')
const sortTasksBySubmitter = rowSortMethod<WorkflowTask>((row) =>
  resolveSubmitterDisplay(row.submitterUserId, row.submitterDisplayName),
)
const sortTasksByAge = rowSortMethod<WorkflowTask>((row) => row.ageSeconds ?? 0)

function resolveThresholdHours(task: WorkflowTask): number | null {
  if (!isCollaboration || !task.queue) {
    return null
  }
  const groupCode = task.groupCode ?? ''
  const effectiveConfig = resolveEffectiveTimeoutConfig(
    props.globalTimeoutConfig,
    groupCode ? props.groupTimeoutConfigs[groupCode] : null,
  )
  if (!effectiveConfig) {
    return null
  }
  return resolveThresholdHoursForQueue(task.queue, effectiveConfig)
}

function showOverdueBadge(task: WorkflowTask): boolean {
  if (!isCollaboration || task.queue === undefined || task.ageSeconds === undefined) {
    return false
  }
  return isAgeOverdueForQueue(task.queue, task.ageSeconds, resolveThresholdHours(task))
}

function openTask(path: string, event?: Event) {
  event?.stopPropagation()
  emit('open', path)
}
</script>

<template>
  <section class="task-partition" :data-partition-id="partition.id">
    <header class="partition-header">
      <h3>{{ t(partition.headingKey) }}</h3>
    </header>

    <el-empty
      v-if="partition.tasks.length === 0"
      :description="t('collaboration.workItems.empty')"
    />

    <div v-else class="tasks-table-wrap">
      <AppDataTable
        activatable
        :data="paginatedRows"
        class="tasks-table"
        highlight-current-row
        :default-sort="{ prop: 'createdAt', order: 'descending' }"
        :row-aria-label="(row) => (row as WorkflowTask).entityName"
        @row-click="(row: WorkflowTask) => emit('open', row.path)"
      >
        <el-table-column sortable :sort-method="sortTasksByTitle" min-width="200">
          <template #header>
            <TableColumnHeader
              :label="t('dashboard.tasks.columns.action')"
              v-model="columnFilters.action"
            />
          </template>
          <template #default="{ row }">
            <strong>{{ t(row.titleKey) }}</strong>
          </template>
        </el-table-column>

        <el-table-column prop="entityName" sortable min-width="220">
          <template #header>
            <TableColumnHeader
              :label="t('dashboard.tasks.columns.item')"
              v-model="columnFilters.item"
            />
          </template>
          <template #default="{ row }">
            {{ row.entityName }}
          </template>
        </el-table-column>

        <el-table-column sortable :sort-method="sortTasksByGroup" width="140">
          <template #header>
            <TableColumnHeader
              :label="t('dashboard.tasks.columns.group')"
              v-model="columnFilters.group"
            />
          </template>
          <template #default="{ row }">
            {{ row.groupCode ?? '—' }}
          </template>
        </el-table-column>

        <template v-if="isCollaboration">
          <el-table-column sortable :sort-method="sortTasksByTrigger" min-width="240">
            <template #header>
              <TableColumnHeader
                :label="t('collaboration.workItems.columns.trigger')"
                v-model="columnFilters.trigger"
              />
            </template>
            <template #default="{ row }">
              {{
                row.triggerType
                  ? t(`collaboration.workItem.trigger.${row.triggerType}.description`)
                  : '—'
              }}
            </template>
          </el-table-column>

          <el-table-column sortable :sort-method="sortTasksBySummary" min-width="220">
            <template #header>
              <TableColumnHeader
                :label="t('collaboration.workItems.columns.summary')"
                v-model="columnFilters.summary"
              />
            </template>
            <template #default="{ row }">
              <span class="summary-cell" :title="row.summaryText">{{ row.summaryText ?? '—' }}</span>
            </template>
          </el-table-column>

          <el-table-column sortable :sort-method="sortTasksByAge" width="160">
            <template #header>
              <TableColumnHeader
                :label="t('collaboration.workItems.columns.age')"
                v-model="columnFilters.age"
              />
            </template>
            <template #default="{ row }">
              <span class="age-cell">
                {{
                  row.ageSeconds !== undefined
                    ? formatCollaborationAgeSeconds(row.ageSeconds)
                    : '—'
                }}
                <el-tag
                  v-if="showOverdueBadge(row)"
                  type="danger"
                  size="small"
                  class="overdue-badge"
                >
                  {{ t('collaboration.workItems.badge.overdue') }}
                </el-tag>
              </span>
            </template>
          </el-table-column>

          <el-table-column sortable :sort-method="sortTasksBySubmitter" width="140">
            <template #header>
              <TableColumnHeader
                :label="t('collaboration.workItems.columns.submitter')"
                v-model="columnFilters.submitter"
              />
            </template>
            <template #default="{ row }">
              {{ resolveSubmitterDisplay(row.submitterUserId, row.submitterDisplayName) }}
            </template>
          </el-table-column>
        </template>

        <el-table-column v-else sortable :sort-method="sortTasksByHint" min-width="260">
          <template #header>
            <TableColumnHeader
              :label="t('dashboard.tasks.columns.hint')"
              v-model="columnFilters.hint"
            />
          </template>
          <template #default="{ row }">
            {{ t(row.descriptionKey) }}
          </template>
        </el-table-column>

        <el-table-column width="100" align="right" fixed="right">
          <template #header>
            <span>{{ t('dashboard.tasks.actions.open') }}</span>
          </template>
          <template #default="{ row }">
            <el-button type="primary" link @click="openTask(row.path, $event)">
              {{ t('dashboard.tasks.actions.open') }}
            </el-button>
          </template>
        </el-table-column>
      </AppDataTable>

      <AppTablePagination
        v-model:current-page="currentPage"
        :page-size="CLIENT_TABLE_PAGE_SIZE"
        :total="totalRows"
      />
    </div>
  </section>
</template>

<style scoped lang="scss">
.task-partition {
  margin-bottom: 1.5rem;
}

.partition-header {
  margin-bottom: 0.75rem;

  h3 {
    margin: 0;
    font-size: 1.05rem;
    font-weight: 600;
  }
}

.tasks-table {
  cursor: pointer;
}

.tasks-table-wrap {
  outline: none;
}

.summary-cell {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}

.age-cell {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  flex-wrap: wrap;
}

.overdue-badge {
  flex-shrink: 0;
}
</style>
