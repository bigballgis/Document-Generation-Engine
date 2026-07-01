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

    await blockButtons[0]?.trigger('click')
    const emitted = wrapper.emitted('update:modelValue')
    expect(emitted?.length).toBeGreaterThan(0)
    const latest = emitted?.[emitted.length - 1]?.[0] as string
    expect(latest).toContain('"type":"sectionHeading"')
  })

  it('skips style catalog API when templateId is absent', async () => {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    mount(ControlledStructuredContentEditor, {
      props: {
        modelValue: '{"schemaVersion":"1.0","nodes":[]}',
      },
      global: { plugins: [i18n, ElementPlus] },
    })

    await flushPromises()

    expect(templatesApi.getMasterStyleCatalog).not.toHaveBeenCalled()
  })

  it('hides editing toolbar in readonly mode', async () => {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ControlledStructuredContentEditor, {
      props: {
        modelValue: '{"schemaVersion":"1.0","nodes":[{"type":"paragraph","children":[{"type":"textRun","value":"Preview"}]}]}',
        readonly: true,
      },
      global: { plugins: [i18n, ElementPlus] },
    })

    await flushPromises()

    expect(wrapper.find('[data-testid="insert-block-node"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('Paragraph')
  })
})
