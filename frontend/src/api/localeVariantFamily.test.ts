import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as contentModulesApi from '@/api/contentModules'
import {
  fetchContentModuleLocaleVariantSiblings,
  fetchTemplateLocaleVariantSiblings,
} from '@/api/localeVariantFamily'
import * as templatesApi from '@/api/templates'

vi.mock('@/api/templates', () => ({
  listAllTemplates: vi.fn(),
}))

vi.mock('@/api/contentModules', () => ({
  listAllContentModules: vi.fn(),
}))

describe('localeVariantFamily API helpers (IBL-E1)', () => {
  beforeEach(() => {
    vi.mocked(templatesApi.listAllTemplates).mockReset()
    vi.mocked(contentModulesApi.listAllContentModules).mockReset()
  })

  it('returns empty siblings when family id is absent', async () => {
    await expect(
      fetchTemplateLocaleVariantSiblings({
        templateId: 'tpl-1',
        groupCode: 'RETAIL',
        localeVariantFamilyId: null,
      }),
    ).resolves.toEqual([])
    expect(templatesApi.listAllTemplates).not.toHaveBeenCalled()
  })

  it('filters authorized template siblings by family and excludes self', async () => {
    vi.mocked(templatesApi.listAllTemplates).mockResolvedValue({
      content: [
        {
          id: 'tpl-en',
          externalId: 'TPL-LETTER-EN',
          groupCode: 'RETAIL',
          name: 'Letter EN',
          locale: 'en-US',
          localeVariantFamilyId: 'family-1',
          lifecycleStatus: 'PUBLISHED',
          releaseVersion: '1.0.0',
          releaseVersionCount: 1,
          masterId: 'master-1',
          updatedBy: 'u1',
          updatedAt: '2026-07-19T10:00:00Z',
        },
        {
          id: 'tpl-zh',
          externalId: 'TPL-LETTER-ZH',
          groupCode: 'RETAIL',
          name: 'Letter ZH',
          locale: 'zh-CN',
          localeVariantFamilyId: 'family-1',
          lifecycleStatus: 'DRAFT',
          releaseVersion: null,
          releaseVersionCount: 0,
          masterId: 'master-1',
          updatedBy: 'u1',
          updatedAt: '2026-07-19T11:00:00Z',
        },
        {
          id: 'tpl-other',
          externalId: 'TPL-OTHER',
          groupCode: 'RETAIL',
          name: 'Other',
          locale: 'en-US',
          localeVariantFamilyId: 'family-2',
          lifecycleStatus: 'DRAFT',
          releaseVersion: null,
          releaseVersionCount: 0,
          masterId: 'master-1',
          updatedBy: 'u1',
          updatedAt: '2026-07-19T12:00:00Z',
        },
      ],
      totalElements: 3,
      truncated: false,
    })

    const siblings = await fetchTemplateLocaleVariantSiblings({
      templateId: 'tpl-en',
      groupCode: 'RETAIL',
      localeVariantFamilyId: 'family-1',
    })

    expect(templatesApi.listAllTemplates).toHaveBeenCalledWith({
      groupCode: 'RETAIL',
      signal: undefined,
    })
    expect(siblings).toEqual([
      {
        id: 'tpl-zh',
        code: 'TPL-LETTER-ZH',
        name: 'Letter ZH',
        locale: 'zh-CN',
        lifecycleLabel: 'DRAFT',
      },
    ])
  })

  it('filters content-module siblings by family', async () => {
    vi.mocked(contentModulesApi.listAllContentModules).mockResolvedValue({
      content: [
        {
          moduleId: 'mod-en',
          moduleCode: 'MOD-DISCLOSURE-EN',
          groupCode: 'HQ',
          name: 'Disclosure EN',
          locale: 'en-US',
          localeVariantFamilyId: 'cm-family',
          reviewState: 'APPROVED',
          lifecycleState: 'ACTIVE',
          createdAt: '2026-07-19T10:00:00Z',
          updatedAt: '2026-07-19T10:00:00Z',
        },
        {
          moduleId: 'mod-zh',
          moduleCode: 'MOD-DISCLOSURE-ZH',
          groupCode: 'HQ',
          name: 'Disclosure ZH',
          locale: 'zh-CN',
          localeVariantFamilyId: 'cm-family',
          reviewState: 'DRAFT',
          createdAt: '2026-07-19T11:00:00Z',
          updatedAt: '2026-07-19T11:00:00Z',
        },
      ],
      totalElements: 2,
      truncated: false,
    })

    const siblings = await fetchContentModuleLocaleVariantSiblings({
      moduleId: 'mod-en',
      groupCode: 'HQ',
      localeVariantFamilyId: 'cm-family',
    })

    expect(siblings.map((row) => row.id)).toEqual(['mod-zh'])
    expect(siblings[0]?.locale).toBe('zh-CN')
  })
})
