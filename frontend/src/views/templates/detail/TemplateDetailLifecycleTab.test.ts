import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { describe, expect, it } from 'vitest'
import TemplateDetailLifecycleTab from '@/views/templates/detail/TemplateDetailLifecycleTab.vue'
import en from '@/i18n/locales/en'

import type { BindingValidationResult } from '@/types/template'

const baseProps = {
  showLifecycleSection: true,
  showGovernanceSection: false,
  lifecycleComment: '',
  showDraftActions: false,
  showTestingDecisionActions: false,
  showSubmitForApproval: false,
  showApprovalDecisionActions: false,
  showPublishActions: true,
  showTestGenerate: false,
  showStopAction: false,
  showRestoreAction: false,
  showDeprecateAction: false,
  publishGateItems: [{ key: 'releaseVersion', label: 'Release version is specified.', ready: true }],
  loadingPublishGate: false,
  publishBumpLevel: 'patch' as const,
  publishVersionConflict: false,
  publishGateReady: false,
  publishBumpOptions: [
    { level: 'patch' as const, label: 'Patch', version: '1.0.1' },
    { level: 'minor' as const, label: 'Minor', version: '1.1.0' },
  ],
  submitting: false,
  bindingGateResult: null as BindingValidationResult | null,
  publishGateLoadError: null as string | null,
  submitGateItems: [{ key: 'ANCHOR_INTEGRITY', label: 'Layout placeholder check', ready: true }],
  loadingSubmitGate: false,
  submitGateReady: true,
  submitGateLoadError: null as string | null,
}

function mountTab(props: Partial<typeof baseProps> = {}) {
  const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
  return mount(TemplateDetailLifecycleTab, {
    props: { ...baseProps, ...props },
    global: { plugins: [i18n, ElementPlus] },
  })
}

describe('TemplateDetailLifecycleTab', () => {
  it('shows publish gate load error with retry', async () => {
    const wrapper = mountTab({ publishGateLoadError: 'templates.error.loadPublishGate' })
    expect(wrapper.text()).toContain('Unable to load pre-release checks.')
    await wrapper.find('.el-button--primary').trigger('click')
    expect(wrapper.emitted('retryPublishGate')).toHaveLength(1)
  })

  it('renders binding validation summary and issues', () => {
    const wrapper = mountTab({
      bindingGateResult: {
        bindings: [
          {
            anchorId: 'ANC-MISSING',
            declaredContentType: 'TEXT',
            structuredContentJson: null,
            validationStatus: 'MISSING_ANCHOR',
          },
        ],
        summary: {
          blocking: true,
          totalBindings: 1,
          validCount: 0,
          missingAnchorCount: 1,
          duplicateBindingCount: 0,
          incompatibleContentTypeCount: 0,
        },
      },
    })

    expect(wrapper.text()).toContain('Binding validation')
    expect(wrapper.text()).toContain('0 of 1 bindings valid')
    expect(wrapper.text()).toContain('1 missing layout placeholder(s)')
    expect(wrapper.text()).toContain('ANC-MISSING')
  })

  it('wraps semver picker for narrow layouts', () => {
    const wrapper = mountTab()
    expect(wrapper.find('.publish-bump-picker').classes()).toContain('publish-bump-picker--wrap')
  })

  it('disables submit-for-approval when submit gate is not ready', () => {
    const wrapper = mountTab({
      showPublishActions: false,
      showSubmitForApproval: true,
      submitGateReady: false,
    })

    const submitButton = wrapper
      .findAll('button')
      .find((button) => button.text().includes('Submit for approval'))

    expect(submitButton?.attributes('disabled')).toBeDefined()
  })

  it('renders submit gate card with submit-specific copy', () => {
    const wrapper = mountTab({
      showPublishActions: false,
      showSubmitForApproval: true,
    })

    expect(wrapper.text()).toContain('Submission readiness checks')
    expect(wrapper.text()).toContain('Confirm these items before submitting the template for approval.')
    expect(wrapper.text()).not.toContain('Confirm these items before going live with a release version.')
  })
})
