import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import en from '@/i18n/locales/en'
import AuthoringSideBySideLayout from '@/components/templates/AuthoringSideBySideLayout.vue'

describe('AuthoringSideBySideLayout', () => {
  let matchesWide = true

  beforeEach(() => {
    matchesWide = true
    vi.spyOn(window, 'matchMedia').mockImplementation((query: string) => ({
      matches: matchesWide,
      media: query,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      addListener: vi.fn(),
      removeListener: vi.fn(),
      dispatchEvent: vi.fn(),
      onchange: null,
    }))
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  function mountLayout() {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    return mount(AuthoringSideBySideLayout, {
      slots: {
        editor: '<div data-testid="editor-slot">Editor</div>',
        preview: '<div data-testid="preview-slot">Preview</div>',
      },
      global: { plugins: [i18n, ElementPlus] },
    })
  }

  it('BDD-F7-B2-001 renders editor and preview panes on wide viewport', () => {
    matchesWide = true
    const wrapper = mountLayout()

    expect(wrapper.find('[data-testid="authoring-side-by-side-layout"]').classes()).not.toContain(
      'authoring-side-by-side--stacked',
    )
    expect(wrapper.find('[data-testid="editor-slot"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="preview-slot"]').exists()).toBe(true)
  })

  it('BDD-F7-B2-002 stacks layout and toggles preview on narrow viewport', async () => {
    matchesWide = false
    const wrapper = mountLayout()
    await flushPromises()

    expect(wrapper.classes()).toContain('authoring-side-by-side--stacked')
    expect(wrapper.find('[data-testid="authoring-preview-toggle"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="preview-slot"]').exists()).toBe(true)

    await wrapper.get('[data-testid="authoring-preview-toggle"]').trigger('click')
    expect(wrapper.find('[data-testid="preview-slot"]').exists()).toBe(false)

    await wrapper.get('[data-testid="authoring-preview-toggle"]').trigger('click')
    expect(wrapper.find('[data-testid="preview-slot"]').exists()).toBe(true)
  })
})
