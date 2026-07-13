<script setup lang="ts">
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import type { WorkflowTask } from '@/composables/useWorkflowTasks'

defineProps<{
  t: (key: string) => string
  triggerFilter: string
  summaryFilter: string
  ageFilter: string
  submitterFilter: string
  sortTasksByTrigger: (a: WorkflowTask, b: WorkflowTask) => number
  sortTasksBySummary: (a: WorkflowTask, b: WorkflowTask) => number
  sortTasksByAge: (a: WorkflowTask, b: WorkflowTask) => number
  sortTasksBySubmitter: (a: WorkflowTask, b: WorkflowTask) => number
  showOverdueBadge: (row: WorkflowTask) => boolean
  formatCollaborationAgeSeconds: (ageSeconds: number) => string
  resolveSubmitterDisplay: (
    submitterUserId: string | undefined,
    submitterDisplayName: string | undefined,
  ) => string
}>()

const emit = defineEmits<{
  'update:triggerFilter': [value: string]
  'update:summaryFilter': [value: string]
  'update:ageFilter': [value: string]
  'update:submitterFilter': [value: string]
}>()
</script>

<template>
  <el-table-column sortable :sort-method="sortTasksByTrigger" min-width="240">
    <template #header>
      <TableColumnHeader
        :label="t('collaboration.workItems.columns.trigger')"
        :model-value="triggerFilter"
        @update:model-value="emit('update:triggerFilter', $event)"
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
        :model-value="summaryFilter"
        @update:model-value="emit('update:summaryFilter', $event)"
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
        :model-value="ageFilter"
        @update:model-value="emit('update:ageFilter', $event)"
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
        :model-value="submitterFilter"
        @update:model-value="emit('update:submitterFilter', $event)"
      />
    </template>
    <template #default="{ row }">
      {{ resolveSubmitterDisplay(row.submitterUserId, row.submitterDisplayName) }}
    </template>
  </el-table-column>
</template>
