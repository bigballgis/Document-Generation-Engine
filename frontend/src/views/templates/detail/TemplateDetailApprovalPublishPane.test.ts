import { describe, expect, it, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import en from '@/i18n/locales/en'
import TemplateDetailApprovalPublishPane from '@/views/templates/detail/TemplateDetailApprovalPublishPane.vue'
import type { PublishGateDisplayItem } from '@/utils/templateLifecycleDecisionForm'

const replaceMock = vi.fn()
const routeQuery = { workspaceTab: 'approval', approvalTab: 'publishReadiness' }

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: routeQuery }),
  useRouter: () => ({ replace: replaceMock }),
}))

function mountPane(publishGateItems: PublishGateDisplayItem[]) {
  const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
  return mount(TemplateDetailApprovalPublishPane, {
    props: {
      bindingGateResult: null,
      bindingGateIssues: [],
      bindingGateIssueMessageKey: {
        missingAnchor: 'templates.bindingGate.issueMissingAnchor',
        duplicateBinding: 'templates.bindingGate.issueDuplicateBinding',
        incompatibleContentType: 'templates.bindingGate.issueIncompatibleContentType',
      },
      invalidBindings: [],
      resolveBindingStatusLabel: (status) => status ?? '',
      publishGateItems,
      loadingPublishGate: false,
      publishBumpLevel: 'patch',
      publishVersionConflict: false,
      publishBumpOptions: [{ level: 'patch', label: 'Patch', version: '1.0.1' }],
    },
    global: { plugins: [i18n, ElementPlus] },
  })
}

describe('TemplateDetailApprovalPublishPane Go fix (BDD-CE-U15-LSS)', () => {
  beforeEach(() => {
    replaceMock.mockReset()
  })

  it('LSS-004: shows Go fix for pending ANCHOR_INTEGRITY and navigates to design/bindings', async () => {
    const wrapper = mountPane([
      { key: 'ANCHOR_INTEGRITY', label: 'Layout placeholder check', ready: false, blocker: true },
    ])

    const goFix = wrapper.get('[data-testid="publish-gate-go-fix-ANCHOR_INTEGRITY"]')
    expect(goFix.text()).toBe('Go fix')
    await goFix.trigger('click')

    expect(replaceMock).toHaveBeenCalledWith({
      query: expect.objectContaining({
        workspaceTab: 'design',
        designTab: 'bindings',
      }),
    })
  })

  it('LSS-005: ready and unknown codes do not render Go fix', () => {
    const wrapper = mountPane([
      { key: 'VARIABLE_SCHEMA', label: 'Variable schema', ready: true, blocker: true },
      { key: 'X_UNKNOWN', label: 'Unknown check', ready: false, blocker: true },
    ])

    expect(wrapper.find('[data-testid="publish-gate-go-fix-VARIABLE_SCHEMA"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="publish-gate-go-fix-X_UNKNOWN"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('Pending')
  })

  it('FOS-W4-11: informational items still show ready/pending tags', () => {
    const wrapper = mountPane([
      {
        key: 'FIDELITY_REVIEW',
        label: 'Fidelity review',
        ready: true,
        informational: true,
      },
    ])
    const text = wrapper.text()
    expect(text).toContain('Informational')
    expect(text).toContain('Ready')
  })

  it('LSS-006: COVERAGE_THRESHOLDS Go fix lands on testing/coverage', async () => {
    const wrapper = mountPane([
      { key: 'COVERAGE_THRESHOLDS', label: 'Coverage', ready: false, blocker: true },
    ])
    await wrapper.get('[data-testid="publish-gate-go-fix-COVERAGE_THRESHOLDS"]').trigger('click')
    expect(replaceMock).toHaveBeenCalledWith({
      query: expect.objectContaining({
        workspaceTab: 'testing',
        testingTab: 'coverage',
      }),
    })
  })
})
