import { rowSortMethod } from '@/composables/useDataTableFilters'
import type { LifecycleAuditEvent, ManagementAuditEvent } from '@/types/audit'
import type { AuditActorDisplayFields } from '@/utils/auditEntityDisplay'
import { resolveAuditTemplateDisplay } from '@/utils/auditEntityDisplay'

export function createAuditConsoleSorts(options: {
  formatActor: (event: AuditActorDisplayFields) => string
  formatEventType: (eventType?: string) => string
  formatLifecycleState: (state?: string) => string
}) {
  const { formatActor, formatEventType, formatLifecycleState } = options

  return {
    sortManagementByActor: rowSortMethod<ManagementAuditEvent>((row) => formatActor(row)),
    sortManagementByTemplate: rowSortMethod<ManagementAuditEvent>(
      (row) => resolveAuditTemplateDisplay(row).label,
    ),
    sortLifecycleByActor: rowSortMethod<LifecycleAuditEvent>((row) => formatActor(row)),
    sortLifecycleByTemplate: rowSortMethod<LifecycleAuditEvent>(
      (row) => resolveAuditTemplateDisplay(row).label,
    ),
    sortManagementByEventType: rowSortMethod<ManagementAuditEvent>((row) =>
      formatEventType(row.eventType),
    ),
    sortManagementByEventAt: rowSortMethod<ManagementAuditEvent>((row) => row.eventAt),
    sortLifecycleByEventType: rowSortMethod<LifecycleAuditEvent>((row) =>
      formatEventType(row.eventType),
    ),
    sortLifecycleByEventAt: rowSortMethod<LifecycleAuditEvent>((row) => row.eventAt),
    sortLifecycleFromState: rowSortMethod<LifecycleAuditEvent>((row) =>
      formatLifecycleState(row.fromState),
    ),
    sortLifecycleToState: rowSortMethod<LifecycleAuditEvent>((row) =>
      formatLifecycleState(row.toState),
    ),
  }
}
