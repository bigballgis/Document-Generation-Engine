import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { describe, expect, it, vi } from 'vitest'
import TemplateDetailApiAccessTab from '@/views/templates/detail/TemplateDetailApiAccessTab.vue'
import en from '@/i18n/locales/en'
import type { ApiPolicy } from '@/types/template'

vi.mock('@/composables/useCapabilities', () => ({
  useCapabilities: () => ({
    manageApiPolicy: { value: true },
  }),
}))

vi.mock('@/stores/templates', () => ({
  useTemplatesStore: () => ({
    lastErrorMessageKey: null,
    saveApiPolicyDomain: vi.fn(),
    saveInvocationRetentionDomain: vi.fn(),
    previewApiPolicyImpact: vi.fn(),
  }),
}))

const samplePolicy: ApiPolicy = {
  policyVersion: 1,
  templateId: 'tpl-1',
  defaultRouteReleaseVersion: '1.0.0',
  allowedAdGroups: [],
  outputFormats: ['DOCX', 'PDF'],
  outputModes: ['SYNC_STREAM', 'SYNC_DOWNLOAD_URL', 'ASYNC_TASK'],
  batchEnabled: true,
  maxBatchSize: 100,
  batchSyncMaxItems: 100,
  batchAsyncMaxItems: 10_000,
  docxEncryptionEnabled: false,
  pdfEncryptionEnabled: false,
  saveGeneratedDocuments: true,
  invocationRecordRetentionDays: 90,
  documentRetentionDays: 30,
  updatedAt: '2026-06-23T10:00:00Z',
}

const baseProps = {
  templateId: 'tpl-1',
  showPolicyPanel: true,
  loadingPolicy: false,
  apiPolicy: samplePolicy,
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
      stubs: {
        TemplateCallerContractPanel: true,
        TemplateRecentInvocationsPanel: true,
        CredentialsPanel: true,
      },
    },
  })
}

describe('TemplateDetailApiAccessTab', () => {
  it('shows policy load error with retry', async () => {
    const wrapper = mountTab({
      policyLoadFailed: true,
      policyLoadErrorKey: 'templates.error.loadPolicy',
    })
    expect(wrapper.text()).toContain('Unable to load access settings.')
    await wrapper.find('.el-button--primary').trigger('click')
    expect(wrapper.emitted('retryPolicyLoad')).toHaveLength(1)
  })

  it('does not show the legacy not-configured empty state', () => {
    const wrapper = mountTab({ apiPolicy: samplePolicy })
    expect(wrapper.text()).not.toContain('API access not configured')
  })

  it('renders retention controls for L1 configuration', () => {
    const wrapper = mountTab({ apiPolicy: samplePolicy })
    expect(wrapper.text()).toContain('Retention')
    expect(wrapper.text()).toContain('Save generated documents on the server')
    expect(wrapper.text()).toContain('Invocation record retention')
    expect(wrapper.text()).toContain('Document retention')
    expect(wrapper.find('.retention-form').exists()).toBe(true)
  })

  it('truncates long AD group lists with tooltip content', () => {
    const longGroups = Array.from({ length: 8 }, (_, index) => `GROUP-${index}-VERY-LONG-NAME`)
    const wrapper = mountTab({
      apiPolicy: {
        ...samplePolicy,
        allowedAdGroups: longGroups,
      },
    })

    const adGroupsCell = wrapper.find('.policy-ad-groups')
    expect(adGroupsCell.exists()).toBe(true)
    expect(adGroupsCell.classes()).toContain('policy-value--truncate')
  })

  it('shows L1 sections even when AD groups and default route are unset', () => {
    const wrapper = mountTab({
      apiPolicy: {
        ...samplePolicy,
        allowedAdGroups: [],
        defaultRouteReleaseVersion: '',
      },
    })

    expect(wrapper.text()).toContain('Authorized AD groups')
    expect(wrapper.text()).toContain('Default route')
    expect(wrapper.text()).not.toContain('API access not configured')
  })
})
