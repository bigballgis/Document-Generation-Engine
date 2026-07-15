import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { describe, expect, it } from 'vitest'
import MasterAnchorDisplayLabelDialog from '@/components/masters/MasterAnchorDisplayLabelDialog.vue'
import en from '@/i18n/locales/en'

function mountDialog(props: {
  modelValue?: boolean
  anchorId?: string
  initialDisplayLabel?: string
  loading?: boolean
}) {
  const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
  return mount(MasterAnchorDisplayLabelDialog, {
    props: {
      modelValue: props.modelValue ?? true,
      anchorId: props.anchorId ?? 'HEADER',
      initialDisplayLabel: props.initialDisplayLabel ?? 'Header block',
      loading: props.loading ?? false,
    },
    global: {
      plugins: [i18n, ElementPlus],
      stubs: {
        ElDialog: {
          template: '<div class="dialog-stub"><slot /><footer><slot name="footer" /></footer></div>',
        },
      },
    },
  })
}

describe('MasterAnchorDisplayLabelDialog', () => {
  it('BDD-CE-U06-MAC-003 — seeds current displayLabel on first open (mount with open=true)', () => {
    const wrapper = mountDialog({
      modelValue: true,
      initialDisplayLabel: 'Header block',
    })

    const input = wrapper.get('[data-testid="master-anchor-display-label-input"]')
    expect((input.element as HTMLInputElement).value).toBe('Header block')
    expect(wrapper.find('[data-testid="master-anchor-display-label-error"]').exists()).toBe(false)
    expect(
      wrapper.get('[data-testid="master-anchor-display-label-save"]').attributes('disabled'),
    ).toBeUndefined()
  })

  it('BDD-CE-U06-MAC-008 — rejects blank displayLabel (English-first)', async () => {
    const wrapper = mountDialog({ initialDisplayLabel: 'Header block' })
    await wrapper.get('[data-testid="master-anchor-display-label-input"]').setValue('   ')

    expect(wrapper.get('[data-testid="master-anchor-display-label-error"]').text()).toBe(
      'Display label is required.',
    )
    expect(
      wrapper.get('[data-testid="master-anchor-display-label-save"]').attributes('disabled'),
    ).toBeDefined()
  })

  it('emits trimmed displayLabel on save', async () => {
    const wrapper = mountDialog({ initialDisplayLabel: 'Old' })
    await wrapper.get('[data-testid="master-anchor-display-label-input"]').setValue('  New label  ')
    await wrapper.get('[data-testid="master-anchor-display-label-save"]').trigger('click')

    expect(wrapper.emitted('submit')?.[0]).toEqual([{ displayLabel: 'New label' }])
  })
})
