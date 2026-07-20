import { mount } from '@vue/test-utils'
import ElementPlus from 'element-plus'
import { describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import TableEditMoreActions from '@/components/common/TableEditMoreActions.vue'
import en from '@/i18n/locales/en'

describe('TableEditMoreActions (BDD-SYS-NORM-W1-007)', () => {
  function mountActions() {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    return mount(TableEditMoreActions, {
      global: { plugins: [i18n, ElementPlus] },
      slots: {
        more: '<el-dropdown-menu><el-dropdown-item>Disable</el-dropdown-item></el-dropdown-menu>',
      },
    })
  }

  it('exposes shared Edit/More test id and labels', () => {
    const wrapper = mountActions()
    expect(wrapper.find('[data-testid="table-edit-more-actions"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Edit')
    expect(wrapper.text()).toContain('More')
  })

  it('emits edit when Edit is clicked', async () => {
    const wrapper = mountActions()
    await wrapper.find('.table-edit-more-actions__edit').trigger('click')
    expect(wrapper.emitted('edit')).toHaveLength(1)
  })
})
