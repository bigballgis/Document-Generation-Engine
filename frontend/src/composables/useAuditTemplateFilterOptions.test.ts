import { mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as templatesApi from '@/api/templates'
import { useAuditTemplateFilterOptions } from '@/composables/useAuditTemplateFilterOptions'

vi.mock('@/api/templates', () => ({
  listTemplates: vi.fn(),
}))

describe('useAuditTemplateFilterOptions', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(templatesApi.listTemplates).mockReset()
  })

  it('searches templates and maps options for the filter select', async () => {
    vi.mocked(templatesApi.listTemplates).mockResolvedValue({
      content: [
        {
          id: 'tpl-1',
          externalId: 'TPL-001',
          groupCode: 'RETAIL',
          name: 'Loan agreement',
          lifecycleStatus: 'PUBLISHED',
          releaseVersion: '1.0',
          releaseVersionCount: 1,
          masterId: 'master-1',
          updatedBy: 'admin',
          updatedAt: '2026-06-23T10:00:00Z',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })

    let searchTemplates: ReturnType<typeof useAuditTemplateFilterOptions>['searchTemplates']
    let templateOptions: ReturnType<typeof useAuditTemplateFilterOptions>['templateOptions']

    const Harness = defineComponent({
      setup() {
        const composable = useAuditTemplateFilterOptions()
        searchTemplates = composable.searchTemplates
        templateOptions = composable.templateOptions
        return () => null
      },
    })

    mount(Harness, {
      global: {
        plugins: [createPinia()],
      },
    })

    await searchTemplates!('loan')

    expect(templatesApi.listTemplates).toHaveBeenCalledWith(0, 20, { search: 'loan' })
    expect(templateOptions!.value).toEqual([
      { value: 'tpl-1', label: 'Loan agreement (TPL-001)' },
    ])
  })
})
