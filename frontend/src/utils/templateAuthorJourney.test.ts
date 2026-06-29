import { describe, expect, it } from 'vitest'
import {
  isTemplateInRemediation,
  resolveTemplateAuthorDashboardJourneyIndex,
  resolveTemplateAuthorJourneyIndex,
  shouldShowTemplateAuthorJourney,
} from '@/utils/templateAuthorJourney'
import type {
  TemplateAuthorDashboardTemplate,
  TemplateAuthorRemediationItem,
} from '@/utils/templateAuthorJourney'

function templateSummary(
  overrides: Partial<TemplateAuthorDashboardTemplate> &
    Pick<TemplateAuthorDashboardTemplate, 'id' | 'lifecycleStatus'>,
): TemplateAuthorDashboardTemplate {
  return {
    externalId: 'TPL-001',
    groupCode: 'RETAIL',
    name: 'Retail letter',
    releaseVersion: null,
    releaseVersionCount: 0,
    masterId: 'master-1',
    updatedBy: '10000005',
    updatedAt: '2026-06-26T10:00:00Z',
    ...overrides,
  }
}

function remediationItem(
  overrides: Partial<TemplateAuthorRemediationItem> &
    Pick<TemplateAuthorRemediationItem, 'templateId'>,
): TemplateAuthorRemediationItem {
  return {
    createdAt: '2026-06-25T10:00:00Z',
    ...overrides,
  }
}

describe('resolveTemplateAuthorJourneyIndex (§12.6 entity mapping)', () => {
  it('maps TESTING to null with waitingTesting guidance', () => {
    expect(
      resolveTemplateAuthorJourneyIndex({
        lifecycleStatus: 'TESTING',
      }),
    ).toEqual({
      currentStepIndex: null,
      guidanceKey: 'journey.roles.TEMPLATE_AUTHOR.waitingTesting.guidance',
    })
  })

  it('maps APPROVAL PENDING_DECISION to null with waitingApproval guidance', () => {
    expect(
      resolveTemplateAuthorJourneyIndex({
        lifecycleStatus: 'APPROVAL',
        approvalSubState: 'PENDING_DECISION',
      }),
    ).toEqual({
      currentStepIndex: null,
      guidanceKey: 'journey.roles.TEMPLATE_AUTHOR.waitingApproval.guidance',
    })
  })

  it('maps missing approvalSubState on APPROVAL to waiting guidance (fail-safe)', () => {
    expect(
      resolveTemplateAuthorJourneyIndex({
        lifecycleStatus: 'APPROVAL',
      }),
    ).toEqual({
      currentStepIndex: null,
      guidanceKey: 'journey.roles.TEMPLATE_AUTHOR.waitingApproval.guidance',
    })
  })

  it('maps APPROVAL PENDING_SUBMIT to submitApproval step 4', () => {
    expect(
      resolveTemplateAuthorJourneyIndex({
        lifecycleStatus: 'APPROVAL',
        approvalSubState: 'PENDING_SUBMIT',
      }),
    ).toEqual({ currentStepIndex: 4, activeStepId: 'submitApproval' })
  })

  it('maps PENDING_RELEASE to awaitGoLive step 5 with team-lead guidance', () => {
    expect(
      resolveTemplateAuthorJourneyIndex({
        lifecycleStatus: 'PENDING_RELEASE',
      }),
    ).toEqual({
      currentStepIndex: 5,
      activeStepId: 'awaitGoLive',
      guidanceKey: 'journey.roles.TEMPLATE_AUTHOR.awaitGoLive.teamLeadGuidance',
    })
  })

  it('maps PUBLISHED to null with complete guidance', () => {
    expect(
      resolveTemplateAuthorJourneyIndex({
        lifecycleStatus: 'PUBLISHED',
      }),
    ).toEqual({
      currentStepIndex: null,
      guidanceKey: 'journey.roles.TEMPLATE_AUTHOR.complete.guidance',
    })
  })

  it('maps DRAFT without bindings to design step 1', () => {
    expect(
      resolveTemplateAuthorJourneyIndex({
        lifecycleStatus: 'DRAFT',
        bindingsCount: 0,
      }),
    ).toEqual({ currentStepIndex: 1, activeStepId: 'design' })
  })

  it('maps DRAFT with bindings and no trial to trialGenerate step 2', () => {
    expect(
      resolveTemplateAuthorJourneyIndex({
        lifecycleStatus: 'DRAFT',
        bindingsCount: 2,
        hasSuccessfulTrialOutput: false,
      }),
    ).toEqual({ currentStepIndex: 2, activeStepId: 'trialGenerate' })
  })

  it('maps DRAFT with bindings and successful trial to submitTest step 3', () => {
    expect(
      resolveTemplateAuthorJourneyIndex({
        lifecycleStatus: 'DRAFT',
        bindingsCount: 1,
        hasSuccessfulTrialOutput: true,
      }),
    ).toEqual({ currentStepIndex: 3, activeStepId: 'submitTest' })
  })

  it('maps remediation DRAFT without trial readiness to design step 1', () => {
    expect(
      resolveTemplateAuthorJourneyIndex({
        lifecycleStatus: 'DRAFT',
        bindingsCount: 2,
        hasSuccessfulTrialOutput: false,
        isRemediation: true,
      }),
    ).toEqual({
      currentStepIndex: 1,
      activeStepId: 'design',
      guidanceKey: 'journey.roles.TEMPLATE_AUTHOR.remediation.guidance',
    })
  })

  it('maps remediation DRAFT ready to resubmit to submitTest step 3', () => {
    expect(
      resolveTemplateAuthorJourneyIndex({
        lifecycleStatus: 'DRAFT',
        bindingsCount: 2,
        hasSuccessfulTrialOutput: true,
        isRemediation: true,
      }),
    ).toEqual({
      currentStepIndex: 3,
      activeStepId: 'submitTest',
      guidanceKey: 'journey.roles.TEMPLATE_AUTHOR.remediation.guidance',
    })
  })

  it('maps terminal STOPPED to null without guidance', () => {
    expect(
      resolveTemplateAuthorJourneyIndex({
        lifecycleStatus: 'STOPPED',
      }),
    ).toEqual({ currentStepIndex: null })
  })
})

