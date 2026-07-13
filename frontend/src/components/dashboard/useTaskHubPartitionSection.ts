import { computed, ref, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { rowSortMethod, useDataTableFilters } from '@/composables/useDataTableFilters'
import { useCatalogPagination } from '@/composables/useCatalogPagination'
import { CLIENT_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import type { TaskPartition, WorkflowTask } from '@/composables/useWorkflowTasks'
import { formatCollaborationAgeSeconds } from '@/utils/collaborationWorkItems'
import { resolveSubmitterDisplay } from '@/utils/userDisplay'
import {
  isAgeOverdueForQueue,
  resolveEffectiveTimeoutConfig,
  resolveThresholdHoursForQueue,
} from '@/utils/collaborationTimeoutThreshold'
import type { CollaborationTimeoutConfig } from '@/types/collaboration'

export function useTaskHubPartitionSection(options: {
  partition: Ref<TaskPartition>
  globalTimeoutConfig: Ref<CollaborationTimeoutConfig | null>
  groupTimeoutConfigs: Ref<Record<string, CollaborationTimeoutConfig | null>>
  emitOpen: (path: string) => void
}) {
  const { t } = useI18n()
  // Partition kind is stable for a mounted section instance.
  const isCollaboration = options.partition.value.kind === 'collaboration'

  const filterDefs = isCollaboration
    ? [
        { key: 'action', getValue: (row: WorkflowTask) => t(row.titleKey) },
        { key: 'item', getValue: (row: WorkflowTask) => row.entityName },
        { key: 'group', getValue: (row: WorkflowTask) => row.groupCode ?? '' },
        {
          key: 'trigger',
          getValue: (row: WorkflowTask) =>
            row.triggerType
              ? t(`collaboration.workItem.trigger.${row.triggerType}.description`)
              : '',
        },
        { key: 'summary', getValue: (row: WorkflowTask) => row.summaryText ?? '' },
        {
          key: 'age',
          getValue: (row: WorkflowTask) =>
            row.ageSeconds !== undefined ? formatCollaborationAgeSeconds(row.ageSeconds) : '',
        },
        {
          key: 'submitter',
          getValue: (row: WorkflowTask) =>
            resolveSubmitterDisplay(row.submitterUserId, row.submitterDisplayName),
        },
      ]
    : [
        { key: 'action', getValue: (row: WorkflowTask) => t(row.titleKey) },
        { key: 'item', getValue: (row: WorkflowTask) => row.entityName },
        { key: 'group', getValue: (row: WorkflowTask) => row.groupCode ?? '' },
        { key: 'hint', getValue: (row: WorkflowTask) => t(row.descriptionKey) },
      ]

  const partitionTasks = computed(() => options.partition.value.tasks)
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
      options.globalTimeoutConfig.value,
      groupCode ? options.groupTimeoutConfigs.value[groupCode] : null,
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
    options.emitOpen(path)
  }

  return {
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
  }
}
