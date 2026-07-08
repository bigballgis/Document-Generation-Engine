export function truncateEntityId(id: string, visibleChars = 8): string {
  const trimmed = id.trim()
  if (!trimmed) {
    return ''
  }
  if (trimmed.length <= visibleChars + 3) {
    return trimmed
  }
  return `${trimmed.slice(0, visibleChars)}…`
}

export interface AuditTemplateDisplayFields {
  templateId?: string
  templateDisplayName?: string
  templateExternalId?: string
}

export function resolveAuditTemplateDisplay(fields: AuditTemplateDisplayFields): {
  label: string
  subtitle?: string
} {
  const externalId = fields.templateExternalId?.trim()
  const displayName = fields.templateDisplayName?.trim()
  const label =
    displayName ||
    externalId ||
    (fields.templateId ? truncateEntityId(fields.templateId) : '') ||
    '—'

  const subtitle =
    externalId && externalId !== label ? externalId : undefined

  return { label, subtitle }
}

export interface AuditActorDisplayFields {
  actorDisplayName?: string
  actorSummary?: string
  actorId?: string
}

export function resolveAuditActorDisplay(fields: AuditActorDisplayFields): string {
  if (fields.actorDisplayName?.trim()) {
    return fields.actorDisplayName.trim()
  }
  if (fields.actorSummary?.trim()) {
    return fields.actorSummary.trim()
  }
  return '—'
}
