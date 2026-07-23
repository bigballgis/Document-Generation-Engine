import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import MasterImpactPanel from '@/components/masters/MasterImpactPanel.vue'
import en from '@/i18n/locales/en'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import { useSessionStore } from '@/stores/session'
import type { MasterImpactAnalysis } from '@/types/master'

describe('MasterImpactPanel', () => {
  let pinia: ReturnType<typeof createPinia>

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    patchSession([ROUTE_KEYS.templateManagement, ROUTE_KEYS.masterManagement])
  })

  function patchSession(visibleRoutes: string[]) {
    const sessionStore = useSessionStore()
    sessionStore.$patch({
      accessToken: 'token',
      session: {
        username: 'designer',
        displayName: 'Designer',
        email: 'designer@example.com',
        authSource: 'LOCAL',
        roles: ['DOCUMENT_AUTHOR'],
        authorizedGroupCodes: ['RETAIL'],
        defaultRoute: ROUTE_KEYS.dashboardHome,
        visibleRoutes,
        expiresAt: '2099-01-01T00:00:00Z',
      },
    })
  }

  function mountPanel(impact: MasterImpactAnalysis | null) {
    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })
    return mount(MasterImpactPanel, {
      props: { impact },
      global: {
        plugins: [pinia, i18n, ElementPlus],
        stubs: {
          RouterLink: {
            props: ['to'],
            template:
              '<a class="router-link-stub" :href="typeof to === \'string\' ? to : to?.path" data-testid="master-impact-template-link"><slot /></a>',
          },
        },
      },
    })
  }

  it('MIR-009 — renders name links and hides empty state when referencedTemplates present', async () => {
    const wrapper = mountPanel({
      masterId: 'master-1',
      referencedTemplateIds: ['tpl-1', 'tpl-2'],
      referencedTemplates: [
        { templateId: 'tpl-1', name: 'Loan Contract', lifecycleStatus: 'DRAFT' },
        { templateId: 'tpl-2', name: 'Credit Notice', lifecycleStatus: 'PUBLISHED' },
      ],
      retestRequired: true,
    })
    await flushPromises()

    expect(wrapper.find('[data-testid="master-impact-empty"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('Loan Contract')
    expect(wrapper.text()).toContain('Credit Notice')
    expect(wrapper.text()).not.toMatch(/No templates currently reference/i)
    const links = wrapper.findAll('[data-testid="master-impact-template-link"]')
    expect(links).toHaveLength(2)
    expect(links[0].text()).toBe('Loan Contract')
    expect(links[0].attributes('href')).toContain('/templates/tpl-1')
  })

  it('shows honest empty state only when API returns no references', async () => {
    const wrapper = mountPanel({
      masterId: 'master-1',
      referencedTemplateIds: [],
      referencedTemplates: [],
      retestRequired: false,
    })
    await flushPromises()

    expect(wrapper.find('[data-testid="master-impact-empty"]').exists()).toBe(true)
    expect(wrapper.text()).toMatch(/No templates currently reference/i)
  })

  describe('PQH N20 EntityLink (BDD-PQH-N19N20-008…011)', () => {
    it('BDD-PQH-N19N20-008 — referenced templates use EntityLinkCell', async () => {
      const wrapper = mountPanel({
        masterId: 'master-1',
        referencedTemplateIds: ['tpl-1'],
        referencedTemplates: [
          { templateId: 'tpl-1', name: 'Loan Contract', lifecycleStatus: 'DRAFT' },
        ],
        retestRequired: true,
      })
      await flushPromises()

      const cells = wrapper.findAll('[data-testid="master-impact-template-cell"]')
      expect(cells).toHaveLength(1)
      expect(cells[0].classes()).toContain('entity-link-cell')
      // No bare ungated router-link as the sole link primitive outside EntityLinkCell
      expect(wrapper.find('li > .router-link-stub').exists()).toBe(false)
      expect(wrapper.find('li > a[data-testid="master-impact-template-link"]').exists()).toBe(
        false,
      )
    })

    it('BDD-PQH-N19N20-009 — label prefers name; optional externalId subtitle', async () => {
      const wrapper = mountPanel({
        masterId: 'master-1',
        referencedTemplateIds: ['tpl-1'],
        referencedTemplates: [
          {
            templateId: 'tpl-1',
            name: 'Loan Contract',
            externalId: 'TPL-1',
            lifecycleStatus: 'DRAFT',
          },
        ],
        retestRequired: false,
      })
      await flushPromises()

      const cell = wrapper.find('[data-testid="master-impact-template-cell"]')
      expect(cell.find('.entity-link-cell__link, .entity-link-cell__text').text()).toBe(
        'Loan Contract',
      )
      expect(cell.find('.entity-link-cell__subtitle').text()).toBe('TPL-1')
    })

    it('BDD-PQH-N19N20-010 — link gated on template management', async () => {
      const permitted = mountPanel({
        masterId: 'master-1',
        referencedTemplateIds: ['tpl-1'],
        referencedTemplates: [
          { templateId: 'tpl-1', name: 'Loan Contract', lifecycleStatus: 'DRAFT' },
        ],
        retestRequired: false,
      })
      await flushPromises()

      const permittedCell = permitted.find('[data-testid="master-impact-template-cell"]')
      const link = permittedCell.find('.entity-link-cell__link, .router-link-stub')
      expect(link.exists()).toBe(true)
      expect(link.attributes('href')).toContain('/templates/tpl-1')

      patchSession([ROUTE_KEYS.masterManagement])
      const denied = mountPanel({
        masterId: 'master-1',
        referencedTemplateIds: ['tpl-1'],
        referencedTemplates: [
          { templateId: 'tpl-1', name: 'Loan Contract', lifecycleStatus: 'DRAFT' },
        ],
        retestRequired: false,
      })
      await flushPromises()

      const deniedCell = denied.find('[data-testid="master-impact-template-cell"]')
      expect(deniedCell.find('.entity-link-cell__link').exists()).toBe(false)
      expect(deniedCell.find('.entity-link-cell__text').text()).toBe('Loan Contract')
      expect(denied.findAll('[data-testid="master-impact-template-link"]')).toHaveLength(0)
    })

    it('BDD-PQH-N19N20-011 — ids-only fallback remains EntityLink + gated', async () => {
      const wrapper = mountPanel({
        masterId: 'master-1',
        referencedTemplateIds: ['tpl-1'],
        referencedTemplates: [],
        retestRequired: false,
      })
      await flushPromises()

      const cell = wrapper.find('[data-testid="master-impact-template-cell"]')
      expect(cell.exists()).toBe(true)
      expect(cell.classes()).toContain('entity-link-cell')
      expect(cell.find('.entity-link-cell__link, .entity-link-cell__text').text()).toBe('tpl-1')
      expect(cell.find('.entity-link-cell__link, .router-link-stub').attributes('href')).toContain(
        '/templates/tpl-1',
      )

      patchSession([ROUTE_KEYS.masterManagement])
      const denied = mountPanel({
        masterId: 'master-1',
        referencedTemplateIds: ['tpl-1'],
        referencedTemplates: [],
        retestRequired: false,
      })
      await flushPromises()

      const deniedCell = denied.find('[data-testid="master-impact-template-cell"]')
      expect(deniedCell.find('.entity-link-cell__link').exists()).toBe(false)
      expect(deniedCell.find('.entity-link-cell__text').text()).toBe('tpl-1')
    })
  })
})
