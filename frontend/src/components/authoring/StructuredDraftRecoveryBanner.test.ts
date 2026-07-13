import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import en from '@/i18n/locales/en'
import StructuredDraftRecoveryBanner from '@/components/authoring/StructuredDraftRecoveryBanner.vue'

function mountBanner(
  props: {
    draftUpdatedAt: string
    serverUpdatedAt?: string | null
  } = { draftUpdatedAt: '2026-07-11T02:00:00.000Z' },
) {
  const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
  return mount(StructuredDraftRecoveryBanner, {
    props,
    global: { plugins: [i18n, ElementPlus] },
  })
}

describe('StructuredDraftRecoveryBanner', () => {
  it('renders restore and discard actions with test ids', () => {
    const wrapper = mountBanner()
    expect(wrapper.find('[data-testid="structured-draft-recovery-banner"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="structured-draft-recovery-banner-restore"]').exists()).toBe(
      true,
    )
    expect(wrapper.find('[data-testid="structured-draft-recovery-banner-discard"]').exists()).toBe(
      true,
    )
    expect(wrapper.text()).toContain('Restore draft')
    expect(wrapper.text()).toContain('Discard')
  })

  it('emits restore and discard', async () => {
    const wrapper = mountBanner()
    await wrapper.find('[data-testid="structured-draft-recovery-banner-restore"]').trigger('click')
    await wrapper.find('[data-testid="structured-draft-recovery-banner-discard"]').trigger('click')
    expect(wrapper.emitted('restore')).toHaveLength(1)
    expect(wrapper.emitted('discard')).toHaveLength(1)
  })
})
