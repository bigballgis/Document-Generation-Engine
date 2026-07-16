import { describe, expect, it } from 'vitest'
import {
  LIFECYCLE_STEPPER_STEP_IDS,
  resolveLifecycleStepperModel,
  resolveLifecycleStepperStepStatus,
  resolveLifecycleStepperWorkspaceQuery,
} from '@/utils/templateLifecycleStepper'

describe('templateLifecycleStepper (BDD-CE-U15-LSS)', () => {
  it('LSS-001: DRAFT maps to Draft current with later steps upcoming', () => {
    const model = resolveLifecycleStepperModel('DRAFT')
    expect(model).toEqual({ terminal: false, currentIndex: 0 })
    expect(resolveLifecycleStepperStepStatus(0, model)).toBe('current')
    expect(resolveLifecycleStepperStepStatus(1, model)).toBe('upcoming')
    expect(resolveLifecycleStepperStepStatus(5, model)).toBe('upcoming')
  })

  it('LSS-002: advances through testing / approval / release / published', () => {
    expect(resolveLifecycleStepperModel('TESTING').currentIndex).toBe(1)

    expect(resolveLifecycleStepperModel('APPROVAL', 'PENDING_SUBMIT').currentIndex).toBe(2)
    expect(resolveLifecycleStepperModel('APPROVAL', null).currentIndex).toBe(2)
    expect(resolveLifecycleStepperModel('APPROVAL', undefined).currentIndex).toBe(2)
    expect(resolveLifecycleStepperModel('APPROVAL', 'PENDING_DECISION').currentIndex).toBe(3)

    expect(resolveLifecycleStepperModel('PENDING_RELEASE').currentIndex).toBe(4)
    expect(resolveLifecycleStepperModel('PUBLISHED').currentIndex).toBe(5)

    const testing = resolveLifecycleStepperModel('TESTING')
    expect(resolveLifecycleStepperStepStatus(0, testing)).toBe('completed')
    expect(resolveLifecycleStepperStepStatus(1, testing)).toBe('current')
  })

  it('LSS-003: STOPPED / DEPRECATED are terminal and do not fake a linear current step', () => {
    for (const status of ['STOPPED', 'DEPRECATED'] as const) {
      const model = resolveLifecycleStepperModel(status)
      expect(model.terminal).toBe(true)
      expect(model.currentIndex).toBeNull()
      for (let i = 0; i < LIFECYCLE_STEPPER_STEP_IDS.length; i += 1) {
        expect(resolveLifecycleStepperStepStatus(i, model)).toBe('inactive')
      }
    }
  })

  it('LSS-007: step clicks only resolve orientation workspace queries', () => {
    expect(resolveLifecycleStepperWorkspaceQuery('draft')).toEqual({ workspaceTab: 'design' })
    expect(resolveLifecycleStepperWorkspaceQuery('testing')).toEqual({ workspaceTab: 'testing' })
    expect(resolveLifecycleStepperWorkspaceQuery('readyForApproval')).toEqual({
      workspaceTab: 'approval',
      approvalTab: 'submitApproval',
    })
    expect(resolveLifecycleStepperWorkspaceQuery('pendingApproval')).toEqual({
      workspaceTab: 'approval',
      approvalTab: 'submitApproval',
    })
    expect(resolveLifecycleStepperWorkspaceQuery('pendingRelease')).toEqual({
      workspaceTab: 'approval',
      approvalTab: 'publishReadiness',
    })
    expect(resolveLifecycleStepperWorkspaceQuery('published')).toBeNull()
  })

  it('exposes six product steps in order', () => {
    expect([...LIFECYCLE_STEPPER_STEP_IDS]).toEqual([
      'draft',
      'testing',
      'readyForApproval',
      'pendingApproval',
      'pendingRelease',
      'published',
    ])
  })
})
