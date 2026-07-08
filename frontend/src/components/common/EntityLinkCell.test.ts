import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { describe, expect, it } from 'vitest'
import EntityLinkCell from '@/components/common/EntityLinkCell.vue'

describe('EntityLinkCell', () => {
  it('renders a router link with subtitle when to is provided', () => {
    const router = createRouter({
      history: createWebHistory(),
      routes: [{ path: '/templates/:id', name: 'template', component: { template: '<div />' } }],
    })

    const wrapper = mount(EntityLinkCell, {
      props: {
        label: 'Loan agreement',
        subtitle: 'TPL-001',
        to: '/templates/tpl-1',
      },
      global: {
        plugins: [router],
      },
    })

    expect(wrapper.find('.entity-link-cell__link').exists()).toBe(true)
    expect(wrapper.find('.entity-link-cell__link').text()).toBe('Loan agreement')
    expect(wrapper.find('.entity-link-cell__subtitle').text()).toBe('TPL-001')
  })

  it('renders plain text when disabled or to is missing', () => {
    const wrapper = mount(EntityLinkCell, {
      props: {
        label: 'Loan agreement',
        subtitle: 'TPL-001',
        disabled: true,
        to: '/templates/tpl-1',
      },
    })

    expect(wrapper.find('.entity-link-cell__link').exists()).toBe(false)
    expect(wrapper.find('.entity-link-cell__text').text()).toBe('Loan agreement')
  })
})
