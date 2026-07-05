import { describe, expect, it, vi, beforeEach } from 'vitest'
import { ref } from 'vue'

// Mock lifecycle hooks so the composable can run outside a component setup.
vi.mock('vue', async () => {
  const actual = await vi.importActual<typeof import('vue')>('vue')
  return {
    ...actual,
    onMounted: vi.fn((fn: () => void) => fn()),
    onBeforeUnmount: vi.fn(),
  }
})

vi.mock('element-plus', () => ({
  ElMessageBox: {
    confirm: vi.fn(() => Promise.resolve(true)),
  },
}))

vi.mock('vue-i18n', () => ({
  useI18n: () => ({
    t: (key: string) => key,
    te: () => true,
  }),
}))

const leaveGuards: Array<() => Promise<boolean>> = []
vi.mock('vue-router', () => ({
  onBeforeRouteLeave: (guard: () => Promise<boolean>) => {
    leaveGuards.push(guard)
  },
}))

import { ElMessageBox } from 'element-plus'
import { useDirtyGuard } from '@/composables/useDirtyGuard'

describe('useDirtyGuard', () => {
  beforeEach(() => {
    leaveGuards.length = 0
    vi.clearAllMocks()
  })

  it('allows navigation when the form is pristine', async () => {
    const dirty = ref(false)
    const { confirmDiscard } = useDirtyGuard(dirty)
    expect(await confirmDiscard()).toBe(true)
    expect(ElMessageBox.confirm).not.toHaveBeenCalled()
  })

  it('prompts and discards via guardDialogClose when the form is dirty', async () => {
    const dirty = ref(true)
    const onDiscard = vi.fn()
    const { guardDialogClose } = useDirtyGuard(dirty, { onDiscard })
    expect(await guardDialogClose()).toBe(true)
    expect(ElMessageBox.confirm).toHaveBeenCalledOnce()
    expect(onDiscard).toHaveBeenCalledOnce()
  })

  it('blocks when the user chooses to stay', async () => {
    vi.mocked(ElMessageBox.confirm).mockRejectedValueOnce(new Error('cancel'))
    const dirty = ref(true)
    const onDiscard = vi.fn()
    const { guardDialogClose } = useDirtyGuard(dirty, { onDiscard })
    expect(await guardDialogClose()).toBe(false)
    expect(onDiscard).not.toHaveBeenCalled()
  })

  it('registers a route-leave guard that prompts when dirty', async () => {
    const dirty = ref(true)
    useDirtyGuard(dirty)
    expect(leaveGuards).toHaveLength(1)
    const guard = leaveGuards[0]!
    expect(await guard()).toBe(true)
    expect(ElMessageBox.confirm).toHaveBeenCalledOnce()
  })

  it('suppress bypasses the guard', async () => {
    const dirty = ref(true)
    const { suppress, confirmDiscard } = useDirtyGuard(dirty)
    suppress()
    expect(await confirmDiscard()).toBe(true)
    expect(ElMessageBox.confirm).not.toHaveBeenCalled()
  })
})
