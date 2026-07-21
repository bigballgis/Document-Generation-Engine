import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { describe, expect, it } from 'vitest'
import MasterRevisionDetailWorkspace from '@/views/masters/detail/MasterRevisionDetailWorkspace.vue'
import en from '@/i18n/locales/en'

function mountWorkspace(changeSummary: string | null) {
  const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
  return mount(MasterRevisionDetailWorkspace, {
    props: {
      modelValue: 'design',
      workspaceTabs: [
        { name: 'design', labelKey: 'masters.revisionWorkspace.tabs.design' },
        { name: 'approval', labelKey: 'masters.revisionWorkspace.tabs.approval' },
      ],
      downloading: false,
      canSubmitForReview: false,
      canDecideReview: false,
      canEditAnchorDisplayLabel: false,
      changeSummary,
      filteredAnchors: [],
      reviewHistory: [],
      formatReviewAction: (action: string) => action,
      anchorColumnFilters: {},
      'onUpdate:anchorColumnFilters': () => undefined,
    },
    global: {
      plugins: [i18n, ElementPlus],
      stubs: {
        MasterAnchorPositionOverview: true,
        WorkspaceTabShell: {
          template: '<div><slot name="design" /><slot name="actions" /></div>',
        },
      },
    },
  })
}

describe('MasterRevisionDetailWorkspace', () => {
  it('BDD-SYS-NORM-W8-012 — shows honest empty when design change summary is empty', () => {
    const wrapper = mountWorkspace(null)

    expect(wrapper.find('[data-testid="master-revision-design-honest-empty"]').exists()).toBe(true)
    expect(wrapper.text()).toContain(en.masters.revision.emptySummaryTitle)
    expect(wrapper.text()).toContain(en.masters.revision.emptySummaryDescription)
  })

  it('BDD-SYS-NORM-W8-012 — shows change summary text when present', () => {
    const wrapper = mountWorkspace('Updated header logo')

    expect(wrapper.find('[data-testid="master-revision-design-honest-empty"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('Updated header logo')
  })
})
