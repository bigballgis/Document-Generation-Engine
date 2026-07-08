import { describe, expect, it } from 'vitest'
import {
  collaborationWorkItemToTask,
  formatCollaborationAgeSeconds,
} from '@/utils/collaborationWorkItems'

describe('collaborationWorkItems utils', () => {
  it('maps API work item to dashboard workflow task', () => {
    const task = collaborationWorkItemToTask({
      workItemId: 'wi-1',
      templateId: 'tpl-1',
      templateName: 'Loan Notice',
      groupCode: 'RETAIL',
      queue: 'TEST',
      triggerType: 'SUBMIT_FOR_TEST',
      submitterUserId: '10000003',
      summaryText: 'Template submitted for testing',
      createdAt: '2026-06-26T10:00:00Z',
      ageSeconds: 7200,
    })

    expect(task.source).toBe('collaboration')
    expect(task.kind).toBe('template-test')
    expect(task.path).toBe('/templates/tpl-1?tab=lifecycle')
    expect(task.submitterUserId).toBe('10000003')
    expect(task.submitterDisplayName).toBeUndefined()
    expect(task.ageSeconds).toBe(7200)
  })

  it('maps submitterDisplayName when provided by API', () => {
    const task = collaborationWorkItemToTask({
      workItemId: 'wi-2',
      templateId: 'tpl-1',
      templateName: 'Loan Notice',
      groupCode: 'RETAIL',
      queue: 'TEST',
      triggerType: 'SUBMIT_FOR_TEST',
      submitterUserId: '10000003',
      submitterDisplayName: 'Carol Tester',
      summaryText: 'Template submitted for testing',
      createdAt: '2026-06-26T10:00:00Z',
      ageSeconds: 3600,
    })

    expect(task.submitterDisplayName).toBe('Carol Tester')
  })

  it('maps ESCALATION queue to template-escalation kind', () => {
    const task = collaborationWorkItemToTask({
      workItemId: 'wi-esc',
      templateId: 'tpl-esc',
      templateName: 'Escalated',
      groupCode: 'RETAIL',
      queue: 'ESCALATION',
      triggerType: 'TIMEOUT_ESCALATION',
      submitterUserId: '10000001',
      summaryText: 'Overdue follow-up',
      createdAt: '2026-06-26T10:00:00Z',
      ageSeconds: 120,
    })

    expect(task.kind).toBe('template-escalation')
    expect(task.queue).toBe('ESCALATION')
  })

  it('formats age seconds into compact units', () => {
    expect(formatCollaborationAgeSeconds(30)).toBe('0m')
    expect(formatCollaborationAgeSeconds(120)).toBe('2m')
    expect(formatCollaborationAgeSeconds(7200)).toBe('2h')
    expect(formatCollaborationAgeSeconds(90000)).toBe('1d')
  })
})
