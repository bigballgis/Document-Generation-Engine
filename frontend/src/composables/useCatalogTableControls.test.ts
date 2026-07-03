import { describe, expect, it } from 'vitest'
import { computed, ref } from 'vue'
import { useCatalogTableControls } from '@/composables/useCatalogTableControls'

interface SampleRow {
  name: string
  groupCode: string
  status: string
  updatedAt: string
}

describe('useCatalogTableControls', () => {
  const rows = ref<SampleRow[]>([
    { name: 'Alpha', groupCode: 'CORP', status: 'APPROVED', updatedAt: '2026-07-01T00:00:00Z' },
    { name: 'Beta', groupCode: 'RETAIL', status: 'DRAFT', updatedAt: '2026-07-03T00:00:00Z' },
  ])

  it('filters rows by search query across configured getters', () => {
    const { searchQuery, sortedRows } = useCatalogTableControls(rows, {
      searchGetters: [(row) => row.name, (row) => row.groupCode],
      filters: [],
      sortOptions: [
        {
          key: 'updatedAtDesc',
          labelKey: 'table.sort.updatedAtDesc',
          getter: (row) => row.updatedAt,
          order: 'desc',
        },
      ],
      defaultSortKey: 'updatedAtDesc',
    })

    searchQuery.value = 'retail'
    expect(sortedRows.value.map((row) => row.name)).toEqual(['Beta'])
  })

  it('applies exact filter and default descending sort', () => {
    const source = computed(() => rows.value)
    const { filters, sortedRows } = useCatalogTableControls(source, {
      searchGetters: [(row) => row.name],
      filters: [
        {
          key: 'status',
          labelKey: 'masters.list.columns.status',
          getValue: (row) => row.status,
          matchMode: 'exact',
        },
      ],
      sortOptions: [
        {
          key: 'updatedAtDesc',
          labelKey: 'table.sort.updatedAtDesc',
          getter: (row) => row.updatedAt,
          order: 'desc',
        },
      ],
      defaultSortKey: 'updatedAtDesc',
    })

    filters.status = 'APPROVED'
    expect(sortedRows.value.map((row) => row.name)).toEqual(['Alpha'])
  })

  it('clears search and filters together', () => {
    const { searchQuery, filters, clearAll, hasAnyActive } = useCatalogTableControls(rows, {
      searchGetters: [(row) => row.name],
      filters: [
        {
          key: 'groupCode',
          labelKey: 'masters.list.columns.group',
          getValue: (row) => row.groupCode,
        },
      ],
      sortOptions: [
        {
          key: 'nameAsc',
          labelKey: 'table.sort.nameAsc',
          getter: (row) => row.name,
          order: 'asc',
        },
      ],
      defaultSortKey: 'nameAsc',
    })

    searchQuery.value = 'Alpha'
    filters.groupCode = 'CORP'
    expect(hasAnyActive.value).toBe(true)

    clearAll()
    expect(hasAnyActive.value).toBe(false)
    expect(searchQuery.value).toBe('')
    expect(filters.groupCode).toBe('')
  })
})
