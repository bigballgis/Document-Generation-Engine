import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import ControlledStructuredContentEditor from '@/components/authoring/ControlledStructuredContentEditor.vue'
import en from '@/i18n/locales/en'
import { useTemplatesStore } from '@/stores/templates'

vi.mock('@/stores/templates', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/stores/templates')>()
  return {
    ...actual,
    useTemplatesStore: vi.fn(),
  }
})

describe('ControlledStructuredContentEditor', () => {
  const fetchMasterStyleCatalog = vi.fn()
  const pasteClean = vi.fn()

  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(useTemplatesStore).mockReturnValue({
      fetchMasterStyleCatalog,
      pasteClean,
    } as unknown as ReturnType<typeof useTemplatesStore>)
  })

  afterEach(() => {
    document.body.innerHTML = ''
    fetchMasterStyleCatalog.mockReset()
    pasteClean.mockReset()
  })

  it('only confirmed nodes are insertable via toolbar', async () => {
    fetchMasterStyleCatalog.mockResolvedValue({
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

    expect(fetchMasterStyleCatalog).not.toHaveBeenCalled()
  })

  it('emits dirty-change when content diverges from baseline', async () => {
    fetchMasterStyleCatalog.mockResolvedValue({
      catalogVersion: '1.0',
      entries: [{ styleKey: 'BodyText', applicableNodeTypes: ['paragraph'], renderPurpose: 'BODY' }],
    })

    const baseline = '{"schemaVersion":"1.0","nodes":[]}'
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ControlledStructuredContentEditor, {
      props: {
        modelValue: baseline,
        templateId: 'tpl-1',
        baseline,
      },
      global: { plugins: [i18n, ElementPlus] },
    })

    await flushPromises()

    const blockButtons = wrapper.findAll('[data-testid="insert-block-node"]')
    await blockButtons[0]?.trigger('click')
    await flushPromises()

    const dirtyEvents = wrapper.emitted('dirty-change')
    expect(dirtyEvents?.[dirtyEvents.length - 1]?.[0]).toBe(true)
  })
})
