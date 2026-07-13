/**
 * LR-C12: keyboard a11y — rows are activatable via Enter/Space, not only click.
 * Callers attach `onRowClick` to `@row-click`. `AppDataTable` (activatable) applies
 * focusable row attrs + keydown wiring so keyboard users can open a row without a mouse.
 * Enter/Space activate only when the row TR itself is the event target (inner controls keep
 * their own keyboard behavior; Space does not steal page scroll from non-row targets).
 * Native table `row` role is preserved (no role=button on <tr>) to avoid nested-interactive
 * axe failures; use aria-label for the accessible name.
 */

export type ActivatableRowA11yAttrs = {
  tabindex: 0
  'aria-label'?: string
}

export type UseActivatableTableRowOptions<T> = {
  /** Accessible name for the focusable row (announced with native row role). */
  getAriaLabel?: (row: T) => string
}

/** Enter/Space activate only when the focused element is the row TR itself. */
export function shouldActivateFromKeyboard(event: KeyboardEvent): boolean {
  if (event.key !== 'Enter' && event.key !== ' ' && event.key !== 'Spacebar') {
    return false
  }
  const target = event.target
  return target instanceof HTMLElement && target.tagName === 'TR'
}

export function useActivatableTableRow<T>(
  activate: (row: T) => void,
  options: UseActivatableTableRowOptions<T> = {},
) {
  function onRowClick(row: T) {
    activate(row)
  }

  function onRowKeydown(row: T, event: KeyboardEvent) {
    if (!shouldActivateFromKeyboard(event)) {
      return
    }
    event.preventDefault()
    activate(row)
  }

  function rowClassName() {
    return 'app-data-table__activatable-row'
  }

  function getRowA11yAttrs(row: T): ActivatableRowA11yAttrs {
    const label = options.getAriaLabel?.(row)
    if (label) {
      return { tabindex: 0, 'aria-label': label }
    }
    return { tabindex: 0 }
  }

  return {
    onRowClick,
    onRowKeydown,
    rowClassName,
    getRowA11yAttrs,
    shouldActivateFromKeyboard,
  }
}
