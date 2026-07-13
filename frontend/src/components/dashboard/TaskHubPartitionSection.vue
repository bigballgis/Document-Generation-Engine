<script setup lang="ts">
import { toRef } from 'vue'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import { useTaskHubPartitionSection } from '@/components/dashboard/useTaskHubPartitionSection'
import type { TaskPartition, WorkflowTask } from '@/composables/useWorkflowTasks'
import type { CollaborationTimeoutConfig } from '@/types/collaboration'

const props = defineProps<{
  partition: TaskPartition
  globalTimeoutConfig: CollaborationTimeoutConfig | null
  groupTimeoutConfigs: Record<string, CollaborationTimeoutConfig | null>
}>()

const emit = defineEmits<{
  open: [path: string]
}>()

const {
  t,
  CLIENT_TABLE_PAGE_SIZE,
  isCollaboration,
  columnFilters,
  currentPage,
  paginatedRows,
  totalRows,
  sortTasksByTitle,
  sortTasksByGroup,
  sortTasksByHint,
  sortTasksByTrigger,
  sortTasksBySummary,
  sortTasksBySubmitter,
  sortTasksByAge,
  showOverdueBadge,
  openTask,
  formatCollaborationAgeSeconds,
  resolveSubmitterDisplay,
} = useTaskHubPartitionSection({
  partition: toRef(props, 'partition'),
  globalTimeoutConfig: toRef(props, 'globalTimeoutConfig'),
  groupTimeoutConfigs: toRef(props, 'groupTimeoutConfigs'),
  emitOpen: (path) => emit('open', path),
})
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
