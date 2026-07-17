import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  clearAuthoringEditorContext,
  registerAuthoringEditorContext,
} from '@/composables/authoringEditorContext'
import { handleAuthoringShortcutKeydown } from '@/composables/useAuthoringEditorShortcuts'

function makeContext(overrides: Partial<{
  saveBinding: ReturnType<typeof vi.fn>
  refreshPreview: ReturnType<typeof vi.fn>
  canSave: boolean
  canRefresh: boolean
  isSaving: boolean
  isRefreshing: boolean
}> = {}) {
  const saveBinding = overrides.saveBinding ?? vi.fn()
  const refreshPreview = overrides.refreshPreview ?? vi.fn()
  return {
    saveBinding,
    refreshPreview,
    canSave: () => overrides.canSave ?? true,
    canRefresh: () => overrides.canRefresh ?? true,
    isSaving: () => overrides.isSaving ?? false,
    isRefreshing: () => overrides.isRefreshing ?? false,
  }
}

function dispatchKey(
  key: string,
  mods: { ctrlKey?: boolean; metaKey?: boolean } = { ctrlKey: true },
) {
  const event = new KeyboardEvent('keydown', {
    key,
    bubbles: true,
    cancelable: true,
    ctrlKey: mods.ctrlKey ?? false,
    metaKey: mods.metaKey ?? false,
  })
  const prevent = vi.spyOn(event, 'preventDefault')
  const stop = vi.spyOn(event, 'stopPropagation')
  handleAuthoringShortcutKeydown(event)
  return { event, prevent, stop }
}

describe('useAuthoringEditorShortcuts (CE-U17)', () => {
  beforeEach(() => {
    clearAuthoringEditorContext()
    document.body.innerHTML = ''
  })

  afterEach(() => {
    clearAuthoringEditorContext()
    document.body.innerHTML = ''
  })

  it('BDD-CE-U17-EKS-005: no-ops without edit-surface context', () => {
    const { prevent } = dispatchKey('s')
    expect(prevent).not.toHaveBeenCalled()
  })

  it('BDD-CE-U17-EKS-001: Ctrl+S calls saveBinding and preventDefault', () => {
    const ctx = makeContext()
    registerAuthoringEditorContext(ctx)
    const { prevent, stop } = dispatchKey('s', { ctrlKey: true })
    expect(prevent).toHaveBeenCalled()
    expect(stop).toHaveBeenCalled()
    expect(ctx.saveBinding).toHaveBeenCalledOnce()
  })

  it('BDD-CE-U17-EKS-001: Cmd+S (meta) calls saveBinding', () => {
    const ctx = makeContext()
    registerAuthoringEditorContext(ctx)
    dispatchKey('s', { metaKey: true })
    expect(ctx.saveBinding).toHaveBeenCalledOnce()
  })

  it('BDD-CE-U17-EKS-002: Ctrl+P calls refreshPreview and preventDefault', () => {
    const ctx = makeContext()
    registerAuthoringEditorContext(ctx)
    const { prevent } = dispatchKey('p', { ctrlKey: true })
    expect(prevent).toHaveBeenCalled()
    expect(ctx.refreshPreview).toHaveBeenCalledOnce()
  })

  it('BDD-CE-U17-EKS-006: suppresses save/refresh when aria-modal is open', () => {
    const modal = document.createElement('div')
    modal.setAttribute('aria-modal', 'true')
    document.body.appendChild(modal)
    const ctx = makeContext()
    registerAuthoringEditorContext(ctx)
    dispatchKey('s')
    dispatchKey('p')
    expect(ctx.saveBinding).not.toHaveBeenCalled()
    expect(ctx.refreshPreview).not.toHaveBeenCalled()
  })

  it('BDD-CE-U17-EKS-006/D8: ignores closed Element Plus overlays (display:none parent)', () => {
    const overlay = document.createElement('div')
    overlay.className = 'el-overlay el-modal-dialog'
    overlay.style.display = 'none'
    const modal = document.createElement('div')
    modal.className = 'el-overlay-dialog'
    modal.setAttribute('aria-modal', 'true')
    modal.setAttribute('role', 'dialog')
    overlay.appendChild(modal)
    document.body.appendChild(overlay)

    const ctx = makeContext()
    registerAuthoringEditorContext(ctx)
    dispatchKey('s')
    dispatchKey('p')
    expect(ctx.saveBinding).toHaveBeenCalledOnce()
    expect(ctx.refreshPreview).toHaveBeenCalledOnce()
  })

  it('BDD-CE-U17-EKS-008: fail-closed — no save API when canSave is false', () => {
    const ctx = makeContext({ canSave: false })
    registerAuthoringEditorContext(ctx)
    const { prevent } = dispatchKey('s')
    expect(prevent).toHaveBeenCalled()
    expect(ctx.saveBinding).not.toHaveBeenCalled()
  })

  it('BDD-CE-U17-EKS-009: ignores shortcuts while saving or refreshing', () => {
    const saving = makeContext({ isSaving: true })
    registerAuthoringEditorContext(saving)
    dispatchKey('s')
    expect(saving.saveBinding).not.toHaveBeenCalled()

    clearAuthoringEditorContext()
    const refreshing = makeContext({ isRefreshing: true })
    registerAuthoringEditorContext(refreshing)
    dispatchKey('p')
    expect(refreshing.refreshPreview).not.toHaveBeenCalled()
  })

  it('does not steal plain character keys', () => {
    const ctx = makeContext()
    registerAuthoringEditorContext(ctx)
    const event = new KeyboardEvent('keydown', { key: 's', bubbles: true, cancelable: true })
    const prevent = vi.spyOn(event, 'preventDefault')
    handleAuthoringShortcutKeydown(event)
    expect(prevent).not.toHaveBeenCalled()
    expect(ctx.saveBinding).not.toHaveBeenCalled()
  })
})
