import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { describe, expect, it } from 'vitest'
import LocaleVariantFamilyNav from '@/components/common/LocaleVariantFamilyNav.vue'
import en from '@/i18n/locales/en'

describe('LocaleVariantFamilyNav (IBL-E1 / BDD-IBL-E1-015)', () => {
  function mountNav(
    props: Partial<{
      currentLocale: string
      siblings: Array<{
        id: string
        code: string
        name: string
        locale: string
        lifecycleLabel?: string | null
      }>
      loading: boolean
    }> = {},
  ) {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    return mount(LocaleVariantFamilyNav, {
      props: {
        currentLocale: props.currentLocale ?? 'en-US',
        siblings: props.siblings ?? [],
        loading: props.loading ?? false,
        siblingLink: (id: string) => `/templates/${id}`,
      },
      global: {
        plugins: [i18n, ElementPlus],
        stubs: {
          RouterLink: {
            props: ['to'],
            template: '<a class="router-link-stub" :href="to"><slot /></a>',
          },
        },
      },
    })
  }

  it('renders current locale and sibling navigation targets', () => {
    const wrapper = mountNav({
      siblings: [
        {
          id: 'tpl-zh',
          code: 'TPL-LETTER-ZH',
          name: 'Letter ZH',
          locale: 'zh-CN',
        },
      ],
    })

    expect(wrapper.find('[data-testid="locale-variant-family-nav"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="locale-variant-current"]').text()).toContain('en-US')
    expect(wrapper.text()).toContain('Letter ZH')
    expect(wrapper.text()).toContain('zh-CN')
    expect(wrapper.findAll('[data-testid="locale-variant-sibling"]')).toHaveLength(1)
  })

  it('shows empty siblings copy when family has no other authorized variants', () => {
    const wrapper = mountNav({ siblings: [] })
    expect(wrapper.find('[data-testid="locale-variant-no-siblings"]').exists()).toBe(true)
  })
})
