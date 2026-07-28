import { describe, expect, it, vi } from 'vitest'
import {
  registerWorkspaceTabDirtyLeave,
  requestWorkspaceTabLeave,
} from '@/composables/workspaceTabDirtyLeave'

describe('workspaceTabDirtyLeave (FOS-W2-8)', () => {
  it('runs leave immediately when no handler is registered', async () => {
    registerWorkspaceTabDirtyLeave(null)
    const leave = vi.fn()
    await requestWorkspaceTabLeave(leave)
    expect(leave).toHaveBeenCalledOnce()
  })

  it('blocks leave until the registered handler invokes it', async () => {
    const leave = vi.fn()
    let captured: (() => void | Promise<void>) | null = null
    registerWorkspaceTabDirtyLeave((next) => {
      captured = next
    })
    await requestWorkspaceTabLeave(leave)
    expect(leave).not.toHaveBeenCalled()
    expect(captured).toBeTypeOf('function')
    await captured!()
    expect(leave).toHaveBeenCalledOnce()
    registerWorkspaceTabDirtyLeave(null)
  })
})