describe('isTemplateInRemediation', () => {
  it('returns true when template id is in open remediation set', () => {
    expect(isTemplateInRemediation('tpl-1', new Set(['tpl-1', 'tpl-2']))).toBe(true)
  })

  it('returns false when template id is absent', () => {
    expect(isTemplateInRemediation('tpl-3', new Set(['tpl-1']))).toBe(false)
  })
})

describe('resolveTemplateAuthorDashboardJourneyIndex (§12.6 dashboard mapping)', () => {
  it('maps empty catalog to create step 0', () => {
    expect(resolveTemplateAuthorDashboardJourneyIndex([], [])).toEqual({
      currentStepIndex: 0,
      activeStepId: 'create',
    })
  })

  it('prioritizes remediation over draft ready to submit', () => {
    const resolution = resolveTemplateAuthorDashboardJourneyIndex(
      [
        templateSummary({
          id: 'ready',
          lifecycleStatus: 'DRAFT',
          bindingsCount: 2,
          hasSuccessfulTrialOutput: true,
          updatedAt: '2026-06-27T10:00:00Z',
        }),
      ],
      [remediationItem({ templateId: 'rework', createdAt: '2026-06-26T10:00:00Z' })],
    )
    expect(resolution.currentStepIndex).toBe(1)
    expect(resolution.guidanceKey).toBe('journey.roles.TEMPLATE_AUTHOR.remediation.guidance')
    expect(resolution.targetTemplateId).toBe('rework')
  })

  it('maps single draft ready to submit to step 3', () => {
    expect(
      resolveTemplateAuthorDashboardJourneyIndex(
        [
          templateSummary({
            id: 'draft-ready',
            lifecycleStatus: 'DRAFT',
            bindingsCount: 1,
            hasSuccessfulTrialOutput: true,
          }),
        ],
        [],
      ),
    ).toMatchObject({ currentStepIndex: 3, activeStepId: 'submitTest', targetTemplateId: 'draft-ready' })
  })

  it('maps draft with bindings but no trial to step 2', () => {
    expect(
      resolveTemplateAuthorDashboardJourneyIndex(
        [
          templateSummary({
            id: 'needs-trial',
            lifecycleStatus: 'DRAFT',
            bindingsCount: 2,
            hasSuccessfulTrialOutput: false,
          }),
        ],
        [],
      ),
    ).toMatchObject({ currentStepIndex: 2, activeStepId: 'trialGenerate' })
  })

  it('maps draft without bindings to step 1', () => {
    expect(
      resolveTemplateAuthorDashboardJourneyIndex(
        [templateSummary({ id: 'needs-design', lifecycleStatus: 'DRAFT', bindingsCount: 0 })],
        [],
      ),
    ).toMatchObject({ currentStepIndex: 1, activeStepId: 'design' })
  })

  it('maps approval pending submit to step 4', () => {
    expect(
      resolveTemplateAuthorDashboardJourneyIndex(
        [
          templateSummary({
            id: 'pending-submit',
            lifecycleStatus: 'APPROVAL',
            approvalSubState: 'PENDING_SUBMIT',
          }),
        ],
        [],
      ),
    ).toMatchObject({ currentStepIndex: 4, activeStepId: 'submitApproval' })
  })

  it('maps pending release to step 5 with team-lead guidance', () => {
    expect(
      resolveTemplateAuthorDashboardJourneyIndex(
        [templateSummary({ id: 'pending-release', lifecycleStatus: 'PENDING_RELEASE' })],
        [],
      ),
    ).toEqual({
      currentStepIndex: 5,
      activeStepId: 'awaitGoLive',
      guidanceKey: 'journey.roles.TEMPLATE_AUTHOR.awaitGoLive.teamLeadGuidance',
      targetTemplateId: 'pending-release',
    })
  })

  it('maps testing-only catalog to waitingTesting guidance', () => {
    expect(
      resolveTemplateAuthorDashboardJourneyIndex(
        [templateSummary({ id: 'testing', lifecycleStatus: 'TESTING' })],
        [],
      ),
    ).toEqual({
      currentStepIndex: null,
      guidanceKey: 'journey.roles.TEMPLATE_AUTHOR.waitingTesting.guidance',
    })
  })

  it('maps all published catalog to empty guidance', () => {
    expect(
      resolveTemplateAuthorDashboardJourneyIndex(
        [templateSummary({ id: 'live', lifecycleStatus: 'PUBLISHED' })],
        [],
      ),
    ).toEqual({
      currentStepIndex: null,
      guidanceKey: 'journey.roles.TEMPLATE_AUTHOR.empty.guidance',
    })
  })

  it('tie-breaks remediation items by most recent createdAt', () => {
    const resolution = resolveTemplateAuthorDashboardJourneyIndex(
      [],
      [
        remediationItem({ templateId: 'older', createdAt: '2026-06-20T10:00:00Z' }),
        remediationItem({ templateId: 'newer', createdAt: '2026-06-28T10:00:00Z' }),
      ],
    )
    expect(resolution.targetTemplateId).toBe('newer')
  })
})

describe('shouldShowTemplateAuthorJourney', () => {
  it('shows when authorTemplates capability is granted', () => {
    expect(
      shouldShowTemplateAuthorJourney({
        authorTemplates: true,
        roles: ['TEMPLATE_AUTHOR'],
      }),
    ).toBe(true)
  })

  it('hides when authorTemplates is false', () => {
    expect(
      shouldShowTemplateAuthorJourney({
        authorTemplates: false,
        roles: ['TEMPLATE_TESTER'],
      }),
    ).toBe(false)
  })
})
