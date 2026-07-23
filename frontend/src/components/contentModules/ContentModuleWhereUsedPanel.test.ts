import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ContentModuleWhereUsedPanel from '@/components/contentModules/ContentModuleWhereUsedPanel.vue'
import en from '@/i18n/locales/en'
import * as contentModulesApi from '@/api/contentModules'
import { GROUPS_CATALOG_PATH } from '@/composables/useEntityLinkTargets'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import { useSessionStore } from '@/stores/session'
import type { ContentModuleWhereUsedTemplate } from '@/types/contentModule'

vi.mock('@/api/contentModules', () => ({
  listContentModuleWhereUsed: vi.fn(),
}))

const SAMPLE_ROW: ContentModuleWhereUsedTemplate = {
  id: 'tpl-1',
  externalId: 'TPL-1',
  name: 'Loan Notice',
  groupCode: 'RETAIL',
  lifecycleStatus: 'PUBLISHED',
  pinnedSemanticVersion: '1.0.0',
}

describe('ContentModuleWhereUsedPanel', () => {
  let pinia: ReturnType<typeof createPinia>

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    vi.mocked(contentModulesApi.listContentModuleWhereUsed).mockReset()
    patchSession([ROUTE_KEYS.templateManagement, ROUTE_KEYS.contentModuleManagement])
  })

  function patchSession(visibleRoutes: string[], roles: string[] = ['DOCUMENT_AUTHOR']) {
    const sessionStore = useSessionStore()
    sessionStore.$patch({
      accessToken: 'token',
      session: {
        username: 'author',
        displayName: 'Author',
        email: 'author@example.com',
        authSource: 'LOCAL',
        roles,
        authorizedGroupCodes: ['RETAIL'],
        defaultRoute: ROUTE_KEYS.dashboardHome,
        visibleRoutes,
        expiresAt: '2099-01-01T00:00:00Z',
      },
    })
  }

  function mountPanel() {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    return mount(ContentModuleWhereUsedPanel, {
      props: { moduleId: 'MOD-1' },
      global: {
        plugins: [pinia, i18n, ElementPlus],
        stubs: {
          RouterLink: {
            props: ['to'],
            template:
              '<a class="router-link-stub" :href="typeof to === \'string\' ? to : to?.path"><slot /></a>',
          },
        },
      },
    })
  }

  async function mountWithRows(rows: ContentModuleWhereUsedTemplate[]) {
    vi.mocked(contentModulesApi.listContentModuleWhereUsed).mockResolvedValue({
      content: rows,
      page: 0,
      size: 20,
      totalElements: rows.length,
      totalPages: rows.length > 0 ? 1 : 0,
    })
    const wrapper = mountPanel()
    await flushPromises()
    return wrapper
  }

  it('renders where-used templates with entity links', async () => {
    const wrapper = await mountWithRows([SAMPLE_ROW])

    expect(contentModulesApi.listContentModuleWhereUsed).toHaveBeenCalledWith('MOD-1', 0, 20)
    expect(wrapper.find('[data-testid="content-module-where-used-table"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Loan Notice')
    expect(wrapper.text()).toContain('1.0.0')
  })

  it('shows empty state when no templates reference the module', async () => {
    const wrapper = await mountWithRows([])

    expect(wrapper.find('[data-testid="content-module-where-used-empty"]').exists()).toBe(true)
  })

  describe('PQH N19 EntityLink (BDD-PQH-N19N20-001…007)', () => {
    it('BDD-PQH-N19N20-001 — template name uses EntityLinkCell', async () => {
      const wrapper = await mountWithRows([SAMPLE_ROW])
      const cell = wrapper.find('[data-testid="where-used-template-name"]')

      expect(cell.exists()).toBe(true)
      expect(cell.classes()).toContain('entity-link-cell')
    })

    it('BDD-PQH-N19N20-002 — template label + externalId subtitle', async () => {
      const wrapper = await mountWithRows([SAMPLE_ROW])
      const cell = wrapper.find('[data-testid="where-used-template-name"]')

      expect(cell.find('.entity-link-cell__link, .entity-link-cell__text').text()).toBe(
        'Loan Notice',
      )
      expect(cell.find('.entity-link-cell__subtitle').text()).toBe('TPL-1')
    })

    it('BDD-PQH-N19N20-003 — template link when template management permitted', async () => {
      const wrapper = await mountWithRows([SAMPLE_ROW])
      const cell = wrapper.find('[data-testid="where-used-template-name"]')
      const link = cell.find('.entity-link-cell__link, .router-link-stub')

      expect(link.exists()).toBe(true)
      expect(link.attributes('href')).toContain('/templates/tpl-1')
    })

    it('BDD-PQH-N19N20-004 — template plain text when template management denied', async () => {
      patchSession([ROUTE_KEYS.contentModuleManagement])
      const wrapper = await mountWithRows([SAMPLE_ROW])
      const cell = wrapper.find('[data-testid="where-used-template-name"]')

      expect(cell.find('.entity-link-cell__link').exists()).toBe(false)
      expect(cell.find('.entity-link-cell__text').text()).toBe('Loan Notice')
      expect(cell.find('.entity-link-cell__subtitle').text()).toBe('TPL-1')
    })

    it('BDD-PQH-N19N20-005 — groupCode uses EntityLinkCell', async () => {
      const wrapper = await mountWithRows([SAMPLE_ROW])
      const cell = wrapper.find('[data-testid="where-used-group-code"]')

      expect(cell.exists()).toBe(true)
      expect(cell.classes()).toContain('entity-link-cell')
      expect(cell.find('.entity-link-cell__link, .entity-link-cell__text').text()).toBe('RETAIL')
    })

    it('BDD-PQH-N19N20-006 — groupCode link when identity administration permitted', async () => {
      patchSession(
        [
          ROUTE_KEYS.templateManagement,
          ROUTE_KEYS.contentModuleManagement,
          ROUTE_KEYS.identityAdministration,
        ],
        ['GROUP_ADMIN'],
      )
      const wrapper = await mountWithRows([SAMPLE_ROW])
      const cell = wrapper.find('[data-testid="where-used-group-code"]')
      const link = cell.find('.entity-link-cell__link, .router-link-stub')

      expect(link.exists()).toBe(true)
      expect(link.attributes('href')).toBe(GROUPS_CATALOG_PATH)
    })

    it('BDD-PQH-N19N20-007 — groupCode plain text when denied or wildcard', async () => {
      patchSession([ROUTE_KEYS.templateManagement, ROUTE_KEYS.contentModuleManagement])
      const deniedWrapper = await mountWithRows([SAMPLE_ROW])
      const deniedCell = deniedWrapper.find('[data-testid="where-used-group-code"]')

      expect(deniedCell.find('.entity-link-cell__link').exists()).toBe(false)
      expect(deniedCell.find('.entity-link-cell__text').text()).toBe('RETAIL')

      patchSession(
        [
          ROUTE_KEYS.templateManagement,
          ROUTE_KEYS.contentModuleManagement,
          ROUTE_KEYS.identityAdministration,
        ],
        ['GROUP_ADMIN'],
      )
      const wildcardWrapper = await mountWithRows([{ ...SAMPLE_ROW, groupCode: '*' }])
      const wildcardCell = wildcardWrapper.find('[data-testid="where-used-group-code"]')

      expect(wildcardCell.find('.entity-link-cell__link').exists()).toBe(false)
      expect(wildcardCell.find('.entity-link-cell__text').text()).toBe('*')
    })
  })
})
