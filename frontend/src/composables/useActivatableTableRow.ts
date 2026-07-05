/**
 * LR-C12: keyboard a11y — rows are activatable via Enter/Space, not only click.
 * The composable returns keydown handlers that callers attach to row <tr> elements
 * (or pass through `el-table` row event wiring) so keyboard users can open a row
 * without a mouse. Space scroll is preserved by only activating on Space when the
 * row is the event target.
 */
export function useActivatableTableRow<T>(activate: (row: T) => void) {
  function onRowClick(row: T) {
    activate(row)
  }

  function onRowKeydown(row: T, event: KeyboardEvent) {
    // Enter activates the row. Space activates only when the row itself (not a button
    // inside it) has focus — otherwise we'd steal Space from inner controls.
    if (event.key === 'Enter') {
      event.preventDefault()
      activate(row)
      return
    }
    if (event.key === ' ' || event.key === 'Spacebar') {
      const target = event.target
      if (target instanceof HTMLElement && target.tagName === 'TR') {
        event.preventDefault()
        activate(row)
      }
    }
  }

  function rowClassName() {
    return 'app-data-table__activatable-row'
  }

  return {
    onRowClick,
    onRowKeydown,
    rowClassName,
  }
}
