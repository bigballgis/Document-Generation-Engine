import { describe, expect, it } from 'vitest'
import {
  templateAuthorJourneySteps,
  templateTeamLeadJourneySteps,
} from '@/constants/roleJourneyDefinitions'
import {
  resolvePrimaryTourRole,
  resolveTourStepsForRole,
} from '@/utils/resolvePrimaryTourRole'

describe('resolvePrimaryTourRole', () => {
  const caps = {
    decideApprovals: false,
    decideLegalApprovals: false,
    publishTemplates: false,
    reviewMasters: false,
  }

  it('resolves DOCUMENT_AUTHOR for author-only session (BDD-SYS-NORM-ROLE-014)', () => {
    expect(resolvePrimaryTourRole({ roles: ['DOCUMENT_AUTHOR'], ...caps })).toBe(
      'DOCUMENT_AUTHOR',
    )
  })

  it('falls back to AUDIT_ADMIN when no higher journey role matches (C8-C5)', () => {
    expect(resolvePrimaryTourRole({ roles: ['AUDIT_ADMIN'], ...caps })).toBe('AUDIT_ADMIN')
  })

  it('returns null when no tour role can be resolved (BDD-LRP-C8-009)', () => {
    expect(resolvePrimaryTourRole({ roles: [], ...caps })).toBeNull()
    expect(resolvePrimaryTourRole({ roles: ['SOME_UNKNOWN'], ...caps })).toBeNull()
  })

  it('uses GROUP_ADMIN team-lead tour when publish/review caps apply (ex-approver remap)', () => {
    expect(
      resolvePrimaryTourRole({
        roles: ['GROUP_ADMIN'],
        decideApprovals: true,
        decideLegalApprovals: false,
        publishTemplates: true,
        reviewMasters: true,
      }),
    ).toBe('GROUP_ADMIN')
  })

  it('uses GLOBAL_ADMIN before GROUP_ADMIN', () => {
    expect(
      resolvePrimaryTourRole({
        roles: ['GLOBAL_ADMIN', 'GROUP_ADMIN'],
        decideApprovals: false,
        decideLegalApprovals: false,
        publishTemplates: true,
        reviewMasters: true,
      }),
    ).toBe('GLOBAL_ADMIN')
  })
})

describe('resolveTourStepsForRole', () => {
  it('returns DOCUMENT_AUTHOR authoring steps from roleJourneyDefinitions (BDD-LRP-C8-006)', () => {
    const steps = resolveTourStepsForRole('DOCUMENT_AUTHOR')
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

  it('returns GROUP_ADMIN team-lead steps (ex-TEMPLATE_APPROVER absorbed)', () => {
    const steps = resolveTourStepsForRole('GROUP_ADMIN')
    expect(steps).toBe(templateTeamLeadJourneySteps)
    expect(steps[0]?.id).toBe('reviewLetterhead')
  })
})
