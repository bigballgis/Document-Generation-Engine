import { describe, expect, it } from 'vitest'
import {
  resolveTemplateWorkflowBannerContext,
  resolveWorkflowBannerActionKind,
  type TemplateWorkflowBannerCapabilities,
} from '@/utils/templateWorkflowBannerContext'
import type { TemplateLifecycleStatus } from '@/types/template'

const allFalse: TemplateWorkflowBannerCapabilities = {
  authorTemplates: false,
  decideTests: false,
  decideApprovals: false,
  publishTemplates: false,
}

describe('templateWorkflowBannerContext', () => {
  it('returns testing banner when TESTING and decideTests', () => {
    const context = resolveTemplateWorkflowBannerContext('TESTING', {
      ...allFalse,
      decideTests: true,
    })

    expect(context).toEqual({
      titleKey: 'dashboard.tasks.templateTest.title',
      descriptionKey: 'dashboard.tasks.templateTest.description',
    })
    expect(resolveWorkflowBannerActionKind('TESTING', { ...allFalse, decideTests: true })).toBe(
      'testing',
    )
  })

  it('returns approval banner when APPROVAL and decideApprovals', () => {
    const context = resolveTemplateWorkflowBannerContext('APPROVAL', {
      ...allFalse,
      decideApprovals: true,
    })

    expect(context).toEqual({
      titleKey: 'dashboard.tasks.templateApproval.title',
      descriptionKey: 'dashboard.tasks.templateApproval.description',
    })
    expect(resolveWorkflowBannerActionKind('APPROVAL', { ...allFalse, decideApprovals: true })).toBe(
      'approval',
    )
  })

  it('returns publish banner when PENDING_RELEASE and publishTemplates', () => {
    const context = resolveTemplateWorkflowBannerContext('PENDING_RELEASE', {
      ...allFalse,
      publishTemplates: true,
    })

    expect(context).toEqual({
      titleKey: 'dashboard.tasks.templatePublish.title',
      descriptionKey: 'dashboard.tasks.templatePublish.description',
    })
    expect(
      resolveWorkflowBannerActionKind('PENDING_RELEASE', { ...allFalse, publishTemplates: true }),
    ).toBe('publish')
  })

  it('returns draft banner when DRAFT and authorTemplates', () => {
    const context = resolveTemplateWorkflowBannerContext('DRAFT', {
      ...allFalse,
      authorTemplates: true,
    })

    expect(context).toEqual({
      titleKey: 'dashboard.tasks.templateDraft.title',
      descriptionKey: 'dashboard.tasks.templateDraft.description',
    })
    expect(resolveWorkflowBannerActionKind('DRAFT', { ...allFalse, authorTemplates: true })).toBe(
      'draft',
    )
  })

  it('returns null when capability does not match lifecycle status', () => {
    expect(resolveTemplateWorkflowBannerContext('TESTING', allFalse)).toBeNull()
    expect(resolveTemplateWorkflowBannerContext('DRAFT', { ...allFalse, decideTests: true })).toBeNull()
    expect(resolveWorkflowBannerActionKind('PUBLISHED', allFalse)).toBeNull()
  })

  it('prioritizes testing over other capabilities at TESTING', () => {
    const caps: TemplateWorkflowBannerCapabilities = {
      authorTemplates: true,
      decideTests: true,
      decideApprovals: true,
      publishTemplates: true,
    }

    expect(resolveWorkflowBannerActionKind('TESTING', caps)).toBe('testing')
  })

  it.each([
    ['TESTING', 'decideTests'],
    ['APPROVAL', 'decideApprovals'],
    ['PENDING_RELEASE', 'publishTemplates'],
    ['DRAFT', 'authorTemplates'],
  ] as const satisfies ReadonlyArray<[TemplateLifecycleStatus, keyof TemplateWorkflowBannerCapabilities]>)(
    'maps %s workflow action visibility to banner kind',
    (status, capabilityKey) => {
      const caps = { ...allFalse, [capabilityKey]: true }
      expect(resolveWorkflowBannerActionKind(status, caps)).not.toBeNull()
    },
  )
})
