import type { TemplateLifecycleStatus } from '@/types/template'

export type WorkflowFilterKey = 'awaitingTest' | 'awaitingApproval' | 'awaitingPublish'

const WORKFLOW_CHIP_QUERY: Record<
  WorkflowFilterKey,
  { lifecycleStatus: TemplateLifecycleStatus; approvalSubState?: string }
> = {
  awaitingTest: { lifecycleStatus: 'TESTING' },
  awaitingApproval: { lifecycleStatus: 'APPROVAL', approvalSubState: 'PENDING_DECISION' },
  awaitingPublish: { lifecycleStatus: 'PENDING_RELEASE' },
}

export function buildTemplateListQuery(options: {
  searchQuery: string
  groupCode?: string
  statusFilter?: string
  activeWorkflowFilter: WorkflowFilterKey | null
  activeSortKey: string
}) {
  const search = options.searchQuery.trim() || undefined
  const groupCode = options.groupCode?.trim() || undefined
  const statusFilter = options.statusFilter?.trim() || undefined
  const chip = options.activeWorkflowFilter
    ? WORKFLOW_CHIP_QUERY[options.activeWorkflowFilter]
    : null

  let lifecycleStatus: string | undefined
  let approvalSubState: string | undefined
  if (chip && statusFilter && chip.lifecycleStatus !== statusFilter) {
    lifecycleStatus = statusFilter
  } else {
    lifecycleStatus = statusFilter || chip?.lifecycleStatus
    approvalSubState = chip?.approvalSubState
  }

  return {
    search,
    groupCode,
    lifecycleStatus,
    approvalSubState,
    sort: options.activeSortKey || 'groupCodeAsc',
  }
}
