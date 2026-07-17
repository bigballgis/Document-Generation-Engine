import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import TemplatePackageHubView from '@/views/templates/TemplatePackageHubView.vue'
import TemplateAuthorJourneyBlock from '@/components/journey/TemplateAuthorJourneyBlock.vue'
import TemplateTesterJourneyBlock from '@/components/journey/TemplateTesterJourneyBlock.vue'
import TemplateApproverJourneyBlock from '@/components/journey/TemplateApproverJourneyBlock.vue'
import TemplateTeamLeadJourneyBlock from '@/components/journey/TemplateTeamLeadJourneyBlock.vue'
import TemplateWorkflowBanner from '@/components/templates/TemplateWorkflowBanner.vue'
import en from '@/i18n/locales/en'
import * as templatesApi from '@/api/templates'
import { useSessionStore } from '@/stores/session'

vi.mock('@/api/templates', () => ({
  getTemplate: vi.fn(),
  listTemplateVersionLines: vi.fn(),
}))

const routerPush = vi.fn()
const routerReplace = vi.fn()

vi.mock('vue-router', () => ({
  useRoute: () => ({
    params: { templateId: 'tpl-1' },
    query: {},
  }),
  useRouter: () => ({ push: routerPush, replace: routerReplace }),
}))

describe('TemplatePackageHubView', () => {
  let pinia: ReturnType<typeof createPinia>

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    const sessionStore = useSessionStore()
    sessionStore.session = {
      roles: ['TEMPLATE_AUTHOR'],
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: ['route.template-management'],
      capabilities: { authorTemplates: true },
    } as never
    routerPush.mockReset()
    routerReplace.mockReset()
    vi.mocked(templatesApi.getTemplate).mockReset()
    vi.mocked(templatesApi.listTemplateVersionLines).mockReset()
  })

  it('renders package header and version lines panel as primary content', async () => {
    vi.mocked(templatesApi.getTemplate).mockResolvedValue({
      id: 'tpl-1',
      externalId: 'TPL-RETAIL-LETTER',
      groupCode: 'RETAIL',
      name: 'Retail letter template',
      description: 'Demo template',
      masterId: 'master-1',
      lifecycleStatus: 'DRAFT',
      releaseVersion: null,
      devVersionId: 'dev-1',
      devVersionNumber: 1,
      bindings: [],
      variables: [],
      rules: [],
      createdAt: '2026-06-23T10:00:00Z',
      updatedAt: '2026-06-23T10:00:00Z',
    })
    vi.mocked(templatesApi.listTemplateVersionLines).mockResolvedValue({
      content: [
        {
          devVersionId: 'dev-1',
          devVersionNumber: 1,
          releaseVersion: null,
          lifecycleStatus: 'DRAFT',
          lineKind: 'IN_FLIGHT',
          updatedAt: '2026-06-23T10:00:00Z',
          updatedBy: '10000003',
          defaultRouteTarget: null,
          cloneable: false,
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })

    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })

    const wrapper = mount(TemplatePackageHubView, {
      global: {
        plugins: [pinia, i18n, ElementPlus],
      },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('Retail letter template')
    expect(wrapper.text()).toContain('Version lines')
    expect(wrapper.text()).toContain('Dev version 1')
    expect(wrapper.text()).toContain('Dependencies')
  })

  it('does not render journey blocks or workflow banner', async () => {
    vi.mocked(templatesApi.getTemplate).mockResolvedValue({
      id: 'tpl-1',
      externalId: 'TPL-RETAIL-LETTER',
      groupCode: 'RETAIL',
      name: 'Retail letter template',
      description: 'Demo template',
      masterId: 'master-1',
      lifecycleStatus: 'TESTING',
      approvalSubState: 'PENDING_DECISION',
      releaseVersion: null,
      devVersionId: 'dev-1',
      devVersionNumber: 1,
      bindings: [],
      variables: [],
      rules: [],
      createdAt: '2026-06-23T10:00:00Z',
      updatedAt: '2026-06-23T10:00:00Z',
    })
    vi.mocked(templatesApi.listTemplateVersionLines).mockResolvedValue({
      content: [
        {
          devVersionId: 'dev-1',
          devVersionNumber: 1,
          releaseVersion: null,
          lifecycleStatus: 'TESTING',
          lineKind: 'IN_FLIGHT',
          updatedAt: '2026-06-23T10:00:00Z',
          updatedBy: '10000003',
          defaultRouteTarget: null,
          cloneable: false,
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })

    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })

    const wrapper = mount(TemplatePackageHubView, {
      global: {
        plugins: [pinia, i18n, ElementPlus],
      },
    })

    await flushPromises()

    expect(wrapper.findComponent(TemplateAuthorJourneyBlock).exists()).toBe(false)
    expect(wrapper.findComponent(TemplateTesterJourneyBlock).exists()).toBe(false)
    expect(wrapper.findComponent(TemplateApproverJourneyBlock).exists()).toBe(false)
    expect(wrapper.findComponent(TemplateTeamLeadJourneyBlock).exists()).toBe(false)
    expect(wrapper.findComponent(TemplateWorkflowBanner).exists()).toBe(false)
  })
})
