import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { describe, expect, it, vi } from 'vitest'
import ApiPolicyDomainEditor from '@/components/api/ApiPolicyDomainEditor.vue'
import en from '@/i18n/locales/en'
import type { ApiPolicy } from '@/types/template'

vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus')
  return {
    ...actual,
    ElMessageBox: {
      ...actual.ElMessageBox,
      confirm: vi.fn().mockResolvedValue(undefined),
    },
  }
})

const previewImpact = vi.fn()
const savePolicyDomain = vi.fn()
const saveInvocationRetentionDomain = vi.fn()

vi.mock('@/stores/apiPolicy', () => ({
  useApiPolicyStore: () => ({
    entryFor: () => ({ lastErrorMessageKey: null }),
    savePolicyDomain,
    saveInvocationRetentionDomain,
    previewImpact,
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

  it('expands advanced settings when initialDomainAnchor is OUTPUT_POLICY', async () => {
    const wrapper = mountEditor({ initialDomainAnchor: 'OUTPUT_POLICY' })
    await wrapper.vm.$nextTick()
    const collapse = wrapper.findComponent({ name: 'ElCollapse' })
    expect(collapse.exists()).toBe(true)
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

  it('disables retention save until the form is dirty', () => {
    const wrapper = mountEditor()
    const saveButton = wrapper.find('[data-testid="retention-save-button"]')
    expect(saveButton.exists()).toBe(true)
    expect(saveButton.attributes('disabled')).toBeDefined()
  })

  it('shows inline success after retention save', async () => {
    saveInvocationRetentionDomain.mockResolvedValue({
      ...samplePolicy,
      invocationRecordRetentionDays: 180,
    })

    const wrapper = mountEditor()
    const exposed = wrapper.vm as unknown as {
      retentionForm: { invocationRecordRetentionDays: number }
    }
    exposed.retentionForm.invocationRecordRetentionDays = 180
    await flushPromises()

    const saveButton = wrapper.find('[data-testid="retention-save-button"]')
    expect(saveButton.attributes('disabled')).toBeUndefined()

    await saveButton.trigger('click')
    await flushPromises()

    expect(saveInvocationRetentionDomain).toHaveBeenCalledWith(
      'tpl-1',
      expect.objectContaining({ invocationRecordRetentionDays: 180 }),
    )
    expect(wrapper.find('[data-testid="retention-save-success"]').exists()).toBe(true)
  })

  it('runs impact preview before saving output settings', async () => {
    previewImpact.mockResolvedValue({
      changedAreas: ['OUTPUT_POLICY'],
      blocking: false,
      warnings: [],
      defaultRouteImpacted: false,
      currentPolicyVersion: 1,
      nextPolicyVersion: 2,
      summaryMessageKey: 'api.apimgmt.policyImpact.safe',
      contractDiffSummary: null,
      idempotencyImpactSummary: null,
    })
    savePolicyDomain.mockResolvedValue(samplePolicy)

    const wrapper = mountEditor({ initialDomainAnchor: 'OUTPUT_POLICY' })
    await flushPromises()

    const saveButtons = wrapper
      .findAll('button')
      .filter((button) => button.text().includes('Save output settings'))
    await saveButtons[0]?.trigger('click')
    await flushPromises()

    expect(previewImpact).toHaveBeenCalled()
    expect(savePolicyDomain).toHaveBeenCalledWith('tpl-1', 'OUTPUT_POLICY', expect.any(Object))
  })
})
