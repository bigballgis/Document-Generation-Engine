import { describe, expect, it, vi } from 'vitest'
import { buildAuthoringPaletteActions, formatModShortcut } from '@/composables/buildAuthoringPaletteActions'
import type { AuthoringEditorContext } from '@/composables/authoringEditorContext'

function translate(key: string): string {
  const map: Record<string, string> = {
    'commandPalette.actions.saveBinding': 'Save binding',
    'commandPalette.actions.refreshPreview': 'Refresh preview',
  }
  return map[key] ?? key
}

function makeContext(overrides: Partial<{
  canSave: boolean
  canRefresh: boolean
  isSaving: boolean
  isRefreshing: boolean
}> = {}): AuthoringEditorContext & {
  saveBinding: ReturnType<typeof vi.fn>
  refreshPreview: ReturnType<typeof vi.fn>
} {
  const saveBinding = vi.fn()
  const refreshPreview = vi.fn()
  return {
    saveBinding,
    refreshPreview,
    canSave: () => overrides.canSave ?? true,
    canRefresh: () => overrides.canRefresh ?? true,
    isSaving: () => overrides.isSaving ?? false,
    isRefreshing: () => overrides.isRefreshing ?? false,
  }
}

describe('buildAuthoringPaletteActions (CE-U17)', () => {
  it('returns empty without edit context', () => {
    expect(buildAuthoringPaletteActions(null, translate, 'other')).toEqual([])
  })

  it('BDD-CE-U17-EKS-003/004: lists Save binding and Refresh preview with shortcut hints', () => {
    const ctx = makeContext()
    const items = buildAuthoringPaletteActions(ctx, translate, 'other')
    expect(items.map((i) => i.id)).toEqual(['action:save-binding', 'action:refresh-preview'])
    expect(items[0]).toMatchObject({
      kind: 'action',
      title: 'Save binding',
      subtitle: 'Ctrl+S',
      optionTestId: 'command-palette-action-save-binding',
    })
    expect(items[1]).toMatchObject({
      kind: 'action',
      title: 'Refresh preview',
      subtitle: 'Ctrl+P',
      optionTestId: 'command-palette-action-refresh-preview',
    })
    expect(formatModShortcut('S', 'mac')).toBe('⌘S')
  })

  it('BDD-CE-U17-EKS-008: omits Save binding when canSave is false', () => {
    const ctx = makeContext({ canSave: false })
    const items = buildAuthoringPaletteActions(ctx, translate, 'other')
    expect(items.map((i) => i.id)).toEqual(['action:refresh-preview'])
  })

  it('executes handlers and respects busy guards', async () => {
    const ctx = makeContext()
    const items = buildAuthoringPaletteActions(ctx, translate, 'other')
    await items[0]!.execute!()
    await items[1]!.execute!()
    expect(ctx.saveBinding).toHaveBeenCalledOnce()
    expect(ctx.refreshPreview).toHaveBeenCalledOnce()

    const busy = makeContext({ isSaving: true, isRefreshing: true })
    const busyItems = buildAuthoringPaletteActions(busy, translate, 'other')
    await busyItems[0]!.execute!()
    await busyItems[1]!.execute!()
    expect(busy.saveBinding).not.toHaveBeenCalled()
    expect(busy.refreshPreview).not.toHaveBeenCalled()
  })
})
