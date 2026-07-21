import { flushPromises, mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import TemplateDependenciesPanel from '@/views/templates/detail/TemplateDependenciesPanel.vue'
import en from '@/i18n/locales/en'
import * as templatesApi from '@/api/templates'
import * as mastersApi from '@/api/masters'
import type { TemplateDetail } from '@/types/template'
import { useSessionStore } from '@/stores/session'

vi.mock('@/api/templates', () => ({
  listTemplateVersionLines: vi.fn(),
  fetchReleaseVersionDetail: vi.fn(),
  listTemplateContentModuleReferences: vi.fn(),
}))

vi.mock('@/api/masters', () => ({
  getMaster: vi.fn(),
  listMasterRevisionLines: vi.fn(),
}))

const routerPush = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush }),
  useRoute: () => ({ params: {}, query: {} }),
}))

function baseTemplate(overrides: Partial<TemplateDetail> = {}): TemplateDetail {
  return {
    id: 'tpl-1',
    externalId: 'TPL-1',
    groupCode: 'RETAIL',
    name: 'Retail letter',
    description: null,
    masterId: 'master-1',
    lifecycleStatus: 'DRAFT',
    releaseVersion: null,
    devVersionId: 'dev-1',
    devVersionNumber: 1,
    variables: [],
    bindings: [],
    rules: [],
    createdAt: '2026-07-17T00:00:00Z',
    updatedAt: '2026-07-17T00:00:00Z',
    ...overrides,
  }
}

