import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { describe, expect, it, vi } from 'vitest'
import ApiPolicyDomainEditor from '@/components/api/ApiPolicyDomainEditor.vue'
import en from '@/i18n/locales/en'
import type { ApiPolicy } from '@/types/template'

vi.mock('@/stores/apiPolicy', () => ({
  useApiPolicyStore: () => ({
    entryFor: () => ({ lastErrorMessageKey: null }),
    savePolicyDomain: vi.fn(),
    saveInvocationRetentionDomain: vi.fn(),
    previewImpact: vi.fn(),
  }),
}))

const samplePolicy: ApiPolicy = {
  policyVersion: 1,
  templateId: 'tpl-1',
  defaultRouteReleaseVersion: '1.0.0',
  allowedAdGroups: ['APP-DOCGEN'],
  outputFormats: ['DOCX', 'PDF'],
  outputModes: ['SYNC_STREAM'],
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

function mountEditor(props: Partial<InstanceType<typeof ApiPolicyDomainEditor>['$props']> = {}) {
  setActivePinia(createPinia())
  const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
  return mount(ApiPolicyDomainEditor, {
    props: {
      templateId: 'tpl-1',
      apiPolicy: samplePolicy,
      canEdit: true,
      submitting: false,
      variant: 'tab-sections',
      ...props,
    },
    global: {
      plugins: [createPinia(), i18n, ElementPlus],
    },
  })
}

describe('ApiPolicyDomainEditor', () => {
  it('renders L1 retention controls in tab-sections variant', () => {
    const wrapper = mountEditor()
    expect(wrapper.text()).toContain('Retention')
    expect(wrapper.find('.retention-form').exists()).toBe(true)
  })

  it('renders active domain form in domain-console variant', () => {
    const wrapper = mountEditor({
      variant: 'domain-console',
      activeDomain: 'OUTPUT_POLICY',
    })
    expect(wrapper.text()).toContain('Output formats')
    expect(wrapper.text()).toContain('Output modes')
  })

  it('truncates long AD group summary text', () => {
    const longGroups = Array.from({ length: 8 }, (_, index) => `GROUP-${index}-VERY-LONG-NAME`)
    const wrapper = mountEditor({
      apiPolicy: {
        ...samplePolicy,
        allowedAdGroups: longGroups,
      },
    })

    const adGroupsCell = wrapper.find('.policy-ad-groups')
    expect(adGroupsCell.exists()).toBe(true)
    expect(adGroupsCell.classes()).toContain('policy-value--truncate')
  })
})
