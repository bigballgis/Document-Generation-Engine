import type {
  CollaborationTimeoutConfig,
  CollaborationWorkItemQueue,
} from '@/types/collaboration'

export function resolveThresholdHoursForQueue(
  queue: CollaborationWorkItemQueue,
  config: CollaborationTimeoutConfig,
): number {
  switch (queue) {
    case 'TEST':
      return config.testThresholdHours
    case 'APPROVAL':
      return config.approvalThresholdHours
    case 'REMEDIATION':
      return config.remediationThresholdHours
    case 'PENDING_RELEASE':
      return config.pendingReleaseThresholdHours
    case 'ESCALATION':
      return 0
  }
}

export function resolveEffectiveTimeoutConfig(
  globalConfig: CollaborationTimeoutConfig | null,
  groupConfig: CollaborationTimeoutConfig | null | undefined,
): CollaborationTimeoutConfig | null {
  if (groupConfig?.scopeType === 'GROUP' && groupConfig.groupCode) {
    return groupConfig
  }
  return globalConfig
}

export function isAgeOverdueForQueue(
  queue: CollaborationWorkItemQueue,
  ageSeconds: number,
  thresholdHours: number | null,
): boolean {
  if (queue === 'ESCALATION') {
    return true
  }
  if (thresholdHours === null) {
    return false
  }
  return ageSeconds >= thresholdHours * 3600
}
