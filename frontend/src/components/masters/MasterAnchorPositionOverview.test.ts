import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { describe, expect, it } from 'vitest'
import MasterAnchorPositionOverview from '@/components/masters/MasterAnchorPositionOverview.vue'
import en from '@/i18n/locales/en'
import type { MasterAnchor } from '@/types/master'

const anchors: MasterAnchor[] = [
  { anchorId: 'FOOTER', displayLabel: 'Footer', documentSequence: 2 },
  { anchorId: 'HEADER', displayLabel: 'Header', documentSequence: 0 },
  { anchorId: 'BODY', displayLabel: 'Body', documentSequence: 1 },
]

function mountOverview(overrides: { canEditDisplayLabel?: boolean } = {}) {
  const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
  return mount(MasterAnchorPositionOverview, {
    props: {
      anchors,
      canEditDisplayLabel: overrides.canEditDisplayLabel ?? true,
      columnFilters: { anchorId: '', displayLabel: '' },
    },
    global: {
      plugins: [i18n, ElementPlus],
    },
  })
}

describe('MasterAnchorPositionOverview', () => {
  it('BDD-CE-U06-MAC-001 — renders 1-based positions in document order; no DOCX canvas', async () => {
    const wrapper = mountOverview()
    await flushPromises()

    expect(wrapper.find('[data-testid="master-anchor-position-overview"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="master-anchor-position-HEADER"]').text()).toBe('1')
    expect(wrapper.find('[data-testid="master-anchor-position-BODY"]').text()).toBe('2')
    expect(wrapper.find('[data-testid="master-anchor-position-FOOTER"]').text()).toBe('3')
    expect(wrapper.text()).toMatch(/Document-order layout placeholder list/)
    expect(wrapper.html()).not.toMatch(/docx-canvas|wysiwyg/i)
  })

  it('BDD-CE-U06-MAC-002 — applies selected row class on row click', async () => {
    const wrapper = mountOverview()
    await flushPromises()

    const headerLabel = wrapper.find('[data-testid="master-anchor-label-HEADER"]')
    expect(headerLabel.exists()).toBe(true)
    await headerLabel.trigger('click')
    // Row click is on tr; click the cell's closest row via table row-click path
    const table = wrapper.findComponent({ name: 'ElTable' })
    if (table.exists()) {
      table.vm.$emit('row-click', anchors.find((a) => a.anchorId === 'HEADER'))
      await flushPromises()
    }

    expect(wrapper.find('.master-anchor-row--selected').exists()).toBe(true)
  })

  it('BDD-CE-U06-MAC-003 — shows edit control when writable and emits edit', async () => {
    const wrapper = mountOverview({ canEditDisplayLabel: true })
    await flushPromises()

    const editBtn = wrapper.find('[data-testid="master-anchor-edit-label-HEADER"]')
    expect(editBtn.exists()).toBe(true)
    await editBtn.trigger('click')
    expect(wrapper.emitted('editDisplayLabel')?.[0]?.[0]).toMatchObject({
      anchorId: 'HEADER',
      displayLabel: 'Header',
    })
  })

  it('BDD-CE-U06-MAC-005/006/007 — hides edit controls when read-only', async () => {
    const wrapper = mountOverview({ canEditDisplayLabel: false })
    await flushPromises()
    expect(wrapper.find('[data-testid="master-anchor-edit-label-HEADER"]').exists()).toBe(false)
  })
})
