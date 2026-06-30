import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import TemplateDetailOverviewTab from '@/views/templates/detail/TemplateDetailOverviewTab.vue'
import en from '@/i18n/locales/en'
import type { TemplateDetail } from '@/types/template'

describe('TemplateDetailOverviewTab', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders template summary fields', async () => {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const template: TemplateDetail = {
      id: 'tpl-1',
      externalId: 'TPL-001',
      groupCode: 'RETAIL',
      name: 'Loan agreement',
      description: 'Retail loan pack',
      masterId: 'master-1',
      lifecycleStatus: 'DRAFT',
      releaseVersion: null,
      devVersionId: 'dev-1',
      devVersionNumber: 1,
      createdAt: '2026-06-23T09:00:00Z',
      updatedAt: '2026-06-23T10:00:00Z',
      variables: [],
      bindings: [],
      rules: [],
    }

    const wrapper = mount(TemplateDetailOverviewTab, {
      props: {
        template,
        formatDateTime: (value: string) => value,
      },
      global: {
        plugins: [i18n, ElementPlus],
        stubs: {
          RouterLink: {
            template: '<a><slot /></a>',
          },
        },
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('TPL-001')
    expect(wrapper.text()).toContain('master-1')
    expect(wrapper.text()).toContain('Retail loan pack')
  })
})
