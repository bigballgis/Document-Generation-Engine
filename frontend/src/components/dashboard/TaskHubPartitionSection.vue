<script setup lang="ts">
import { toRef } from 'vue'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import EntityLinkCell from '@/components/common/EntityLinkCell.vue'
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import TaskHubCollaborationColumns from '@/components/dashboard/TaskHubCollaborationColumns.vue'
import { useTaskHubPartitionSection } from '@/components/dashboard/useTaskHubPartitionSection'
import { useEntityLinkTargets } from '@/composables/useEntityLinkTargets'
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

const { groupCatalogLink, taskEntityLink } = useEntityLinkTargets()
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
            <EntityLinkCell :label="row.entityName" :to="taskEntityLink(row)" />
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
            <EntityLinkCell
              v-if="row.groupCode"
              :label="row.groupCode"
              :to="groupCatalogLink(row.groupCode)"
            />
            <span v-else>—</span>
          </template>
        </el-table-column>

        <TaskHubCollaborationColumns
          v-if="isCollaboration"
          :t="t"
          :trigger-filter="columnFilters.trigger"
          :summary-filter="columnFilters.summary"
          :age-filter="columnFilters.age"
          :submitter-filter="columnFilters.submitter"
          :sort-tasks-by-trigger="sortTasksByTrigger"
          :sort-tasks-by-summary="sortTasksBySummary"
          :sort-tasks-by-age="sortTasksByAge"
          :sort-tasks-by-submitter="sortTasksBySubmitter"
          :show-overdue-badge="showOverdueBadge"
          :format-collaboration-age-seconds="formatCollaborationAgeSeconds"
          :resolve-submitter-display="resolveSubmitterDisplay"
          @update:trigger-filter="columnFilters.trigger = $event"
          @update:summary-filter="columnFilters.summary = $event"
          @update:age-filter="columnFilters.age = $event"
          @update:submitter-filter="columnFilters.submitter = $event"
        />

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

<style scoped lang="scss" src="./TaskHubPartitionSection.scss"></style>
