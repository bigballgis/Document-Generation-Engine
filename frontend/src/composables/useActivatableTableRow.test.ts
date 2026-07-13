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

  it('activates the row on Enter when the TR has focus', () => {
    const activate = vi.fn()
    const { onRowKeydown } = useActivatableTableRow(activate)
    const preventDefault = vi.fn()
    const tr = document.createElement('tr')
    const event = { key: 'Enter', target: tr, preventDefault } as unknown as KeyboardEvent

    onRowKeydown({ id: 'tpl-2' }, event)

    expect(preventDefault).toHaveBeenCalledOnce()
    expect(activate).toHaveBeenCalledWith({ id: 'tpl-2' })
  })

  it('does not activate Enter when an inner control has focus', () => {
    const activate = vi.fn()
    const { onRowKeydown } = useActivatableTableRow(activate)
    const preventDefault = vi.fn()
    const button = document.createElement('button')
    const event = { key: 'Enter', target: button, preventDefault } as unknown as KeyboardEvent

    onRowKeydown({ id: 'tpl-inner' }, event)

    expect(activate).not.toHaveBeenCalled()
    expect(preventDefault).not.toHaveBeenCalled()
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
    const tr = document.createElement('tr')
    const event = { key: 'ArrowDown', target: tr } as unknown as KeyboardEvent

    onRowKeydown({ id: 'r3' }, event)

    expect(activate).not.toHaveBeenCalled()
  })

  it('exposes focusable row a11y attrs with optional aria-label (keeps native row role)', () => {
    const activate = vi.fn()
    const { getRowA11yAttrs } = useActivatableTableRow<{ id: string; name: string }>(activate, {
      getAriaLabel: (row) => `Open ${row.name}`,
    })

    expect(getRowA11yAttrs({ id: '1', name: 'Alpha' })).toEqual({
      tabindex: 0,
      'aria-label': 'Open Alpha',
    })
  })

  it('exposes focusable row a11y attrs without aria-label when no getter is provided', () => {
    const activate = vi.fn()
    const { getRowA11yAttrs } = useActivatableTableRow(activate)

    expect(getRowA11yAttrs({ id: '1' })).toEqual({ tabindex: 0 })
  })

  it('shouldActivateFromKeyboard requires the TR as event target for Enter and Space', () => {
    const activate = vi.fn()
    const { shouldActivateFromKeyboard } = useActivatableTableRow(activate)
    const tr = document.createElement('tr')
    const button = document.createElement('button')

    expect(
      shouldActivateFromKeyboard({ key: 'Enter', target: tr } as unknown as KeyboardEvent),
    ).toBe(true)
    expect(
      shouldActivateFromKeyboard({ key: 'Enter', target: button } as unknown as KeyboardEvent),
    ).toBe(false)
    expect(
      shouldActivateFromKeyboard({ key: ' ', target: tr } as unknown as KeyboardEvent),
    ).toBe(true)
    expect(
      shouldActivateFromKeyboard({ key: ' ', target: button } as unknown as KeyboardEvent),
    ).toBe(false)
    expect(
      shouldActivateFromKeyboard({ key: 'Tab', target: tr } as unknown as KeyboardEvent),
    ).toBe(false)
  })
})
