import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import en from '@/i18n/locales/en'
import TemplateAuthoringBindingEditor from '@/components/templates/TemplateAuthoringBindingEditor.vue'

vi.mock('@/components/authoring/ControlledStructuredContentEditor.vue', () => ({
  default: {
    name: 'ControlledStructuredContentEditor',
    props: {
      modelValue: { type: String, default: '' },
      templateId: { type: String, default: undefined },
      devVersionId: { type: String, default: undefined },
      anchorId: { type: String, default: undefined },
      serverUpdatedAt: { type: String, default: null },
      variables: { type: Array, default: () => [] },
      contentModuleReferenceKeys: { type: Array, default: () => [] },
      baseline: { type: String, default: undefined },
      compactToolbar: { type: Boolean, default: false },
    },
    template:
      '<div data-testid="controlled-structured-content-editor-stub" :data-compact="compactToolbar ? \'yes\' : \'no\'" />',
  },
}))

vi.mock('@/components/templates/AuthoringPreviewPane.vue', () => ({
  default: {
    name: 'AuthoringPreviewPane',
    props: ['templateId', 'bindings', 'preview', 'stale', 'refreshing'],
    emits: ['refresh'],
    template: `
      <div data-testid="authoring-preview-pane">
        <button data-testid="authoring-preview-refresh" type="button" @click="$emit('refresh')">Refresh</button>
      </div>
    `,
  },
}))

vi.mock('@/components/authoring/ConditionExpressionInput.vue', () => ({
  default: {
    name: 'ConditionExpressionInput',
    props: ['modelValue', 'variableKeys', 'testId', 'placeholder'],
    template: '<input data-testid="visibility-expression-input" />',
  },
}))

describe('TemplateAuthoringBindingEditor IA (BEI #155)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  afterEach(() => {
    document.body.innerHTML = ''
  })

  function mountEditor() {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    return mount(TemplateAuthoringBindingEditor, {
      props: {
        templateId: 'tpl-1',
        editingRow: {
          anchorId: 'BODY_MAIN',
          displayLabel: 'Main body',
          declaredContentType: 'STRUCTURED',
          validationStatus: null,
          configured: false,
        },
        editingAnchorId: 'BODY_MAIN',
        draftDevVersionId: 'dev-1',
        contentTypes: ['STRUCTURED'],
        editingPasteResidueBlocked: false,
        variables: [],
        contentModuleReferenceKeys: [],
        bindings: [],
        lastPreview: null,
        previewStale: false,
        previewRefreshing: false,
        submitting: false,
        pasteResidueItemLabel: (key: string) => key,
        declaredContentType: 'STRUCTURED',
        structuredContentJson: '{"schemaVersion":"1.0","nodes":[]}',
        visibilityEnabled: false,
        visibilityExpression: '',
        'onUpdate:declaredContentType': vi.fn(),
        'onUpdate:structuredContentJson': vi.fn(),
        'onUpdate:visibilityEnabled': vi.fn(),
        'onUpdate:visibilityExpression': vi.fn(),
      },
      global: {
        plugins: [createPinia(), i18n, ElementPlus],
        stubs: {
          AppSearchSelect: {
            props: ['modelValue'],
            template: '<div data-testid="content-type-select" />',
          },
          ElOption: true,
          AuthoringSideBySideLayout: {
            template: `
              <div data-testid="authoring-side-by-side-layout">
                <div data-testid="authoring-editor-pane"><slot name="editor" /></div>
                <div data-testid="authoring-preview-pane-slot"><slot name="preview" /></div>
              </div>
            `,
          },
        },
      },
    })
  }

  it('BDD-BEI-001 renders sticky action rail with Back, anchor title, Save', () => {
    const wrapper = mountEditor()

    expect(wrapper.find('[data-testid="binding-editor-action-rail"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="binding-editor-back"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="binding-editor-save"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="binding-editor-anchor-title"]').text()).toContain('BODY_MAIN')
    expect(wrapper.find('[data-testid="binding-editor-anchor-title"]').text()).toContain('Main body')
  })

  it('BDD-BEI-002 Save is primary CTA on the rail', () => {
    const wrapper = mountEditor()
    const save = wrapper.get('[data-testid="binding-editor-save"]')
    expect(save.classes().join(' ')).toMatch(/el-button--primary|primary/)
  })

  it('BDD-BEI-003 Visibility advanced is collapsed by default', async () => {
    const wrapper = mountEditor()
    await flushPromises()

    const advanced = wrapper.get('[data-testid="binding-editor-visibility-advanced"]')
    expect(advanced.find('.el-collapse-item').classes()).not.toContain('is-active')
    const checkbox = wrapper.find('[data-testid="enable-visibility-checkbox"]')
    expect(checkbox.exists()).toBe(true)
    expect(checkbox.isVisible()).toBe(false)
  })

  it('BDD-BEI-003 expanding Visibility advanced reveals enable checkbox', async () => {
    const wrapper = mountEditor()
    await flushPromises()

    await wrapper.get('.el-collapse-item__header').trigger('click')
    await flushPromises()

    const checkbox = wrapper.find('[data-testid="enable-visibility-checkbox"]')
    expect(checkbox.exists()).toBe(true)
    expect(checkbox.isVisible()).toBe(true)
  })

  it('BDD-BEI-004 passes compactToolbar to structured editor', () => {
    const wrapper = mountEditor()
    const editor = wrapper.get('[data-testid="controlled-structured-content-editor-stub"]')
    expect(editor.attributes('data-compact')).toBe('yes')
  })

  it('BDD-BEI-009 uses English i18n for rail chrome', () => {
    const wrapper = mountEditor()
    expect(wrapper.find('[data-testid="binding-editor-back"]').text()).toBe('Back')
    expect(wrapper.find('[data-testid="binding-editor-save"]').text()).toBe('Save')
  })

  it('emits back and save from the action rail', async () => {
    const wrapper = mountEditor()

    await wrapper.get('[data-testid="binding-editor-back"]').trigger('click')
    await wrapper.get('[data-testid="binding-editor-save"]').trigger('click')

    expect(wrapper.emitted('back')).toHaveLength(1)
    expect(wrapper.emitted('save')).toHaveLength(1)
  })
})
