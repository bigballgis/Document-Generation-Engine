import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import TemplateWorkflowBanner from '@/components/templates/TemplateWorkflowBanner.vue'
import en from '@/i18n/locales/en'
import type { TemplateDetail } from '@/types/template'

const decideTests = vi.hoisted(() => ({ value: true }))

vi.mock('@/composables/useCapabilities', () => ({
  useCapabilities: () => ({
    authorTemplates: { value: false },
    decideTests: decideTests,
    decideApprovals: { value: false },
    publishTemplates: { value: false },
  }),
}))

describe('TemplateWorkflowBanner', () => {
  beforeEach(() => {
    decideTests.value = true
  })

  function mountBanner(template: Partial<TemplateDetail> = {}) {
    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })

    return mount(TemplateWorkflowBanner, {
      attachTo: document.body,
      props: {
        template: {
          id: 'tpl-1',
          externalId: 'TPL-1',
          groupCode: 'RETAIL',
          name: 'Loan agreement',
          description: null,
          masterId: 'master-1',
          lifecycleStatus: 'TESTING',
          releaseVersion: null,
          updatedAt: '2026-06-23T10:00:00Z',
          ...template,
        } as TemplateDetail,
      },
      global: {
        plugins: [i18n, ElementPlus],
      },
    })
  }

  it('emits openLifecycle when the CTA is clicked', async () => {
    const wrapper = mountBanner()
    await flushPromises()

    await wrapper.find('.workflow-banner__cta').trigger('click')

    expect(wrapper.emitted('openLifecycle')).toHaveLength(1)
  })

  it('hides the banner when the viewer has no workflow action', async () => {
    decideTests.value = false
    const wrapper = mountBanner({ lifecycleStatus: 'TESTING' })
    await flushPromises()

    expect(wrapper.find('.workflow-banner').exists()).toBe(false)
  })
})
