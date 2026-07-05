import { describe, expect, it, vi } from 'vitest'
import { useActivatableTableRow } from '@/composables/useActivatableTableRow'

describe('useActivatableTableRow', () => {
  it('forwards row activation through onRowClick', () => {
    const activate = vi.fn()
    const { onRowClick, rowClassName } = useActivatableTableRow(activate)

    onRowClick({ id: 'tpl-1' })

    expect(activate).toHaveBeenCalledWith({ id: 'tpl-1' })
    expect(rowClassName()).toBe('app-data-table__activatable-row')
  })

  it('activates the row on Enter keydown', () => {
    const activate = vi.fn()
    const { onRowKeydown } = useActivatableTableRow(activate)
    const preventDefault = vi.fn()
    const event = { key: 'Enter', preventDefault } as unknown as KeyboardEvent

    onRowKeydown({ id: 'tpl-2' }, event)

    expect(preventDefault).toHaveBeenCalledOnce()
    expect(activate).toHaveBeenCalledWith({ id: 'tpl-2' })
  })

  it('activates on Space only when the row TR has focus', () => {
    const activate = vi.fn()
    const { onRowKeydown } = useActivatableTableRow(activate)
    const preventDefault = vi.fn()
    const tr = document.createElement('tr')
    const trEvent = {
      key: ' ',
      target: tr,
      preventDefault,
    } as unknown as KeyboardEvent
    const buttonTarget = document.createElement('button')
    const buttonEvent = {
      key: ' ',
      target: buttonTarget,
      preventDefault,
    } as unknown as KeyboardEvent

    onRowKeydown({ id: 'r1' }, trEvent)
    onRowKeydown({ id: 'r2' }, buttonEvent)

    expect(activate).toHaveBeenCalledOnce()
    expect(activate).toHaveBeenCalledWith({ id: 'r1' })
  })

  it('ignores other keys', () => {
    const activate = vi.fn()
    const { onRowKeydown } = useActivatableTableRow(activate)
    const event = { key: 'ArrowDown' } as unknown as KeyboardEvent

    onRowKeydown({ id: 'r3' }, event)

    expect(activate).not.toHaveBeenCalled()
  })
})
