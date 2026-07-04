import { mount } from '@vue/test-utils'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, nextTick } from 'vue'
import ElementPlus from 'element-plus'

import { useRouteScopedAbortController } from './useRouteScopedAbortController'

describe('useRouteScopedAbortController', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('aborts the active signal when the component unmounts', async () => {
    const abortSpy = vi.spyOn(AbortController.prototype, 'abort')

    const Harness = defineComponent({
      setup() {
        const { signal } = useRouteScopedAbortController()
        return { signal }
      },
      template: '<div />',
    })

    const wrapper = mount(Harness, {
      global: {
        plugins: [ElementPlus],
      },
    })

    expect(wrapper.vm.signal.aborted).toBe(false)
    wrapper.unmount()
    await nextTick()

    expect(abortSpy).toHaveBeenCalled()
  })

  it('renews the controller when requested', () => {
    const Harness = defineComponent({
      setup() {
        const { signal, renewController } = useRouteScopedAbortController()
        return { signal, renewController }
      },
      template: '<div />',
    })

    const wrapper = mount(Harness, {
      global: {
        plugins: [ElementPlus],
      },
    })

    const firstSignal = wrapper.vm.signal
    wrapper.vm.renewController()

    expect(firstSignal.aborted).toBe(true)
    expect(wrapper.vm.signal.aborted).toBe(false)
    expect(wrapper.vm.signal).not.toBe(firstSignal)
  })
})
