import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { describe, expect, it } from 'vitest'
import FidelityWarningList from '@/components/authoring/FidelityWarningList.vue'
import en from '@/i18n/locales/en'
import type { FidelityWarning } from '@/types/template'

const warnings: FidelityWarning[] = [
  {
    code: 'IMAGE_SCALING_ADJUSTED',
    messageKey: 'authoring.fidelity.imageScaling',
    location: 'HEADER:node[0]',
    artifact: 'HEADER',
    viewed: false,
  },
  {
    code: 'UNRESOLVED_VARIABLE',
    messageKey: 'authoring.fidelity.unresolvedVariable',
    location: 'BODY:node[2]',
    artifact: 'BODY',
    viewed: true,
  },
]

describe('FidelityWarningList', () => {
  it('filters warnings by code, location, artifact, and viewed state', async () => {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const wrapper = mount(FidelityWarningList, {
      props: { warnings },
      global: { plugins: [i18n, ElementPlus] },
    })

    await flushPromises()
    expect(wrapper.findAll('.el-table__row').length).toBe(2)

    const textInputs = wrapper.findAll('input.el-input__inner')
    expect(textInputs.length).toBeGreaterThanOrEqual(2)

    await textInputs[0].setValue('UNRESOLVED')
    await flushPromises()
    expect(wrapper.findAll('.el-table__row').length).toBe(1)
    expect(wrapper.text()).toContain('UNRESOLVED_VARIABLE')

    await textInputs[0].setValue('')
    await textInputs[1].setValue('HEADER')
    await flushPromises()
    expect(wrapper.findAll('.el-table__row').length).toBe(1)
    expect(wrapper.text()).toContain('HEADER')

    await textInputs[1].setValue('')
    const selects = wrapper.findAll('.el-select')
    expect(selects.length).toBeGreaterThanOrEqual(2)
    await selects[0].trigger('click')
    await flushPromises()
    const bodyOption = [...document.querySelectorAll('.el-select-dropdown__item')].find((item) =>
      item.textContent?.includes('BODY'),
    )
    bodyOption?.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    await flushPromises()
    expect(wrapper.findAll('.el-table__row').length).toBe(1)
    expect(wrapper.text()).toContain('BODY')

    await selects[1].trigger('click')
    await flushPromises()
    const unviewedOption = [...document.querySelectorAll('.el-select-dropdown__item')].find((item) =>
      item.textContent?.includes('Unviewed only'),
    )
    unviewedOption?.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    await flushPromises()
    expect(wrapper.text()).toContain('Unviewed')
    expect(wrapper.text()).not.toContain('UNRESOLVED_VARIABLE')
  })
})
