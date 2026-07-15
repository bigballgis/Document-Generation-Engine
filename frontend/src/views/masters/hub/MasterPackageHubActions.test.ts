import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { describe, expect, it } from 'vitest'
import MasterPackageHubActions from '@/views/masters/hub/MasterPackageHubActions.vue'
import en from '@/i18n/locales/en'

function mountActions(
  props: Partial<{
    status: 'DRAFT' | 'REJECTED' | 'PENDING_REVIEW' | 'APPROVED'
    downloading: boolean
    canReplaceFile: boolean
    canEditMetadata: boolean
    canSubmitForReview: boolean
    canDecideReview: boolean
  }> = {},
) {
  const i18n = createI18n({
    legacy: false,
    locale: 'en',
    messages: { en },
  })
  return mount(MasterPackageHubActions, {
    props: {
      status: 'DRAFT',
      downloading: false,
      canReplaceFile: false,
      canEditMetadata: false,
      canSubmitForReview: false,
      canDecideReview: false,
      ...props,
    },
    global: {
      plugins: [i18n, ElementPlus],
    },
  })
}

describe('MasterPackageHubActions', () => {
  it('shows Submit for review when canSubmitForReview is true (MRR-001)', () => {
    const wrapper = mountActions({
      status: 'DRAFT',
      canSubmitForReview: true,
    })
    expect(wrapper.text()).toContain('Submit for review')
  })

  it('hides Submit for review when canSubmitForReview is false (MRR-003)', () => {
    const wrapper = mountActions({
      status: 'DRAFT',
      canSubmitForReview: false,
    })
    expect(wrapper.text()).not.toContain('Submit for review')
  })

  it('shows Approve and Reject when canDecideReview is true (MRR-002)', () => {
    const wrapper = mountActions({
      status: 'PENDING_REVIEW',
      canDecideReview: true,
    })
    expect(wrapper.text()).toContain('Approve')
    expect(wrapper.text()).toContain('Reject')
  })

  it('hides Approve and Reject when canDecideReview is false (MRR-003)', () => {
    const wrapper = mountActions({
      status: 'PENDING_REVIEW',
      canDecideReview: false,
    })
    expect(wrapper.text()).not.toContain('Approve')
    expect(wrapper.text()).not.toContain('Reject')
  })

  it('emits openSubmitReview when Submit is clicked', async () => {
    const wrapper = mountActions({ canSubmitForReview: true })
    const buttons = wrapper.findAll('button')
    const submit = buttons.find((button) => button.text().includes('Submit for review'))
    expect(submit).toBeDefined()
    await submit!.trigger('click')
    expect(wrapper.emitted('openSubmitReview')).toHaveLength(1)
  })

  it('emits openReview with APPROVED and REJECTED', async () => {
    const wrapper = mountActions({ canDecideReview: true, status: 'PENDING_REVIEW' })
    const buttons = wrapper.findAll('button')
    const approve = buttons.find((button) => button.text() === 'Approve')
    const reject = buttons.find((button) => button.text() === 'Reject')
    await approve!.trigger('click')
    await reject!.trigger('click')
    expect(wrapper.emitted('openReview')?.[0]).toEqual(['APPROVED'])
    expect(wrapper.emitted('openReview')?.[1]).toEqual(['REJECTED'])
  })
})
