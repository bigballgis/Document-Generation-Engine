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
    expect(task.ageSeconds).toBe(7200)
  })

  it('formats age seconds into compact units', () => {
    expect(formatCollaborationAgeSeconds(30)).toBe('0m')
    expect(formatCollaborationAgeSeconds(120)).toBe('2m')
    expect(formatCollaborationAgeSeconds(7200)).toBe('2h')
    expect(formatCollaborationAgeSeconds(90000)).toBe('1d')
  })
})
