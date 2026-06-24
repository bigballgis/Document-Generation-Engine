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
})
