import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ElMessageBox } from 'element-plus'
import {
  presentBindingVersionConflict,
  resolveBindingVersionConflictAndReload,
} from '@/utils/bindingVersionConflict'
import type { MasterAnchorBindingRow } from '@/utils/masterAnchorBindingRows'
import type { AnchorBinding } from '@/types/template'

vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus')
  return {
    ...actual,
    ElMessageBox: {
      confirm: vi.fn(),
    },
  }
})

describe('resolveBindingVersionConflictAndReload', () => {
  const t = ((key: string) => key) as (key: string) => string

  const storeBinding = {
    anchorId: 'A',
    declaredContentType: 'TEXT',
    structuredContentJson: '{"from":"store"}',
    updatedAt: '2026-07-17T12:00:01.000Z',
  } as AnchorBinding

  const editingRow: MasterAnchorBindingRow = {
    anchorId: 'A',
    displayLabel: 'Anchor A',
    declaredContentType: 'TEXT',
    validationStatus: null,
    configured: true,
    binding: {
      anchorId: 'A',
      declaredContentType: 'TEXT',
      structuredContentJson: '{"from":"local"}',
      updatedAt: '2026-07-17T12:00:00.000Z',
    } as AnchorBinding,
  }

  beforeEach(() => {
    vi.mocked(ElMessageBox.confirm).mockReset()
  })

  it('on Reload: fetchTemplate → onUpdated → reload with store binding when present', async () => {
    vi.mocked(ElMessageBox.confirm).mockResolvedValue('confirm' as never)
    const fetchTemplate = vi.fn().mockResolvedValue(undefined)
    const onUpdated = vi.fn()
    const reloadBindingFromServer = vi.fn().mockResolvedValue(undefined)
    let bindingsAfterFetch: AnchorBinding[] | undefined

    const action = await resolveBindingVersionConflictAndReload({
      t,
      fetchTemplate: async () => {
        await fetchTemplate()
        bindingsAfterFetch = [storeBinding]
      },
      onUpdated,
      editingAnchorId: () => 'A',
      editingRow: () => editingRow,
      storeBindings: () => bindingsAfterFetch,
      reloadBindingFromServer,
    })

    expect(action).toBe('reload')
    expect(fetchTemplate).toHaveBeenCalledOnce()
    expect(onUpdated).toHaveBeenCalledOnce()
    expect(reloadBindingFromServer).toHaveBeenCalledWith({
      ...editingRow,
      binding: storeBinding,
    })
  })

  it('on Keep editing: does not fetch or reload', async () => {
    vi.mocked(ElMessageBox.confirm).mockRejectedValue('cancel')
    const fetchTemplate = vi.fn()
    const onUpdated = vi.fn()
    const reloadBindingFromServer = vi.fn()

    const action = await resolveBindingVersionConflictAndReload({
      t,
      fetchTemplate,
      onUpdated,
      editingAnchorId: () => 'A',
      editingRow: () => editingRow,
      storeBindings: () => [storeBinding],
      reloadBindingFromServer,
    })

    expect(action).toBe('keep')
    expect(fetchTemplate).not.toHaveBeenCalled()
    expect(onUpdated).not.toHaveBeenCalled()
    expect(reloadBindingFromServer).not.toHaveBeenCalled()
  })

  it('on Reload without store binding: reloads editing row as-is', async () => {
    vi.mocked(ElMessageBox.confirm).mockResolvedValue('confirm' as never)
    const reloadBindingFromServer = vi.fn().mockResolvedValue(undefined)

    await resolveBindingVersionConflictAndReload({
      t,
      fetchTemplate: vi.fn().mockResolvedValue(undefined),
      onUpdated: vi.fn(),
      editingAnchorId: () => 'A',
      editingRow: () => editingRow,
      storeBindings: () => undefined,
      reloadBindingFromServer,
    })

    expect(reloadBindingFromServer).toHaveBeenCalledWith(editingRow)
  })

  it('presentBindingVersionConflict remains the dialog primitive', async () => {
    vi.mocked(ElMessageBox.confirm).mockResolvedValue('confirm' as never)
    await expect(presentBindingVersionConflict(t)).resolves.toBe('reload')
  })
})
