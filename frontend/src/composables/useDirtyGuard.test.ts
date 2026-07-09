import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { computed, defineComponent, nextTick, ref, type Plugin } from 'vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import en from '@/i18n/locales/en'
import { useDirtyGuard } from '@/composables/useDirtyGuard'

function makeI18n() {
  return createI18n({ legacy: false, locale: 'en', messages: { en } })
}

function mountHarness(
  setupFn: () => ReturnType<typeof useDirtyGuard> & { isDirty: ReturnType<typeof ref<boolean>> },
  router?: ReturnType<typeof createRouter>,
) {
  const Harness = defineComponent({
    setup() {
      return setupFn()
    },
    template: '<div data-testid="harness" />',
  })

  const plugins: (Plugin | [Plugin, ...unknown[]])[] = [makeI18n(), ElementPlus]
  if (router) {
    plugins.push(router)
  }

  return mount(Harness, { global: { plugins } })
}

describe('useDirtyGuard', () => {
  let addEventListenerSpy: ReturnType<typeof vi.spyOn>
  let removeEventListenerSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    addEventListenerSpy = vi.spyOn(window, 'addEventListener')
    removeEventListenerSpy = vi.spyOn(window, 'removeEventListener')
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('BDD-F7-B1-005 does not open dialog when pristine on requestLeave', async () => {
    const isDirty = ref(false)
    const wrapper = mountHarness(() => {
      const guard = useDirtyGuard({ isDirty })
      return { ...guard, isDirty }
    })

    const action = vi.fn()
    await wrapper.vm.requestLeave(action)

    expect(wrapper.vm.dialogVisible).toBe(false)
    expect(action).toHaveBeenCalledOnce()
  })

  it('BDD-F7-B1-001 opens dialog on requestLeave when dirty', async () => {
    const isDirty = ref(true)
    const wrapper = mountHarness(() => {
      const guard = useDirtyGuard({ isDirty })
      return { ...guard, isDirty }
    })

    const action = vi.fn()
    await wrapper.vm.requestLeave(action)

    expect(wrapper.vm.dialogVisible).toBe(true)
    expect(action).not.toHaveBeenCalled()
  })

  it('BDD-F7-B1-002 Stay keeps dirty and does not run pending action', async () => {
    const isDirty = ref(true)
    const wrapper = mountHarness(() => {
      const guard = useDirtyGuard({ isDirty })
      return { ...guard, isDirty }
    })

    const action = vi.fn()
    await wrapper.vm.requestLeave(action)
    wrapper.vm.handleStay()
    await nextTick()

    expect(wrapper.vm.dialogVisible).toBe(false)
    expect(isDirty.value).toBe(true)
    expect(action).not.toHaveBeenCalled()
  })

  it('BDD-F7-B1-003 Discard runs pending action', async () => {
    const isDirty = ref(true)
    const wrapper = mountHarness(() => {
      const guard = useDirtyGuard({ isDirty })
      return { ...guard, isDirty }
    })

    const action = vi.fn()
    await wrapper.vm.requestLeave(action)
    await wrapper.vm.handleDiscard()
    await nextTick()

    expect(action).toHaveBeenCalledOnce()
  })

  it('BDD-F7-B1-004 Save runs onSave then pending action when save succeeds', async () => {
    const isDirty = ref(true)
    const onSave = vi.fn().mockResolvedValue(true)
    const wrapper = mountHarness(() => {
      const guard = useDirtyGuard({ isDirty, onSave })
      return { ...guard, isDirty }
    })

    const action = vi.fn()
    await wrapper.vm.requestLeave(action)
    await wrapper.vm.handleSave()
    await flushPromises()

    expect(onSave).toHaveBeenCalledOnce()
    expect(action).toHaveBeenCalledOnce()
    expect(wrapper.vm.dialogVisible).toBe(false)
  })

  it('keeps dialog open when save fails', async () => {
    const isDirty = ref(true)
    const onSave = vi.fn().mockResolvedValue(false)
    const wrapper = mountHarness(() => {
      const guard = useDirtyGuard({ isDirty, onSave })
      return { ...guard, isDirty }
    })

    const action = vi.fn()
    await wrapper.vm.requestLeave(action)
    await wrapper.vm.handleSave()
    await flushPromises()

    expect(wrapper.vm.dialogVisible).toBe(true)
    expect(action).not.toHaveBeenCalled()
  })

  it('BDD-F7-B1-006 registers beforeunload while mounted', () => {
    const isDirty = ref(true)
    const wrapper = mountHarness(() => {
      const guard = useDirtyGuard({ isDirty })
      return { ...guard, isDirty }
    })

    expect(addEventListenerSpy).toHaveBeenCalledWith('beforeunload', expect.any(Function))

    wrapper.unmount()

    expect(removeEventListenerSpy).toHaveBeenCalledWith('beforeunload', expect.any(Function))
  })

  it('beforeunload preventDefault fires when dirty', () => {
    const isDirty = ref(true)
    mountHarness(() => {
      const guard = useDirtyGuard({ isDirty })
      return { ...guard, isDirty }
    })

    const handler = addEventListenerSpy.mock.calls.find(([event]) => event === 'beforeunload')?.[1] as
      | ((event: BeforeUnloadEvent) => void)
      | undefined

    expect(handler).toBeDefined()
    const event = new Event('beforeunload') as BeforeUnloadEvent
    const preventDefault = vi.fn()
    Object.defineProperty(event, 'preventDefault', { value: preventDefault })
    handler?.(event)
    expect(preventDefault).toHaveBeenCalled()
  })

  it('blocks vue-router navigation when dirty inside router-view', async () => {
    const isDirty = ref(true)
    const GuardedPage = defineComponent({
      setup() {
        useDirtyGuard({ isDirty })
        return () => null
      },
    })

    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', component: GuardedPage },
        { path: '/away', component: { template: '<div />' } },
      ],
    })

    const App = defineComponent({
      template: '<router-view />',
    })

    mount(App, {
      global: { plugins: [makeI18n(), ElementPlus, router] },
    })

    await router.push('/')
    await router.isReady()
    await router.push('/away')
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/')
  })

  it('guardDialogClose proceeds immediately when pristine', async () => {
    const isDirty = ref(false)
    const wrapper = mountHarness(() => {
      const guard = useDirtyGuard({ isDirty })
      return { ...guard, isDirty }
    })

    const onProceed = vi.fn()
    const allowed = await wrapper.vm.guardDialogClose(onProceed)

    expect(allowed).toBe(true)
    expect(onProceed).toHaveBeenCalledOnce()
  })

  it('showSaveAction is false without onSave handler', () => {
    const isDirty = ref(true)
    const wrapper = mountHarness(() => {
      const guard = useDirtyGuard({ isDirty })
      return { ...guard, isDirty }
    })

    expect(wrapper.vm.showSaveAction).toBe(false)
  })

  it('respects enabled=false even when dirty', async () => {
    const isDirty = ref(true)
    const wrapper = mountHarness(() => {
      const guard = useDirtyGuard({ isDirty, enabled: computed(() => false) })
      return { ...guard, isDirty }
    })

    const action = vi.fn()
    await wrapper.vm.requestLeave(action)

    expect(wrapper.vm.dialogVisible).toBe(false)
    expect(action).toHaveBeenCalledOnce()
  })
})
