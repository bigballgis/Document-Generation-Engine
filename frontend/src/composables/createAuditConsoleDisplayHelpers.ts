import type { RouteLocationRaw } from 'vue-router'
import { ROUTE_KEYS, templatePackageHubPath } from '@/routing/routeKeys'
import type { ManagementAuditEvent } from '@/types/audit'
import type { TemplateLifecycleStatus } from '@/types/template'
import { resolveAuditActorDisplay, resolveAuditTemplateDisplay } from '@/utils/auditEntityDisplay'
import type { AuditActorDisplayFields } from '@/utils/auditEntityDisplay'
import { formatAuditEventType } from '@/utils/auditEventLabels'

type Translate = (key: string) => string
type HasKey = (key: string) => boolean

export function createAuditConsoleDisplayHelpers(options: {
  t: Translate
  te: HasKey
  formatDateTime: (value: string) => string
  canLinkTemplates: () => boolean
}) {
  const { t, te, formatDateTime, canLinkTemplates } = options

  const eventLabelTranslator = {
    translate: t,
    hasKey: te,
  }

  function formatLifecycleState(state?: string) {
    if (!state) {
      return '—'
    }
    const key = `templates.status.${state as TemplateLifecycleStatus}`
    return te(key) ? t(key) : state
  }

  function formatDate(value: string) {
    return formatDateTime(value)
  }

  function formatEventType(eventType?: string) {
    if (!eventType) {
      return '—'
    }
    return formatAuditEventType(eventType, eventLabelTranslator)
  }

  function formatActor(event: AuditActorDisplayFields) {
    return resolveAuditActorDisplay(event)
  }

  function resolveTemplateCell(
    event: Pick<
      ManagementAuditEvent,
      'templateId' | 'templateDisplayName' | 'templateExternalId'
    >,
  ) {
    const display = resolveAuditTemplateDisplay(event)
    const to: RouteLocationRaw | undefined =
      event.templateId && canLinkTemplates()
        ? templatePackageHubPath(event.templateId)
        : undefined
    return { ...display, to }
  }

  return {
    formatLifecycleState,
    formatDate,
    formatEventType,
    formatActor,
    resolveTemplateCell,
    ROUTE_KEYS,
  }
}
