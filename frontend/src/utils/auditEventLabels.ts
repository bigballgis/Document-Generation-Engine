export function auditEventTypeMessageKey(eventType: string): string {
  return `audit.eventTypes.${eventType}`
}

export interface AuditEventLabelTranslator {
  translate: (key: string) => string
  hasKey: (key: string) => boolean
}

export function formatAuditEventType(
  eventType: string,
  { translate, hasKey }: AuditEventLabelTranslator,
): string {
  if (!eventType) {
    return ''
  }
  const key = auditEventTypeMessageKey(eventType)
  if (hasKey(key)) {
    return translate(key)
  }
  return eventType
}
