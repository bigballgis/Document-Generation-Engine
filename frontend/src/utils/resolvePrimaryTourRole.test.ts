import { describe, expect, it } from 'vitest'
import {
  masterDesignerJourneySteps,
  templateAuthorJourneySteps,
} from '@/constants/roleJourneyDefinitions'
import {
  resolvePrimaryTourRole,
  resolveTourStepsForRole,
} from '@/utils/resolvePrimaryTourRole'

describe('resolvePrimaryTourRole', () => {
  const caps = {
    decideApprovals: false,
    publishTemplates: false,
    reviewMasters: false,
  }

  it('resolves TEMPLATE_AUTHOR for author-only session (BDD-LRP-C8-001)', () => {
    expect(resolvePrimaryTourRole({ roles: ['TEMPLATE_AUTHOR'], ...caps })).toBe(
      'TEMPLATE_AUTHOR',
    )
  })

  it('prefers MASTER_DESIGNER over TEMPLATE_AUTHOR (BDD-LRP-C8-008)', () => {
    expect(
      resolvePrimaryTourRole({
        roles: ['MASTER_DESIGNER', 'TEMPLATE_AUTHOR'],
        ...caps,
      }),
    ).toBe('MASTER_DESIGNER')
  })

  it('falls back to AUDIT_ADMIN when no higher journey role matches (C8-C5)', () => {
    expect(resolvePrimaryTourRole({ roles: ['AUDIT_ADMIN'], ...caps })).toBe('AUDIT_ADMIN')
  })

  it('returns null when no tour role can be resolved (BDD-LRP-C8-009)', () => {
    expect(resolvePrimaryTourRole({ roles: [], ...caps })).toBeNull()
    expect(resolvePrimaryTourRole({ roles: ['SOME_UNKNOWN'], ...caps })).toBeNull()
  })

  it('uses TEMPLATE_APPROVER when cluster-one absent and decideApprovals true', () => {
    expect(
      resolvePrimaryTourRole({
        roles: ['TEMPLATE_APPROVER'],
        decideApprovals: true,
        publishTemplates: false,
        reviewMasters: false,
      }),
    ).toBe('TEMPLATE_APPROVER')
  })

  it('uses GLOBAL_ADMIN before GROUP_ADMIN', () => {
    expect(
      resolvePrimaryTourRole({
        roles: ['GLOBAL_ADMIN', 'GROUP_ADMIN'],
        decideApprovals: false,
        publishTemplates: true,
        reviewMasters: true,
      }),
    ).toBe('GLOBAL_ADMIN')
  })
})

describe('resolveTourStepsForRole', () => {
  it('returns author steps from roleJourneyDefinitions without forking (BDD-LRP-C8-006)', () => {
    const steps = resolveTourStepsForRole('TEMPLATE_AUTHOR')
    expect(steps).toBe(templateAuthorJourneySteps)
    expect(steps.map((s) => s.id)).toEqual([
      'create',
      'design',
      'trialGenerate',
      'submitTest',
      'submitApproval',
      'awaitGoLive',
    ])
  })

  it('returns master designer steps aligned with definitions (BDD-LRP-C8-007)', () => {
    const steps = resolveTourStepsForRole('MASTER_DESIGNER')
    expect(steps).toBe(masterDesignerJourneySteps)
    expect(steps[0]?.id).toBe('upload')
  })
})
