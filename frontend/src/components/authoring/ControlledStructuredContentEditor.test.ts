import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import ControlledStructuredContentEditor from '@/components/authoring/ControlledStructuredContentEditor.vue'
import en from '@/i18n/locales/en'
import { useSessionStore } from '@/stores/session'
import { useTemplatesStore } from '@/stores/templates'
import {
  buildStructuredDraftStorageKey,
  writeStructuredDraft,
} from '@/utils/structuredContentDraftStorage'

vi.mock('@/stores/templates', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/stores/templates')>()
  return {
    ...actual,
    useTemplatesStore: vi.fn(),
  }
})

function patchAuthorSession() {
  useSessionStore().$patch({
    session: {
      username: 'author-1',
      displayName: 'Author',
      email: 'a@example.com',
      authSource: 'LOCAL',
      roles: ['TEMPLATE_AUTHOR'],
      authorizedGroupCodes: [],
      defaultRoute: 'dashboard',
      visibleRoutes: ['dashboard'],
      expiresAt: '2099-01-01T00:00:00.000Z',
    },
  })
}

describe('ControlledStructuredContentEditor', () => {
  const fetchMasterStyleCatalog = vi.fn()
  const pasteClean = vi.fn()

  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.mocked(useTemplatesStore).mockReturnValue({
      fetchMasterStyleCatalog,
      pasteClean,
    } as unknown as ReturnType<typeof useTemplatesStore>)
  })

  afterEach(() => {
    document.body.innerHTML = ''
    localStorage.clear()
    fetchMasterStyleCatalog.mockReset()
    pasteClean.mockReset()
    vi.useRealTimers()
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
    wrapper.unmount()
  })

  it('skips style catalog API when templateId is absent', async () => {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ControlledStructuredContentEditor, {
      props: {
        modelValue: '{"schemaVersion":"1.0","nodes":[]}',
      },
      global: { plugins: [i18n, ElementPlus] },
    })

    await flushPromises()

    expect(fetchMasterStyleCatalog).not.toHaveBeenCalled()
    wrapper.unmount()
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
    wrapper.unmount()
  })

  it('BDD-LRP-C2-001 shows recovery banner and restore applies draft JSON', async () => {
    fetchMasterStyleCatalog.mockResolvedValue({
      catalogVersion: '1.0',
      entries: [{ styleKey: 'BodyText', applicableNodeTypes: ['paragraph'], renderPurpose: 'BODY' }],
    })
    patchAuthorSession()

    const server = '{"schemaVersion":"1.0","nodes":[]}'
    const draftJson =
      '{"schemaVersion":"1.0","nodes":[{"type":"paragraph","children":[{"type":"textRun","value":"recovered"}]}]}'
    writeStructuredDraft(localStorage, buildStructuredDraftStorageKey('author-1', 'tpl-1', 'dev-1'), {
      schemaVersion: 1,
      structureJson: draftJson,
      draftUpdatedAt: '2026-07-11T02:00:00.000Z',
      serverUpdatedAt: '2026-07-11T01:00:00.000Z',
      anchorId: 'anchor-1',
    })

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ControlledStructuredContentEditor, {
      props: {
        modelValue: server,
        templateId: 'tpl-1',
        devVersionId: 'dev-1',
        anchorId: 'anchor-1',
        baseline: server,
      },
      global: { plugins: [i18n, ElementPlus] },
    })

    await flushPromises()

    expect(wrapper.find('[data-testid="structured-draft-recovery-banner"]').exists()).toBe(true)
    await wrapper.find('[data-testid="structured-draft-recovery-banner-restore"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="structured-draft-recovery-banner"]').exists()).toBe(false)
    const emitted = wrapper.emitted('update:modelValue')
    const latest = emitted?.[emitted.length - 1]?.[0] as string
    expect(latest).toBe(draftJson)
    expect(wrapper.emitted('dirty-change')?.at(-1)?.[0]).toBe(true)
    // Unmount cancels pending draft debounce timers (must not leak into later tests).
    wrapper.unmount()
  })

  it('BDD-LRP-C2-003 discard clears draft and keeps server content', async () => {
    fetchMasterStyleCatalog.mockResolvedValue({
      catalogVersion: '1.0',
      entries: [{ styleKey: 'BodyText', applicableNodeTypes: ['paragraph'], renderPurpose: 'BODY' }],
    })
    patchAuthorSession()

    const server = '{"schemaVersion":"1.0","nodes":[]}'
    const key = buildStructuredDraftStorageKey('author-1', 'tpl-1', 'dev-1')
    writeStructuredDraft(localStorage, key, {
      schemaVersion: 1,
      structureJson: '{"schemaVersion":"1.0","nodes":[{"type":"list"}]}',
      draftUpdatedAt: '2026-07-11T02:00:00.000Z',
      anchorId: 'anchor-1',
    })

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ControlledStructuredContentEditor, {
      props: {
        modelValue: server,
        templateId: 'tpl-1',
        devVersionId: 'dev-1',
        anchorId: 'anchor-1',
        baseline: server,
      },
      global: { plugins: [i18n, ElementPlus] },
    })

    await flushPromises()
    await wrapper.find('[data-testid="structured-draft-recovery-banner-discard"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-testid="structured-draft-recovery-banner"]').exists()).toBe(false)
    expect(localStorage.getItem(key)).toBeNull()
    wrapper.unmount()
  })

  it('BDD-LRP-C2-009 does not show recovery banner when readonly', async () => {
    patchAuthorSession()

    writeStructuredDraft(localStorage, buildStructuredDraftStorageKey('author-1', 'tpl-1', 'dev-1'), {
      schemaVersion: 1,
      structureJson: '{"schemaVersion":"1.0","nodes":[{"type":"paragraph"}]}',
      draftUpdatedAt: '2026-07-11T02:00:00.000Z',
    })

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ControlledStructuredContentEditor, {
      props: {
        modelValue: '{"schemaVersion":"1.0","nodes":[]}',
        templateId: 'tpl-1',
        devVersionId: 'dev-1',
        readonly: true,
      },
      global: { plugins: [i18n, ElementPlus] },
    })

    await flushPromises()
    expect(wrapper.find('[data-testid="structured-draft-recovery-banner"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('BDD-LRP-C2-005 markPristine clears local draft', async () => {
    fetchMasterStyleCatalog.mockResolvedValue({
      catalogVersion: '1.0',
      entries: [{ styleKey: 'BodyText', applicableNodeTypes: ['paragraph'], renderPurpose: 'BODY' }],
    })
    patchAuthorSession()

    const key = buildStructuredDraftStorageKey('author-1', 'tpl-1', 'dev-1')
    writeStructuredDraft(localStorage, key, {
      schemaVersion: 1,
      structureJson: '{"schemaVersion":"1.0","nodes":[{"type":"paragraph"}]}',
      draftUpdatedAt: '2026-07-11T02:00:00.000Z',
    })

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ControlledStructuredContentEditor, {
      props: {
        modelValue: '{"schemaVersion":"1.0","nodes":[]}',
        templateId: 'tpl-1',
        devVersionId: 'dev-1',
      },
      global: { plugins: [i18n, ElementPlus] },
    })

    await flushPromises()
    ;(wrapper.vm as { markPristine: () => void }).markPristine()
    expect(localStorage.getItem(key)).toBeNull()
    wrapper.unmount()
  })

  it('BDD-LRP-C2-002 markPristine clear survives modelValue echo remount race', async () => {
    vi.useFakeTimers()
    fetchMasterStyleCatalog.mockResolvedValue({
      catalogVersion: '1.0',
      entries: [{ styleKey: 'BodyText', applicableNodeTypes: ['paragraph'], renderPurpose: 'BODY' }],
    })
    patchAuthorSession()

    const key = buildStructuredDraftStorageKey('author-1', 'tpl-1', 'dev-1')
    const dirty =
      '{"schemaVersion":"1.0","nodes":[{"type":"paragraph","children":[{"type":"textRun","value":"draft"}]}]}'
    const echoCanonicalized =
      '{"schemaVersion":"1.0","nodes":[{"type":"paragraph","children":[{"type":"textRun","value":"draft"}],"styleKey":"BodyText"}]}'

    writeStructuredDraft(localStorage, key, {
      schemaVersion: 1,
      structureJson: dirty,
      draftUpdatedAt: '2026-07-11T02:00:00.000Z',
    })

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ControlledStructuredContentEditor, {
      props: {
        modelValue: '{"schemaVersion":"1.0","nodes":[]}',
        templateId: 'tpl-1',
        devVersionId: 'dev-1',
        baseline: '{"schemaVersion":"1.0","nodes":[]}',
      },
      global: { plugins: [i18n, ElementPlus] },
    })

    await flushPromises()
    await wrapper.find('[data-testid="structured-draft-recovery-banner-restore"]').trigger('click')
    await flushPromises()

    ;(wrapper.vm as { markPristine: () => void }).markPristine()
    expect(localStorage.getItem(key)).toBeNull()

    // Simulate parent refresh echoing a slightly different serialization after save.
    await wrapper.setProps({ modelValue: echoCanonicalized, baseline: echoCanonicalized })
    await flushPromises()
    await vi.advanceTimersByTimeAsync(500)

    expect(localStorage.getItem(key)).toBeNull()
    expect(wrapper.find('[data-testid="structured-draft-recovery-banner"]').exists()).toBe(false)

    wrapper.unmount()
    vi.useRealTimers()

    // Remount with server-matching structure: no banner, no draft rewrite.
    const remount = mount(ControlledStructuredContentEditor, {
      props: {
        modelValue: echoCanonicalized,
        templateId: 'tpl-1',
        devVersionId: 'dev-1',
        baseline: echoCanonicalized,
      },
      global: {
        plugins: [createI18n({ legacy: false, locale: 'en', messages: { en } }), ElementPlus],
      },
    })
    await flushPromises()
    expect(remount.find('[data-testid="structured-draft-recovery-banner"]').exists()).toBe(false)
    expect(localStorage.getItem(key)).toBeNull()
    remount.unmount()
  })

  it('BDD-LRP-C3-005 empty history disables undo/redo toolbar buttons', async () => {
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

    const undoBtn = wrapper.get('[data-testid="structured-editor-undo"]')
    const redoBtn = wrapper.get('[data-testid="structured-editor-redo"]')
    expect(undoBtn.attributes('disabled')).toBeDefined()
    expect(redoBtn.attributes('disabled')).toBeDefined()
    expect(undoBtn.attributes('aria-label')).toBe(en.templates.structuredEditor.undo)
    expect(redoBtn.attributes('title')).toBe(en.templates.structuredEditor.redoTooltip)
    wrapper.unmount()
  })

  it('BDD-LRP-C3-001/003 toolbar undo×2 then redo restores structure', async () => {
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
    await blockButtons[0]?.trigger('click') // sectionHeading
    await blockButtons[1]?.trigger('click') // paragraph
    await blockButtons[2]?.trigger('click') // list
    await flushPromises()

    const afterThree = wrapper.emitted('update:modelValue')?.at(-1)?.[0] as string
    expect(afterThree).toContain('"type":"list"')

    await wrapper.get('[data-testid="structured-editor-undo"]').trigger('click')
    await wrapper.get('[data-testid="structured-editor-undo"]').trigger('click')
    await flushPromises()

    const afterUndo2 = wrapper.emitted('update:modelValue')?.at(-1)?.[0] as string
    expect(afterUndo2).toContain('"type":"sectionHeading"')
    expect(afterUndo2).not.toContain('"type":"list"')
    expect(wrapper.get('[data-testid="structured-editor-redo"]').attributes('disabled')).toBeUndefined()

    await wrapper.get('[data-testid="structured-editor-redo"]').trigger('click')
    await flushPromises()
    const afterRedo = wrapper.emitted('update:modelValue')?.at(-1)?.[0] as string
    expect(afterRedo).toContain('"type":"paragraph"')
    wrapper.unmount()
  })

  it('BDD-LRP-C3-006 Ctrl/Cmd+Z and Ctrl+Y / Cmd+Shift+Z keyboard shortcuts', async () => {
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
    const root = wrapper.get('[data-testid="controlled-structured-content-editor"]')
    const blockButtons = wrapper.findAll('[data-testid="insert-block-node"]')
    await blockButtons[0]?.trigger('click')
    await blockButtons[1]?.trigger('click')
    await flushPromises()

    await root.trigger('keydown', { key: 'z', ctrlKey: true })
    await flushPromises()
    let latest = wrapper.emitted('update:modelValue')?.at(-1)?.[0] as string
    expect(latest).toContain('"type":"sectionHeading"')
    expect(latest).not.toContain('"type":"paragraph"')

    await root.trigger('keydown', { key: 'y', ctrlKey: true })
    await flushPromises()
    latest = wrapper.emitted('update:modelValue')?.at(-1)?.[0] as string
    expect(latest).toContain('"type":"paragraph"')

    await root.trigger('keydown', { key: 'z', ctrlKey: true })
    await flushPromises()
    await root.trigger('keydown', { key: 'z', metaKey: true, shiftKey: true })
    await flushPromises()
    latest = wrapper.emitted('update:modelValue')?.at(-1)?.[0] as string
    expect(latest).toContain('"type":"paragraph"')
    wrapper.unmount()
  })

  it('BDD-LRP-C3-012 undo to baseline clears dirty; redo sets dirty', async () => {
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
    await wrapper.findAll('[data-testid="insert-block-node"]')[0]?.trigger('click')
    await flushPromises()
    expect(wrapper.emitted('dirty-change')?.at(-1)?.[0]).toBe(true)

    await wrapper.get('[data-testid="structured-editor-undo"]').trigger('click')
    await flushPromises()
    expect(wrapper.emitted('dirty-change')?.at(-1)?.[0]).toBe(false)

    await wrapper.get('[data-testid="structured-editor-redo"]').trigger('click')
    await flushPromises()
    expect(wrapper.emitted('dirty-change')?.at(-1)?.[0]).toBe(true)
    wrapper.unmount()
  })

  it('BDD-LRP-C3-009 markPristine clears history stacks', async () => {
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
    await wrapper.findAll('[data-testid="insert-block-node"]')[0]?.trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-testid="structured-editor-undo"]').attributes('disabled')).toBeUndefined()

    ;(wrapper.vm as { markPristine: () => void }).markPristine()
    await flushPromises()
    expect(wrapper.get('[data-testid="structured-editor-undo"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('[data-testid="structured-editor-redo"]').attributes('disabled')).toBeDefined()
    wrapper.unmount()
  })

  it('BDD-LRP-C3-008 draft blob never contains undo/history fields', async () => {
    vi.useFakeTimers()
    fetchMasterStyleCatalog.mockResolvedValue({
      catalogVersion: '1.0',
      entries: [{ styleKey: 'BodyText', applicableNodeTypes: ['paragraph'], renderPurpose: 'BODY' }],
    })
    patchAuthorSession()

    const key = buildStructuredDraftStorageKey('author-1', 'tpl-1', 'dev-1')
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ControlledStructuredContentEditor, {
      props: {
        modelValue: '{"schemaVersion":"1.0","nodes":[]}',
        templateId: 'tpl-1',
        devVersionId: 'dev-1',
        baseline: '{"schemaVersion":"1.0","nodes":[]}',
      },
      global: { plugins: [i18n, ElementPlus] },
    })

    await flushPromises()
    await wrapper.findAll('[data-testid="insert-block-node"]')[0]?.trigger('click')
    await wrapper.findAll('[data-testid="insert-block-node"]')[1]?.trigger('click')
    await vi.advanceTimersByTimeAsync(500)
    await flushPromises()

    const raw = localStorage.getItem(key)
    expect(raw).toBeTruthy()
    const payload = JSON.parse(raw!) as Record<string, unknown>
    expect(payload).toHaveProperty('schemaVersion')
    expect(payload).toHaveProperty('structureJson')
    expect(payload).toHaveProperty('draftUpdatedAt')
    expect(payload).not.toHaveProperty('undoStack')
    expect(payload).not.toHaveProperty('redoStack')
    expect(payload).not.toHaveProperty('history')
    expect(JSON.stringify(payload).toLowerCase()).not.toMatch(/undostack|redostack|"history"/)
    wrapper.unmount()
    vi.useRealTimers()
  })

  it('BDD-LRP-C3-010 restore draft clears history', async () => {
    fetchMasterStyleCatalog.mockResolvedValue({
      catalogVersion: '1.0',
      entries: [{ styleKey: 'BodyText', applicableNodeTypes: ['paragraph'], renderPurpose: 'BODY' }],
    })
    patchAuthorSession()

    const server = '{"schemaVersion":"1.0","nodes":[]}'
    const draftJson =
      '{"schemaVersion":"1.0","nodes":[{"type":"paragraph","children":[{"type":"textRun","value":"recovered"}]}]}'
    writeStructuredDraft(localStorage, buildStructuredDraftStorageKey('author-1', 'tpl-1', 'dev-1'), {
      schemaVersion: 1,
      structureJson: draftJson,
      draftUpdatedAt: '2026-07-11T02:00:00.000Z',
      anchorId: 'anchor-1',
    })

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ControlledStructuredContentEditor, {
      props: {
        modelValue: server,
        templateId: 'tpl-1',
        devVersionId: 'dev-1',
        anchorId: 'anchor-1',
        baseline: server,
      },
      global: { plugins: [i18n, ElementPlus] },
    })

    await flushPromises()
    // Build some history before restore (banner still visible — edit under banner).
    await wrapper.findAll('[data-testid="insert-block-node"]')[0]?.trigger('click')
    await flushPromises()
    expect(wrapper.get('[data-testid="structured-editor-undo"]').attributes('disabled')).toBeUndefined()

    await wrapper.find('[data-testid="structured-draft-recovery-banner-restore"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="structured-editor-undo"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('[data-testid="structured-editor-redo"]').attributes('disabled')).toBeDefined()
    wrapper.unmount()
  })

  it('BDD-LRP-C3-011 discard draft clears history', async () => {
    fetchMasterStyleCatalog.mockResolvedValue({
      catalogVersion: '1.0',
      entries: [{ styleKey: 'BodyText', applicableNodeTypes: ['paragraph'], renderPurpose: 'BODY' }],
    })
    patchAuthorSession()

    const server = '{"schemaVersion":"1.0","nodes":[]}'
    writeStructuredDraft(localStorage, buildStructuredDraftStorageKey('author-1', 'tpl-1', 'dev-1'), {
      schemaVersion: 1,
      structureJson: '{"schemaVersion":"1.0","nodes":[{"type":"list"}]}',
      draftUpdatedAt: '2026-07-11T02:00:00.000Z',
      anchorId: 'anchor-1',
    })

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ControlledStructuredContentEditor, {
      props: {
        modelValue: server,
        templateId: 'tpl-1',
        devVersionId: 'dev-1',
        anchorId: 'anchor-1',
        baseline: server,
      },
      global: { plugins: [i18n, ElementPlus] },
    })

    await flushPromises()
    await wrapper.findAll('[data-testid="insert-block-node"]')[0]?.trigger('click')
    await flushPromises()

    await wrapper.find('[data-testid="structured-draft-recovery-banner-discard"]').trigger('click')
    await flushPromises()

    expect(wrapper.get('[data-testid="structured-editor-undo"]').attributes('disabled')).toBeDefined()
    expect(wrapper.get('[data-testid="structured-editor-redo"]').attributes('disabled')).toBeDefined()
    wrapper.unmount()
  })

  it('BDD-LRP-C3-013 readonly hides undo/redo toolbar', async () => {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ControlledStructuredContentEditor, {
      props: {
        modelValue: '{"schemaVersion":"1.0","nodes":[]}',
        templateId: 'tpl-1',
        readonly: true,
      },
      global: { plugins: [i18n, ElementPlus] },
    })

    await flushPromises()
    expect(wrapper.find('[data-testid="structured-editor-undo"]').exists()).toBe(false)
    expect(wrapper.find('[data-testid="structured-editor-redo"]').exists()).toBe(false)

    const root = wrapper.get('[data-testid="controlled-structured-content-editor"]')
    await root.trigger('keydown', { key: 'z', ctrlKey: true })
    expect(wrapper.emitted('update:modelValue')).toBeFalsy()
    wrapper.unmount()
  })

  it('BDD-LRP-C3-014 coalesces consecutive paragraph text edits into one undo step', async () => {
    fetchMasterStyleCatalog.mockResolvedValue({
      catalogVersion: '1.0',
      entries: [{ styleKey: 'BodyText', applicableNodeTypes: ['paragraph'], renderPurpose: 'BODY' }],
    })

    const baseline =
      '{"schemaVersion":"1.0","nodes":[{"type":"paragraph","children":[{"type":"textRun","value":""}]}]}'
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
    const input = wrapper.get('[data-testid="paragraph-input"]')
    await input.setValue('a')
    await input.setValue('ab')
    await input.setValue('abc')
    await flushPromises()

    expect(wrapper.emitted('update:modelValue')?.at(-1)?.[0]).toContain('"value":"abc"')

    await wrapper.get('[data-testid="structured-editor-undo"]').trigger('click')
    await flushPromises()
    const afterUndo = wrapper.emitted('update:modelValue')?.at(-1)?.[0] as string
    expect(afterUndo).toContain('"value":""')
    expect(afterUndo).not.toContain('"value":"ab"')
    // One undo should exhaust the coalesced field edit.
    expect(wrapper.get('[data-testid="structured-editor-undo"]').attributes('disabled')).toBeDefined()
    wrapper.unmount()
  })

  it('emits paste-accepted evidence with blockedCount=0 on Accept (never source HTML)', async () => {
    fetchMasterStyleCatalog.mockResolvedValue({
      catalogVersion: '1.0',
      entries: [{ styleKey: 'BodyText', applicableNodeTypes: ['paragraph'], renderPurpose: 'BODY' }],
    })
    pasteClean.mockResolvedValue({
      blocked: false,
      cleanedStructuredContentJson:
        '{"schemaVersion":"1.0","nodes":[{"type":"paragraph","children":[{"type":"textRun","value":"Clean"}]}]}',
      summary: {
        items: [
          {
            category: 'TRANSFORMED',
            messageKey: 'paste.summary.transformed',
            detectionSummary: 'Transformed paragraph.',
          },
        ],
        transformedCount: 1,
        removedCount: 0,
        warningCount: 0,
        blockedCount: 0,
      },
      prePasteSnapshotJson: '{"schemaVersion":"1.0","nodes":[]}',
    })

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ControlledStructuredContentEditor, {
      props: {
        modelValue: '{"schemaVersion":"1.0","nodes":[]}',
        templateId: 'tpl-1',
      },
      attachTo: document.body,
      global: { plugins: [i18n, ElementPlus] },
    })

    await flushPromises()

    const fileInput = wrapper.get('input[type="file"]')
    const file = {
      text: async () => '<p>Hello</p>',
    } as unknown as File
    Object.defineProperty(fileInput.element, 'files', { value: [file], configurable: true })
    await fileInput.trigger('change')
    await flushPromises()

    await wrapper.get('[data-testid="paste-summary-accept"]').trigger('click')
    await flushPromises()

    const accepted = wrapper.emitted('paste-accepted')?.at(-1)?.[0] as {
      blockedCount: number
      unresolvedPasteBlockers?: boolean
      items: unknown[]
    }
    expect(accepted).toBeTruthy()
    expect(accepted.blockedCount).toBe(0)
    expect(accepted.unresolvedPasteBlockers).toBe(false)
    expect(JSON.stringify(accepted)).not.toMatch(/sourceHtml|<p>Hello/i)
    wrapper.unmount()
  })

  it('keeps Accept disabled when paste-clean reports blocked=true', async () => {
    fetchMasterStyleCatalog.mockResolvedValue({
      catalogVersion: '1.0',
      entries: [{ styleKey: 'BodyText', applicableNodeTypes: ['paragraph'], renderPurpose: 'BODY' }],
    })
    pasteClean.mockResolvedValue({
      blocked: true,
      cleanedStructuredContentJson: null,
      summary: {
        items: [
          {
            category: 'BLOCKED',
            messageKey: 'paste.summary.blocked',
            detectionSummary: 'Embedded object detected.',
          },
        ],
        transformedCount: 0,
        removedCount: 0,
        warningCount: 0,
        blockedCount: 1,
      },
      prePasteSnapshotJson: '{"schemaVersion":"1.0","nodes":[]}',
    })

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(ControlledStructuredContentEditor, {
      props: {
        modelValue: '{"schemaVersion":"1.0","nodes":[]}',
        templateId: 'tpl-1',
      },
      attachTo: document.body,
      global: { plugins: [i18n, ElementPlus] },
    })

    await flushPromises()

    const fileInput = wrapper.get('input[type="file"]')
    const file = {
      text: async () => '<object></object>',
    } as unknown as File
    Object.defineProperty(fileInput.element, 'files', { value: [file], configurable: true })
    await fileInput.trigger('change')
    await flushPromises()

    const acceptButton = wrapper.get('[data-testid="paste-summary-accept"]').element as HTMLButtonElement
    expect(acceptButton.disabled).toBe(true)
    expect(wrapper.emitted('paste-accepted')).toBeFalsy()
    wrapper.unmount()
  })

  describe('CE-U01 nested structured editor', () => {
    const conditionWithChild =
      '{"schemaVersion":"1.0","nodes":[{"type":"conditionBlock","conditionExpression":"${flag}","children":[{"type":"paragraph","children":[{"type":"textRun","value":"nested"}]}]}]}'

    async function mountEditor(modelValue: string) {
      fetchMasterStyleCatalog.mockResolvedValue({
        catalogVersion: '1.0',
        entries: [{ styleKey: 'BodyText', applicableNodeTypes: ['paragraph'], renderPurpose: 'BODY' }],
      })
      const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
      const wrapper = mount(ControlledStructuredContentEditor, {
        props: {
          modelValue,
          templateId: 'tpl-1',
          baseline: modelValue,
        },
        global: { plugins: [i18n, ElementPlus] },
      })
      await flushPromises()
      return wrapper
    }

    it('BDD-CE-U01-NE-01 renders nested paragraph inside conditionBlock', async () => {
      const wrapper = await mountEditor(conditionWithChild)
      expect(wrapper.find('[data-testid="nested-block-children-0"]').exists()).toBe(true)
      expect(wrapper.find('[data-testid="structured-block-card-0-0"]').exists()).toBe(true)
      wrapper.unmount()
    })

    it('BDD-CE-U01-NE-02 inserts paragraph into empty condition children', async () => {
      const baseline =
        '{"schemaVersion":"1.0","nodes":[{"type":"conditionBlock","conditionExpression":"x","children":[]}]}'
      const wrapper = await mountEditor(baseline)

      const nestedButtons = wrapper.findAll('[data-testid="insert-nested-block-node"]')
      const paragraphBtn = nestedButtons.find((btn) => btn.text().includes('Paragraph'))
      await paragraphBtn?.trigger('click')
      await flushPromises()

      const emitted = wrapper.emitted('update:modelValue')?.at(-1)?.[0] as string
      expect(emitted).toContain('"type":"paragraph"')
      expect(emitted).toMatch(/"children":\[.*"type":"paragraph"/s)
      wrapper.unmount()
    })

    it('BDD-CE-U01-NE-03 hides nested add toolbar at max depth', async () => {
      const deep =
        '{"schemaVersion":"1.0","nodes":[{"type":"conditionBlock","conditionExpression":"a","children":[{"type":"loopBlock","loopVariable":"items","children":[{"type":"conditionBlock","conditionExpression":"b","children":[]}]}]}]}'
      const wrapper = await mountEditor(deep)

      expect(wrapper.find('[data-testid="nested-depth-limit"]').exists()).toBe(true)
      const deepestToolbar = wrapper.find('[data-testid="nested-block-children-0-0-0"]')
      expect(deepestToolbar.find('[data-testid="insert-nested-block-node"]').exists()).toBe(false)
      wrapper.unmount()
    })

    it('BDD-CE-U01-NE-04 undo/redo nested block insert', async () => {
      const baseline =
        '{"schemaVersion":"1.0","nodes":[{"type":"conditionBlock","conditionExpression":"x","children":[]}]}'
      const wrapper = await mountEditor(baseline)

      const paragraphBtn = wrapper
        .findAll('[data-testid="insert-nested-block-node"]')
        .find((btn) => btn.text().includes('Paragraph'))
      await paragraphBtn?.trigger('click')
      await flushPromises()

      const afterInsert = wrapper.emitted('update:modelValue')?.at(-1)?.[0] as string
      expect(afterInsert).toContain('"type":"paragraph"')

      await wrapper.get('[data-testid="structured-editor-undo"]').trigger('click')
      await flushPromises()

      const afterUndo = wrapper.emitted('update:modelValue')?.at(-1)?.[0] as string
      expect(afterUndo).not.toContain('"type":"paragraph"')

      await wrapper.get('[data-testid="structured-editor-redo"]').trigger('click')
      await flushPromises()

      const afterRedo = wrapper.emitted('update:modelValue')?.at(-1)?.[0] as string
      expect(afterRedo).toContain('"type":"paragraph"')
      wrapper.unmount()
    })
  })

  describe('CE-U02 block sort / copy / validate scroll', () => {
    const twoParagraphs =
      '{"schemaVersion":"1.0","nodes":[{"type":"paragraph","children":[{"type":"textRun","value":"A"}]},{"type":"paragraph","children":[{"type":"textRun","value":"B"}]}]}'

    async function mountEditor(modelValue: string, extraProps: Record<string, unknown> = {}) {
      fetchMasterStyleCatalog.mockResolvedValue({
        catalogVersion: '1.0',
        entries: [{ styleKey: 'BodyText', applicableNodeTypes: ['paragraph'], renderPurpose: 'BODY' }],
      })
      const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
      const wrapper = mount(ControlledStructuredContentEditor, {
        props: {
          modelValue,
          templateId: 'tpl-1',
          baseline: modelValue,
          ...extraProps,
        },
        global: { plugins: [i18n, ElementPlus] },
        attachTo: document.body,
      })
      await flushPromises()
      return wrapper
    }

    it('BDD-CE-U02-BS-02 copies a block as adjacent sibling', async () => {
      const wrapper = await mountEditor(twoParagraphs)
      await wrapper.get('[data-testid="structured-block-copy"]').trigger('click')
      await flushPromises()

      const latest = wrapper.emitted('update:modelValue')?.at(-1)?.[0] as string
      const parsed = JSON.parse(latest) as { nodes: Array<{ children?: Array<{ value?: string }> }> }
      expect(parsed.nodes).toHaveLength(3)
      expect(parsed.nodes[1]?.children?.[0]?.value).toBe('A')
      wrapper.unmount()
    })

    it('BDD-CE-U02-BS-04 undo restores copied block removal', async () => {
      const wrapper = await mountEditor(twoParagraphs)
      await wrapper.get('[data-testid="structured-block-copy"]').trigger('click')
      await flushPromises()
      await wrapper.get('[data-testid="structured-editor-undo"]').trigger('click')
      await flushPromises()
      const latest = wrapper.emitted('update:modelValue')?.at(-1)?.[0] as string
      const parsed = JSON.parse(latest) as { nodes: unknown[] }
      expect(parsed.nodes).toHaveLength(2)
      wrapper.unmount()
    })

    it('BDD-CE-U02-BS-03 validate structure lists issues and scrolls to block', async () => {
      const invalidVariable =
        '{"schemaVersion":"1.0","nodes":[{"type":"paragraph","children":[{"type":"variable","key":"ghostVar"}]}]}'
      const wrapper = await mountEditor(invalidVariable, {
        variables: [{ variableKey: 'customerName', variableType: 'TEXT', required: false, defaultValue: null, enumValues: null, description: null }],
      })

      await wrapper.get('[data-testid="structured-editor-validate-structure"]').trigger('click')
      await flushPromises()

      const issues = wrapper.findAll('[data-testid="structured-editor-validation-issue"]')
      expect(issues.length).toBeGreaterThan(0)

      const scrollIntoView = vi.fn()
      Object.defineProperty(HTMLElement.prototype, 'scrollIntoView', {
        configurable: true,
        value: scrollIntoView,
      })
      await issues[0]?.trigger('click')
      expect(scrollIntoView).toHaveBeenCalled()
      wrapper.unmount()
    })

    it('BDD-CE-U02-BS-05 hides reorder/copy/validate controls when readonly', async () => {
      const wrapper = await mountEditor(twoParagraphs, { readonly: true })
      expect(wrapper.find('[data-testid="structured-block-drag-handle"]').exists()).toBe(false)
      expect(wrapper.find('[data-testid="structured-block-copy"]').exists()).toBe(false)
      expect(wrapper.find('[data-testid="structured-editor-validate-structure"]').exists()).toBe(false)
      wrapper.unmount()
    })
  })
})
