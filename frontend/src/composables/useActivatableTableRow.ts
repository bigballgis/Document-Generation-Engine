export function useActivatableTableRow<T>(activate: (row: T) => void) {
  function onRowClick(row: T) {
    activate(row)
  }

  function rowClassName() {
    return 'app-data-table__activatable-row'
  }

  return {
    onRowClick,
    rowClassName,
  }
}
