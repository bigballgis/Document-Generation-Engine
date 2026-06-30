import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { describe, expect, it } from 'vitest'
import TemplateDetailApiAccessTab from '@/views/templates/detail/TemplateDetailApiAccessTab.vue'
import en from '@/i18n/locales/en'
import type { ApiPolicy } from '@/types/template'

const baseProps = {
  templateId: 'tpl-1',
  showPolicyPanel: true,
  loadingPolicy: false,
  apiPolicy: null as ApiPolicy | null,
  policyLoadFailed: false,
  policyLoadErrorKey: null as string | null,
  paginatedCredentials: [],
  credentialStatusFilterOptions: [],
  pageSize: 10,
  totalCredentialRows: 0,
  submitting: false,
  formatDateTime: (value: string) => value,
  sortCredentialsByCreatedAt: () => 0,
  credentialColumnFilters: {},
  credentialsCurrentPage: 1,
  selectedContractEnvironment: 'dev' as const,
}

function mountTab(props: Partial<typeof baseProps> = {}) {
  const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
  return mount(TemplateDetailApiAccessTab, {
    props: { ...baseProps, ...props },
    global: {
      plugins: [i18n, ElementPlus],
      stubs: { TemplateCallerContractPanel: true },
    },
  })
}

describe('TemplateDetailApiAccessTab', () => {
  it('shows policy load error with retry', async () => {
    const wrapper = mountTab({
      policyLoadFailed: true,
      policyLoadErrorKey: 'templates.error.loadPolicy',
    })
    expect(wrapper.text()).toContain('Unable to load API policy.')
    await wrapper.find('.el-button--primary').trigger('click')
    expect(wrapper.emitted('retryPolicyLoad')).toHaveLength(1)
  })

  it('shows empty state when no policy is configured', () => {
    const wrapper = mountTab({ apiPolicy: null, policyLoadFailed: false, loadingPolicy: false })
    expect(wrapper.text()).toContain('No API policy configured')
  })

  it('truncates long AD group lists with tooltip content', () => {
    const longGroups = Array.from({ length: 8 }, (_, index) => `GROUP-${index}-VERY-LONG-NAME`)
    const wrapper = mountTab({
      apiPolicy: {
        policyVersion: 1,
        templateId: 'tpl-1',
        defaultRouteReleaseVersion: '1.0.0',
        allowedAdGroups: longGroups,
        outputFormats: ['PDF'],
        outputModes: ['SYNC'],
        batchEnabled: false,
        maxBatchSize: 1,
        docxEncryptionEnabled: false,
        pdfEncryptionEnabled: false,
        updatedAt: '2026-06-23T10:00:00Z',
      },
    })

    const adGroupsCell = wrapper.find('.policy-ad-groups')
    expect(adGroupsCell.exists()).toBe(true)
    expect(adGroupsCell.classes()).toContain('policy-value--truncate')
  })
})
