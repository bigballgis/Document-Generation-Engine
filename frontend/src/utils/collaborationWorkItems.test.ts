import { describe, expect, it } from 'vitest'
import {
  collaborationWorkItemPath,
  collaborationWorkItemToTask,
  formatCollaborationAgeSeconds,
} from '@/utils/collaborationWorkItems'

const baseItem = {
  workItemId: 'wi-1',
  templateId: 'tpl-1',
  templateName: 'Loan Notice',
  groupCode: 'RETAIL' as const,
  triggerType: 'SUBMIT_FOR_TEST' as const,
  submitterUserId: '10000003',
  summaryText: 'Template submitted for testing',
  createdAt: '2026-06-26T10:00:00Z',
  ageSeconds: 7200,
}

describe('collaborationWorkItems utils', () => {
  it('maps API work item to dashboard workflow task', () => {
    const task = collaborationWorkItemToTask({
      ...baseItem,
      queue: 'TEST',
    })

    expect(task.source).toBe('collaboration')
    expect(task.kind).toBe('template-test')
    expect(task.submitterUserId).toBe('10000003')
    expect(task.submitterDisplayName).toBeUndefined()
    expect(task.ageSeconds).toBe(7200)
  })

  it('BDD-CE-U14-DLT-001: TEST path deep-links to testing workspace when devVersion known', () => {
    const path = collaborationWorkItemPath({ templateId: 'tpl-1', queue: 'TEST' }, 'dev-9')
    expect(path).toBe(
      '/templates/tpl-1/dev/dev-9?workspaceTab=testing&testingTab=previewRuns',
    )
    const task = collaborationWorkItemToTask({ ...baseItem, queue: 'TEST' }, 'dev-9')
    expect(task.path).toBe(path)
  })

  it('BDD-CE-U14-DLT-002: APPROVAL path deep-links to submitApproval workspace', () => {
    expect(collaborationWorkItemPath({ templateId: 'tpl-appr', queue: 'APPROVAL' }, 'dev-2')).toBe(
      '/templates/tpl-appr/dev/dev-2?workspaceTab=approval&approvalTab=submitApproval',
    )
  })

  it('BDD-CE-U14-DLT-003: PENDING_RELEASE path deep-links to publishReadiness', () => {
    expect(
      collaborationWorkItemPath({ templateId: 'tpl-pub', queue: 'PENDING_RELEASE' }, 'dev-3'),
    ).toBe(
      '/templates/tpl-pub/dev/dev-3?workspaceTab=approval&approvalTab=publishReadiness',
    )
  })

  it('BDD-CE-U14-DLT-001/D3: without devVersionId, hub path preserves queue + workspace hints', () => {
    const path = collaborationWorkItemPath({ templateId: 'tpl-1', queue: 'TEST' })
    expect(path).toContain('/templates/tpl-1?')
    expect(path).toContain('tab=lifecycle')
    expect(path).toContain('queue=TEST')
    expect(path).toContain('workspaceTab=testing')
    expect(path).toContain('testingTab=previewRuns')
  })

  it('maps submitterDisplayName when provided by API', () => {
    const task = collaborationWorkItemToTask({
      ...baseItem,
      queue: 'TEST',
      submitterDisplayName: 'Carol Tester',
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
    expect(task.path).toContain('queue=ESCALATION')
    expect(task.path).toContain('workspaceTab=approval')
  })

  it('formats age seconds into compact units', () => {
    expect(formatCollaborationAgeSeconds(30)).toBe('0m')
    expect(formatCollaborationAgeSeconds(120)).toBe('2m')
    expect(formatCollaborationAgeSeconds(7200)).toBe('2h')
    expect(formatCollaborationAgeSeconds(90000)).toBe('1d')
  })
})
