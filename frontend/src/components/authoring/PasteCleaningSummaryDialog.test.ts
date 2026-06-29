import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { describe, expect, it } from 'vitest'
import PasteCleaningSummaryDialog from '@/components/authoring/PasteCleaningSummaryDialog.vue'
import en from '@/i18n/locales/en'
import type { PasteCleaningSummary } from '@/types/template'

const summary: PasteCleaningSummary = {
  items: [
    {
      category: 'TRANSFORMED',
      messageKey: 'paste.summary.transformed',
      detectionSummary: 'Transformed paragraph element into controlled structured node.',
    },
  ],
  transformedCount: 1,
  removedCount: 0,
  warningCount: 0,
  blockedCount: 0,
}

describe('PasteCleaningSummaryDialog', () => {
  it('emits cancel and undo to restore pre-paste state', async () => {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(PasteCleaningSummaryDialog, {
      props: {
        modelValue: true,
        summary,
        blocked: false,
      },
      attachTo: document.body,
      global: { plugins: [i18n, ElementPlus] },
    })

    await flushPromises()

    await wrapper.get('[data-testid="paste-summary-cancel"]').trigger('click')

    expect(wrapper.emitted('cancel')).toBeTruthy()
    expect(wrapper.emitted('undo')).toBeTruthy()
    expect(wrapper.emitted('update:modelValue')?.at(-1)).toEqual([false])
  })

  it('accepts cleaned paste when not blocked', async () => {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(PasteCleaningSummaryDialog, {
      props: {
        modelValue: true,
        summary,
        blocked: false,
      },
      attachTo: document.body,
      global: { plugins: [i18n, ElementPlus] },
    })

    await flushPromises()

    await wrapper.get('[data-testid="paste-summary-accept"]').trigger('click')

    expect(wrapper.emitted('accept')).toBeTruthy()
  })

  it('disables accept when paste is blocked', async () => {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(PasteCleaningSummaryDialog, {
      props: {
        modelValue: true,
        summary: { ...summary, blockedCount: 1 },
        blocked: true,
      },
      attachTo: document.body,
      global: { plugins: [i18n, ElementPlus] },
    })

    await flushPromises()

    const acceptButton = wrapper.get('[data-testid="paste-summary-accept"]').element as HTMLButtonElement
    expect(acceptButton.disabled).toBe(true)
  })
})
