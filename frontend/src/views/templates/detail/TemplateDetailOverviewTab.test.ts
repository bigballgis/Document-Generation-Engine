import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus, { ElMessageBox } from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import TemplateDetailOverviewTab from '@/views/templates/detail/TemplateDetailOverviewTab.vue'
import en from '@/i18n/locales/en'
import * as mastersApi from '@/api/masters'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import { useSessionStore } from '@/stores/session'
import { useTemplatesStore } from '@/stores/templates'
import type { TemplateDetail } from '@/types/template'

vi.mock('@/api/masters', () => ({
  getMaster: vi.fn(),
}))

vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>()
  return {
    ...actual,
    ElMessageBox: {
      confirm: vi.fn(),
    },
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
    },
  }
})

describe('TemplateDetailOverviewTab', () => {
  const template: TemplateDetail = {
    id: 'tpl-1',
    externalId: 'TPL-001',
    groupCode: 'RETAIL',
    name: 'Loan agreement',
    description: 'Retail loan pack',
    masterId: 'master-1',
    lifecycleStatus: 'PUBLISHED',
    releaseVersion: '1.0.0',
    nextReviewDue: '2026-07-17',
    devVersionId: 'dev-1',
    devVersionNumber: 1,
    createdAt: '2026-06-23T09:00:00Z',
    updatedAt: '2026-06-23T10:00:00Z',
    variables: [],
    bindings: [],
    rules: [],
  }

  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(mastersApi.getMaster).mockResolvedValue({
      id: 'master-1',
      groupCode: 'RETAIL',
      name: 'Loan Offer Base',
      description: null,
      status: 'APPROVED',
      originalFilename: 'master.docx',
      changeSummary: null,
      anchors: [],
      reviewHistory: [],
      createdBy: 'admin',
      updatedBy: 'admin',
      createdAt: '2026-06-23T08:00:00Z',
      updatedAt: '2026-06-23T09:00:00Z',
    })
    vi.mocked(ElMessageBox.confirm).mockReset()
    vi.mocked(ElMessageBox.confirm).mockResolvedValue('confirm' as never)
  })

  function mountTab(overrides: Partial<TemplateDetail> = {}, roles: string[] = ['GLOBAL_ADMIN']) {
    const pinia = createPinia()
    setActivePinia(pinia)
    const sessionStore = useSessionStore()
    sessionStore.$patch({
      accessToken: 'token',
      session: {
        username: 'admin',
        displayName: 'Admin',
        email: 'admin@example.com',
        authSource: 'LOCAL',
        roles,
        authorizedGroupCodes: ['*'],
        defaultRoute: ROUTE_KEYS.templateManagement,
        visibleRoutes: [ROUTE_KEYS.templateManagement, ROUTE_KEYS.masterManagement],
        expiresAt: new Date().toISOString(),
        capabilities:
          roles.includes('DOCUMENT_AUTHOR') || roles.includes('GLOBAL_ADMIN')
            ? ({ authorTemplates: true } as never)
            : ({ authorTemplates: false } as never),
      },
    })

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    return mount(TemplateDetailOverviewTab, {
      props: {
        template: { ...template, ...overrides },
        formatDateTime: (value: string) => value,
      },
      global: {
        plugins: [pinia, i18n, ElementPlus],
        stubs: {
          RouterLink: {
            props: ['to'],
            template: '<a class="router-link-stub"><slot /></a>',
          },
        },
      },
    })
  }

  it('renders template summary fields', async () => {
    const wrapper = mountTab()
    await flushPromises()

    expect(wrapper.text()).toContain('TPL-001')
    expect(wrapper.text()).toContain('Loan Offer Base')
    expect(wrapper.text()).not.toContain('master-1')
    expect(wrapper.text()).toContain('Retail loan pack')
    expect(mastersApi.getMaster).toHaveBeenCalledWith('master-1')
  })

  it('falls back to master id when master lookup fails', async () => {
    vi.mocked(mastersApi.getMaster).mockRejectedValue(new Error('not found'))

    const wrapper = mountTab()
    await flushPromises()

    expect(wrapper.text()).toContain('master-1')
  })

  it('CE-G05: shows nextReviewDue and complete control for authors', async () => {
    const wrapper = mountTab({}, ['DOCUMENT_AUTHOR'])
    await flushPromises()

    expect(wrapper.find('[data-testid="template-annual-review-due-value"]').text()).toBe(
      '2026-07-17',
    )
    expect(wrapper.find('[data-testid="template-annual-review-complete"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="template-annual-review-complete"]').text()).toContain(
      'Complete annual review',
    )
  })

  it('CE-G05: shows unset label when nextReviewDue is null', async () => {
    const wrapper = mountTab({ nextReviewDue: null }, ['DOCUMENT_AUTHOR'])
    await flushPromises()

    expect(wrapper.find('[data-testid="template-annual-review-due-value"]').text()).toBe(
      'Not scheduled',
    )
  })

  it('CE-G05: completes annual review after confirmation', async () => {
    const wrapper = mountTab({}, ['DOCUMENT_AUTHOR'])
    await flushPromises()

    const templatesStore = useTemplatesStore()
    const completeSpy = vi
      .spyOn(templatesStore, 'completeAnnualReview')
      .mockResolvedValue({
        id: 'tpl-1',
        externalId: 'TPL-001',
        groupCode: 'RETAIL',
        name: 'Loan agreement',
        lifecycleStatus: 'PUBLISHED',
        releaseVersion: '1.0.0',
        releaseVersionCount: 1,
        masterId: 'master-1',
        updatedBy: 'admin',
        updatedAt: '2026-07-17T12:00:00Z',
        nextReviewDue: '2027-07-17',
      })

    await wrapper.get('[data-testid="template-annual-review-complete"]').trigger('click')
    await flushPromises()

    expect(ElMessageBox.confirm).toHaveBeenCalled()
    expect(completeSpy).toHaveBeenCalledWith('tpl-1')
  })

  it('hard-retires document brand allow-list editor (BDD-SYS-NORM-D1-007/014)', async () => {
    const wrapper = mountTab(
      {
        lifecycleStatus: 'DRAFT',
        allowedDocumentBrandCodes: ['PLATFORM_DEFAULT'],
      },
      ['DOCUMENT_AUTHOR'],
    )
    await flushPromises()

    expect(wrapper.find('[data-testid="template-overview-document-brand-allow-list"]').exists()).toBe(
      false,
    )
    expect(
      wrapper.find('[data-testid="template-overview-document-brand-allow-list-edit"]').exists(),
    ).toBe(false)
    expect(wrapper.find('[data-testid="template-document-brand-allow-list"]').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('Allowed document brands')
  })
})