describe('TemplateDependenciesPanel', () => {
  let pinia: ReturnType<typeof createPinia>

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    const sessionStore = useSessionStore()
    sessionStore.session = {
      roles: ['DOCUMENT_AUTHOR'],
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: ['route.template-management', 'route.master-management'],
      capabilities: { authorTemplates: true, manageMasters: true },
    } as never
    routerPush.mockReset()
    vi.mocked(templatesApi.listTemplateVersionLines).mockReset()
    vi.mocked(templatesApi.fetchReleaseVersionDetail).mockReset()
    vi.mocked(templatesApi.listTemplateContentModuleReferences).mockReset()
    vi.mocked(mastersApi.getMaster).mockReset()
    vi.mocked(mastersApi.listMasterRevisionLines).mockReset()

    vi.mocked(mastersApi.getMaster).mockResolvedValue({
      id: 'master-1',
      name: 'Corporate letterhead',
    } as never)
    vi.mocked(mastersApi.listMasterRevisionLines).mockResolvedValue({
      content: [
        {
          id: 'rev-work-1',
          lineLabel: 'CURRENT',
          status: 'APPROVED',
          originalFilename: 'master.docx',
          anchorCount: 1,
          updatedAt: '2026-07-17T00:00:00Z',
          updatedBy: '10000001',
          current: true,
          revisionSequence: 3,
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })
    vi.mocked(templatesApi.listTemplateContentModuleReferences).mockResolvedValue([])
    vi.mocked(templatesApi.listTemplateVersionLines).mockResolvedValue({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    })
  })

  function mountPanel(template: TemplateDetail = baseTemplate()) {
    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    return mount(TemplateDependenciesPanel, {
      props: { template },
      global: {
        plugins: [pinia, i18n, ElementPlus],
        stubs: {
          RouterLink: { template: '<a><slot /></a>' },
        },
      },
    })
  }

  it('BDD-CE-U19-DRV-001 — renders dependencies panel root testid', async () => {
    const wrapper = mountPanel()
    await flushPromises()
    expect(wrapper.find('[data-testid="template-dependencies-panel"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="template-dependencies-master-section"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="template-dependencies-anchors-section"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="template-dependencies-clauses-section"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="template-dependencies-release-lines-section"]').exists()).toBe(
      true,
    )
  })

  it('BDD-CE-U19-DRV-002 — shows pinned master revision from release detail', async () => {
    vi.mocked(templatesApi.listTemplateVersionLines).mockResolvedValue({
      content: [
        {
          devVersionId: 'dev-1',
          devVersionNumber: 1,
          releaseVersion: '1.0.0',
          lifecycleStatus: 'PUBLISHED',
          lineKind: 'PUBLISHED',
          updatedAt: '2026-07-17T00:00:00Z',
          updatedBy: '10000001',
          defaultRouteTarget: true,
          cloneable: true,
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })
    vi.mocked(templatesApi.fetchReleaseVersionDetail).mockResolvedValue(
      baseTemplate({
        releaseVersion: '1.0.0',
        lifecycleStatus: 'PUBLISHED',
        readOnly: true,
        masterPin: {
          masterRevisionId: 'rev-pinned-1',
          masterFileHash: 'abcdef0123456789ffffeeee',
          revisionSequence: 2,
          pinOrigin: 'PUBLISHED',
        },
      }),
    )

    const wrapper = mountPanel()
    await flushPromises()

    expect(templatesApi.fetchReleaseVersionDetail).toHaveBeenCalledWith('tpl-1', '1.0.0')
    expect(wrapper.find('[data-testid="template-dependencies-pinned"]').text()).toContain(
      'Pinned at publish',
    )
    expect(wrapper.find('[data-testid="template-dependencies-pin-revision-id"]').text()).toBe(
      'rev-pinned-1',
    )
    expect(wrapper.find('[data-testid="template-dependencies-pin-hash"]').text()).toContain('abcdef012345')
    expect(wrapper.find('[data-testid="template-dependencies-not-pinned"]').exists()).toBe(false)
  })

  it('BDD-CE-U19-DRV-003 — shows not pinned until publish for in-flight only', async () => {
    vi.mocked(templatesApi.listTemplateVersionLines).mockResolvedValue({
      content: [
        {
          devVersionId: 'dev-1',
          devVersionNumber: 1,
          releaseVersion: null,
          lifecycleStatus: 'DRAFT',
          lineKind: 'IN_FLIGHT',
          updatedAt: '2026-07-17T00:00:00Z',
          updatedBy: '10000001',
          defaultRouteTarget: null,
          cloneable: false,
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })

    const wrapper = mountPanel()
    await flushPromises()

    expect(templatesApi.fetchReleaseVersionDetail).not.toHaveBeenCalled()
    expect(wrapper.find('[data-testid="template-dependencies-not-pinned"]').text()).toContain(
      'Not pinned until publish',
    )
    expect(wrapper.find('[data-testid="template-dependencies-working-revision"]').text()).toContain(
      'not pinned',
    )
    expect(wrapper.find('[data-testid="template-dependencies-pinned"]').exists()).toBe(false)
  })

  it('BDD-CE-U19-DRV-004 — lists anchor bindings without write controls', async () => {
    const wrapper = mountPanel(
      baseTemplate({
        bindings: [
          {
            anchorId: 'HEADER',
            declaredContentType: 'RICH_TEXT',
            structuredContentJson: '{}',
            updatedAt: '2026-07-17T00:00:00Z',
          },
        ],
      }),
    )
    await flushPromises()

    const table = wrapper.find('[data-testid="template-dependencies-anchors-table"]')
    expect(table.exists()).toBe(true)
    expect(table.text()).toContain('HEADER')
    expect(table.text()).toContain('RICH_TEXT')
    expect(wrapper.text()).not.toContain('Save')
    expect(wrapper.find('[data-version-line-create-from-latest]').exists()).toBe(false)
  })

  it('BDD-CE-U19-DRV-005 — shows honest empty state for anchors', async () => {
    const wrapper = mountPanel(baseTemplate({ bindings: [] }))
    await flushPromises()
    expect(wrapper.find('[data-testid="template-dependencies-anchors-empty"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="template-dependencies-anchors-table"]').exists()).toBe(false)
  })

  it('BDD-CE-U19-DRV-006 — lists clause versions as read-only', async () => {
    vi.mocked(templatesApi.listTemplateContentModuleReferences).mockResolvedValue([
      {
        referenceKey: 'LOAN_DISCLOSURE',
        moduleId: 'mod-1',
        semanticVersion: '1.2.0',
        locked: true,
        outOfDate: true,
        latestApprovedSemanticVersion: '1.3.0',
      },
    ])

    const wrapper = mountPanel()
    await flushPromises()

    const table = wrapper.find('[data-testid="template-dependencies-clauses-table"]')
    expect(table.text()).toContain('LOAN_DISCLOSURE')
    expect(table.text()).toContain('1.2.0')
    expect(wrapper.find('[data-testid="template-dependencies-clause-outdated"]').text()).toContain(
      '1.3.0',
    )
    expect(wrapper.text()).not.toContain('Add reference')
    expect(wrapper.text()).not.toContain('Save reference')
  })

  it('BDD-CE-U19-DRV-007 — release lines navigate and omit write CTAs', async () => {
    vi.mocked(templatesApi.listTemplateVersionLines).mockResolvedValue({
      content: [
        {
          devVersionId: 'dev-2',
          devVersionNumber: 2,
          releaseVersion: null,
          lifecycleStatus: 'DRAFT',
          lineKind: 'IN_FLIGHT',
          updatedAt: '2026-07-17T00:00:00Z',
          updatedBy: '10000001',
          defaultRouteTarget: null,
          cloneable: false,
        },
        {
          devVersionId: 'dev-1',
          devVersionNumber: 1,
          releaseVersion: '1.0.0',
          lifecycleStatus: 'PUBLISHED',
          lineKind: 'PUBLISHED',
          updatedAt: '2026-07-16T00:00:00Z',
          updatedBy: '10000001',
          defaultRouteTarget: true,
          cloneable: true,
        },
      ],
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
    })
    vi.mocked(templatesApi.fetchReleaseVersionDetail).mockResolvedValue(
      baseTemplate({
        releaseVersion: '1.0.0',
        masterPin: {
          masterRevisionId: 'rev-1',
          masterFileHash: 'abc123',
        },
      }),
    )

    const wrapper = mountPanel()
    await flushPromises()

    expect(wrapper.find('[data-testid="template-dependencies-release-lines-table"]').exists()).toBe(
      true,
    )
    expect(wrapper.text()).toContain('IN_FLIGHT')
    expect(wrapper.text()).toContain('PUBLISHED')
    expect(wrapper.text()).not.toContain('Create from latest release')
    expect(wrapper.find('[data-version-line-create-from-latest]').exists()).toBe(false)
    expect(wrapper.findAll('button').filter((b) => /abandon|clone/i.test(b.text())).length).toBe(0)

    const links = wrapper.findAll('[data-testid="template-dependencies-release-line-link"]')
    const publishedLink = links.find((node) => node.text().includes('1.0.0'))
    expect(publishedLink).toBeTruthy()
    await publishedLink!.trigger('click')
    expect(routerPush).toHaveBeenCalledWith('/templates/tpl-1/releases/1.0.0')
  })

  it('BDD-CE-U19-DRV-009 — clause load failure shows error not empty success', async () => {
    vi.mocked(templatesApi.listTemplateContentModuleReferences).mockRejectedValue(
      new Error('network'),
    )

    const wrapper = mountPanel()
    await flushPromises()

    expect(wrapper.find('[data-testid="template-dependencies-clauses-error"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="template-dependencies-clauses-empty"]').exists()).toBe(false)
  })

  it('BDD-CE-U19-DRV-010 — does not expose write CTAs for authors', async () => {
    const wrapper = mountPanel()
    await flushPromises()
    expect(wrapper.find('[data-version-line-create-from-latest]').exists()).toBe(false)
    expect(wrapper.findAll('button').some((b) => /save|upsert|publish|clone|abandon|add reference/i.test(b.text()))).toBe(
      false,
    )
  })
})
