import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus, { ElTableColumn } from 'element-plus'
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h, nextTick } from 'vue'
import AppDataTable from '@/components/common/AppDataTable.vue'
import en from '@/i18n/locales/en'

const i18n = createI18n({
  legacy: false,
  locale: 'en',
  messages: { en },
})

function mountTable(
  props: Record<string, unknown> = {},
  listeners: Record<string, unknown> = {},
) {
  const rows = [
    { id: 'a', name: 'Alpha' },
    { id: 'b', name: 'Beta' },
  ]

  return mount(AppDataTable, {
    props: {
      activatable: true,
      data: rows,
      ...props,
    },
    attrs: {
      ...listeners,
    },
    slots: {
      default: () =>
        h(
          defineComponent({
            name: 'StubColumns',
            setup() {
              return () =>
                h(
                  'el-table-column' as string,
                  { prop: 'name', label: 'Name' },
                )
            },
          }),
        ),
    },
    global: {
      plugins: [i18n, ElementPlus],
    },
  })
}

describe('AppDataTable activatable keyboard a11y', () => {
  it('makes activatable body rows focusable with aria-label', async () => {
    const wrapper = mountTable()
    await flushPromises()
    await nextTick()

    const rows = wrapper.element.querySelectorAll(
      'tbody tr.app-data-table__activatable-row',
    ) as NodeListOf<HTMLElement>
    expect(rows.length).toBeGreaterThan(0)
    expect(rows[0].tabIndex).toBe(0)
    expect(rows[0].getAttribute('aria-label')).toBeTruthy()
    // Preserve native table row semantics — do not override with role=button.
    expect(rows[0].getAttribute('role')).not.toBe('button')
  })

  it('activates the focused row on Enter via the row-click listener', async () => {
    const onRowClick = vi.fn()
    const wrapper = mountTable({}, { onRowClick })
    await flushPromises()
    await nextTick()

    const row = wrapper.element.querySelector(
      'tbody tr.app-data-table__activatable-row',
    ) as HTMLElement
    expect(row).toBeTruthy()
    row.focus()
    row.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }))

    expect(onRowClick).toHaveBeenCalled()
    expect(onRowClick.mock.calls[0][0]).toMatchObject({ id: 'a', name: 'Alpha' })
  })

  it('activates the focused row on Space', async () => {
    const onRowClick = vi.fn()
    const wrapper = mountTable({}, { onRowClick })
    await flushPromises()
    await nextTick()

    const row = wrapper.element.querySelector(
      'tbody tr.app-data-table__activatable-row',
    ) as HTMLElement
    row.focus()
    row.dispatchEvent(new KeyboardEvent('keydown', { key: ' ', bubbles: true }))

    expect(onRowClick).toHaveBeenCalled()
  })

  it('does not put tabindex on non-activatable tables', async () => {
    const wrapper = mount(AppDataTable, {
      props: {
        activatable: false,
        data: [{ id: 'x', name: 'X' }],
      },
      global: {
        plugins: [i18n, ElementPlus],
      },
    })
    await flushPromises()
    await nextTick()

    const rows = wrapper.element.querySelectorAll('tbody tr') as NodeListOf<HTMLElement>
    for (const row of Array.from(rows)) {
      expect(row.classList.contains('app-data-table__activatable-row')).toBe(false)
      expect(row.tabIndex).toBeLessThan(0)
    }
  })

  it('after client sort, Enter activates the same row object as click on that visible row', async () => {
    const onRowClick = vi.fn()
    // Parent :data stays insertion order; EP client sort reorders display only.
    const rows = [
      { id: 'older', name: 'Alpha', createdAt: '2020-01-01T00:00:00Z' },
      { id: 'newer', name: 'Beta', createdAt: '2024-06-01T00:00:00Z' },
    ]
    const Host = defineComponent({
      components: { AppDataTable, ElTableColumn },
      setup() {
        return { rows, onRowClick }
      },
      template: `
        <AppDataTable activatable :data="rows" @row-click="onRowClick">
          <ElTableColumn prop="name" label="Name" />
          <ElTableColumn prop="createdAt" label="Created" sortable />
        </AppDataTable>
      `,
    })
    const wrapper = mount(Host, {
      global: {
        plugins: [i18n, ElementPlus],
      },
    })
    await flushPromises()
    await nextTick()

    const tableApi = (
      wrapper.findComponent(AppDataTable).vm as unknown as {
        $: {
          refs: {
            tableRef: {
              sort: (prop: string, order: string) => void
              store: { states: { data: { value: Array<{ id: string }> } } }
            }
          }
        }
      }
    ).$.refs.tableRef

    tableApi.sort('createdAt', 'descending')
    await flushPromises()
    await nextTick()
    await nextTick()

    const sorted = tableApi.store.states.data.value
    expect(sorted.map((r) => r.id)).toEqual(['newer', 'older'])
    // Parent :data is unchanged — DOM index into attrs.data would wrongly yield "older".
    expect(rows.map((r) => r.id)).toEqual(['older', 'newer'])

    const visibleRows = wrapper.element.querySelectorAll(
      'tbody tr.app-data-table__activatable-row',
    ) as NodeListOf<HTMLElement>
    expect(visibleRows.length).toBe(2)
    const firstVisible = visibleRows[0]

    onRowClick.mockClear()
    firstVisible.click()
    expect(onRowClick).toHaveBeenCalledOnce()
    const clickRow = onRowClick.mock.calls[0][0] as { id: string }
    expect(clickRow.id).toBe('newer')

    onRowClick.mockClear()
    firstVisible.focus()
    firstVisible.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }))

    expect(onRowClick).toHaveBeenCalledOnce()
    const keyboardRow = onRowClick.mock.calls[0][0] as { id: string }
    expect(keyboardRow.id).toBe(clickRow.id)
    expect(keyboardRow.id).toBe('newer')
    expect(keyboardRow.id).not.toBe(rows[0].id)
  })
})
