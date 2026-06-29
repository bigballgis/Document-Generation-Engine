import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { afterEach, describe, expect, it, vi } from 'vitest'
import ControlledStructuredContentEditor from '@/components/authoring/ControlledStructuredContentEditor.vue'
import en from '@/i18n/locales/en'
import * as templatesApi from '@/api/templates'

vi.mock('@/api/templates', () => ({
  getMasterStyleCatalog: vi.fn(),
  pasteClean: vi.fn(),
}))

describe('ControlledStructuredContentEditor', () => {
  afterEach(() => {
    document.body.innerHTML = ''
    vi.mocked(templatesApi.getMasterStyleCatalog).mockReset()
    vi.mocked(templatesApi.pasteClean).mockReset()
  })

  it('only confirmed nodes are insertable via toolbar', async () => {
    vi.mocked(templatesApi.getMasterStyleCatalog).mockResolvedValue({
      catalogVersion: '1.0',
      entries: [{ styleKey: 'BodyText', applicableNodeTypes: ['paragraph'], renderPurpose: 'BODY' }],
    })

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ControlledStructuredContentEditor, {
      props: {
        modelValue: '{"schemaVersion":"1.0","nodes":[]}',
        templateId: 'tpl-1',
      },
      global: { plugins: [i18n, ElementPlus] },
    })

    await flushPromises()

    const blockButtons = wrapper.findAll('[data-testid="insert-block-node"]')
    expect(blockButtons.length).toBeGreaterThan(0)

    const disabledButtons = wrapper.findAll('[data-testid="disabled-toolbar-item"]')
    expect(disabledButtons.length).toBe(3)
    for (const button of disabledButtons) {
      expect((button.element as HTMLButtonElement).disabled).toBe(true)
    }

    await blockButtons[0]?.trigger('click')
    const emitted = wrapper.emitted('update:modelValue')
    expect(emitted?.length).toBeGreaterThan(0)
    const latest = emitted?.[emitted.length - 1]?.[0] as string
    expect(latest).toContain('"type":"sectionHeading"')
  })
})
