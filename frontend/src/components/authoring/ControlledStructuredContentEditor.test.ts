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
})
