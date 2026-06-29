import { describe, expect, it } from 'vitest'
import {
  isAgeOverdueForQueue,
  resolveEffectiveTimeoutConfig,
  resolveThresholdHoursForQueue,
} from '@/utils/collaborationTimeoutThreshold'

const globalConfig = {
  scopeType: 'GLOBAL' as const,
  groupCode: null,
  testThresholdHours: 24,
  approvalThresholdHours: 48,
  pendingReleaseThresholdHours: 12,
  remediationThresholdHours: 72,
  updatedAt: '2026-06-01T00:00:00Z',
}

describe('collaborationTimeoutThreshold', () => {
  it('resolves threshold hours per queue from config', () => {
    expect(resolveThresholdHoursForQueue('TEST', globalConfig)).toBe(24)
    expect(resolveThresholdHoursForQueue('APPROVAL', globalConfig)).toBe(48)
    expect(resolveThresholdHoursForQueue('REMEDIATION', globalConfig)).toBe(72)
    expect(resolveThresholdHoursForQueue('PENDING_RELEASE', globalConfig)).toBe(12)
    expect(resolveThresholdHoursForQueue('ESCALATION', globalConfig)).toBe(0)
  })

  it('prefers group override config when present', () => {
    const groupConfig = {
      ...globalConfig,
      scopeType: 'GROUP' as const,
      groupCode: 'RETAIL',
      testThresholdHours: 6,
    }
    expect(resolveEffectiveTimeoutConfig(globalConfig, groupConfig)?.testThresholdHours).toBe(6)
    expect(resolveEffectiveTimeoutConfig(globalConfig, null)).toBe(globalConfig)
  })

  it('marks ESCALATION queue rows always overdue', () => {
    expect(isAgeOverdueForQueue('ESCALATION', 60, null)).toBe(true)
  })

  it('marks TEST rows overdue when age meets threshold', () => {
    expect(isAgeOverdueForQueue('TEST', 86400, 24)).toBe(true)
    expect(isAgeOverdueForQueue('TEST', 3600, 24)).toBe(false)
  })

  it('suppresses overdue badge when threshold unavailable', () => {
    expect(isAgeOverdueForQueue('TEST', 86400, null)).toBe(false)
  })
})
