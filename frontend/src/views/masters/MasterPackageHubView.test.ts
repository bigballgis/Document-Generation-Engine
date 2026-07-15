import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MasterPackageHubView from '@/views/masters/MasterPackageHubView.vue'
import en from '@/i18n/locales/en'
import * as mastersApi from '@/api/masters'
import { useSessionStore } from '@/stores/session'

vi.mock('@/api/masters', () => ({
  listMasters: vi.fn(),
  getMaster: vi.fn(),
  getMasterImpactAnalysis: vi.fn(),
  listMasterRevisionLines: vi.fn(),
  downloadMasterFile: vi.fn(),
  submitMasterReview: vi.fn(),
  decideMasterReview: vi.fn(),
}))

const routerPush = vi.fn()

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { masterId: 'master-1' } }),
  useRouter: () => ({ push: routerPush }),
}))

describe('MasterPackageHubView', () => {
  let pinia: ReturnType<typeof createPinia>

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    const sessionStore = useSessionStore()
    sessionStore.session = {
      roles: ['MASTER_DESIGNER'],
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: ['route.master-management'],
      capabilities: { manageMasters: true },
    } as never
    routerPush.mockReset()
    vi.mocked(mastersApi.getMaster).mockReset()
    vi.mocked(mastersApi.getMasterImpactAnalysis).mockReset()
    vi.mocked(mastersApi.listMasterRevisionLines).mockReset()
  })

  it('renders package header and revision lines panel', async () => {
    vi.mocked(mastersApi.getMaster).mockResolvedValue({
      id: 'master-1',
      groupCode: 'RETAIL',
      name: 'Retail letterhead',
      description: 'Header master',
      status: 'DRAFT',
      originalFilename: 'letterhead.docx',
      changeSummary: null,
      anchors: [],
      reviewHistory: [],
      createdBy: '10000001',
      updatedBy: '10000001',
      createdAt: '2026-06-23T10:00:00Z',
      updatedAt: '2026-06-23T10:00:00Z',
    })
    vi.mocked(mastersApi.getMasterImpactAnalysis).mockResolvedValue({
      masterId: 'master-1',
      referencedTemplateIds: [],
      retestRequired: false,
    })
    vi.mocked(mastersApi.listMasterRevisionLines).mockResolvedValue({
      content: [
        {
          id: 'revision-1',
          lineLabel: 'CURRENT',
          status: 'DRAFT',
          originalFilename: 'letterhead.docx',
          anchorCount: 1,
          updatedAt: '2026-06-23T10:00:00Z',
          updatedBy: '10000001',
          current: true,
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

    const wrapper = mount(MasterPackageHubView, {
      global: {
        plugins: [pinia, i18n, ElementPlus],
      },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('Retail letterhead')
    expect(wrapper.text()).toContain('Revision lines')
    expect(wrapper.text()).toContain('Current revision')
    expect(wrapper.text()).toContain('Impact analysis')
  })

  it('renders designer journey at upload step when no file is uploaded', async () => {
    vi.mocked(mastersApi.getMaster).mockResolvedValue({
      id: 'master-1',
      groupCode: 'RETAIL',
      name: 'New letterhead',
      description: null,
      status: 'DRAFT',
      originalFilename: '',
      changeSummary: null,
      anchors: [],
      reviewHistory: [],
      createdBy: '10000005',
      updatedBy: '10000005',
      createdAt: '2026-06-23T10:00:00Z',
      updatedAt: '2026-06-23T10:00:00Z',
    })
    vi.mocked(mastersApi.getMasterImpactAnalysis).mockResolvedValue({
      masterId: 'master-1',
      referencedTemplateIds: [],
      retestRequired: false,
    })
    vi.mocked(mastersApi.listMasterRevisionLines).mockResolvedValue({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    })

    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })

    const wrapper = mount(MasterPackageHubView, {
      global: {
        plugins: [pinia, i18n, ElementPlus],
      },
    })

    await flushPromises()

    expect(wrapper.find('[data-journey-timeline]').exists()).toBe(true)
    expect(wrapper.find('[data-journey-guidance]').exists()).toBe(false)
    expect(wrapper.find('.context-help-trigger').exists()).toBe(true)
    expect(wrapper.find('[data-master-journey-cta]').exists()).toBe(true)
    expect(wrapper.text()).toContain('Upload letterhead')
  })

  it('shows Submit for review on Hub when manageMasters and DRAFT (MRR-001)', async () => {
    vi.mocked(mastersApi.getMaster).mockResolvedValue({
      id: 'master-1',
      groupCode: 'RETAIL',
      name: 'Retail letterhead',
      description: null,
      status: 'DRAFT',
      originalFilename: 'letterhead.docx',
      changeSummary: null,
      anchors: [{ anchorId: 'HEADER', displayLabel: 'Header' }],
      reviewHistory: [],
      createdBy: '10000001',
      updatedBy: '10000001',
      createdAt: '2026-06-23T10:00:00Z',
      updatedAt: '2026-06-23T10:00:00Z',
    })
    vi.mocked(mastersApi.getMasterImpactAnalysis).mockResolvedValue({
      masterId: 'master-1',
      referencedTemplateIds: [],
      retestRequired: false,
    })
    vi.mocked(mastersApi.listMasterRevisionLines).mockResolvedValue({
      content: [
        {
          id: 'revision-1',
          lineLabel: 'CURRENT',
          status: 'DRAFT',
          originalFilename: 'letterhead.docx',
          anchorCount: 1,
          updatedAt: '2026-06-23T10:00:00Z',
          updatedBy: '10000001',
          current: true,
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

    const wrapper = mount(MasterPackageHubView, {
      global: {
        plugins: [pinia, i18n, ElementPlus],
      },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('Submit for review')
    expect(wrapper.find('[data-master-journey-cta]').exists()).toBe(true)
  })

  it('hides Submit for review without manageMasters (MRR-003)', async () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      roles: ['TEMPLATE_APPROVER'],
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: ['route.master-management'],
      capabilities: { manageMasters: false, reviewMasters: true },
    } as never

    vi.mocked(mastersApi.getMaster).mockResolvedValue({
      id: 'master-1',
      groupCode: 'RETAIL',
      name: 'Pending letterhead',
      description: null,
      status: 'DRAFT',
      originalFilename: 'letterhead.docx',
      changeSummary: null,
      anchors: [{ anchorId: 'HEADER', displayLabel: 'Header' }],
      reviewHistory: [],
      createdBy: '10000001',
      updatedBy: '10000001',
      createdAt: '2026-06-23T10:00:00Z',
      updatedAt: '2026-06-23T10:00:00Z',
    })
    vi.mocked(mastersApi.getMasterImpactAnalysis).mockResolvedValue({
      masterId: 'master-1',
      referencedTemplateIds: [],
      retestRequired: false,
    })
    vi.mocked(mastersApi.listMasterRevisionLines).mockResolvedValue({
      content: [
        {
          id: 'revision-1',
          lineLabel: 'CURRENT',
          status: 'DRAFT',
          originalFilename: 'letterhead.docx',
          anchorCount: 1,
          updatedAt: '2026-06-23T10:00:00Z',
          updatedBy: '10000001',
          current: true,
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

    const wrapper = mount(MasterPackageHubView, {
      global: {
        plugins: [pinia, i18n, ElementPlus],
      },
    })

    await flushPromises()

    expect(wrapper.text()).not.toContain('Submit for review')
  })

  it('shows Approve and Reject on Hub when reviewMasters and PENDING_REVIEW (MRR-002)', async () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      roles: ['TEMPLATE_APPROVER'],
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: ['route.master-management'],
      capabilities: { manageMasters: false, reviewMasters: true },
    } as never

    vi.mocked(mastersApi.getMaster).mockResolvedValue({
      id: 'master-1',
      groupCode: 'RETAIL',
      name: 'Pending letterhead',
      description: null,
      status: 'PENDING_REVIEW',
      originalFilename: 'letterhead.docx',
      changeSummary: 'Ready',
      anchors: [{ anchorId: 'HEADER', displayLabel: 'Header' }],
      reviewHistory: [],
      createdBy: '10000001',
      updatedBy: '10000001',
      createdAt: '2026-06-23T10:00:00Z',
      updatedAt: '2026-06-23T10:00:00Z',
    })
    vi.mocked(mastersApi.getMasterImpactAnalysis).mockResolvedValue({
      masterId: 'master-1',
      referencedTemplateIds: [],
      retestRequired: false,
    })
    vi.mocked(mastersApi.listMasterRevisionLines).mockResolvedValue({
      content: [
        {
          id: 'revision-1',
          lineLabel: 'CURRENT',
          status: 'PENDING_REVIEW',
          originalFilename: 'letterhead.docx',
          anchorCount: 1,
          updatedAt: '2026-06-23T10:00:00Z',
          updatedBy: '10000001',
          current: true,
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

    const wrapper = mount(MasterPackageHubView, {
      global: {
        plugins: [pinia, i18n, ElementPlus],
      },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('Approve')
    expect(wrapper.text()).toContain('Reject')
    expect(wrapper.text()).not.toContain('Update letterhead DOCX')
  })

  it('renders multiple revision lines from paginated history API', async () => {
    vi.mocked(mastersApi.getMaster).mockResolvedValue({
      id: 'master-1',
      groupCode: 'RETAIL',
      name: 'Retail letterhead',
      description: 'Header master',
      status: 'DRAFT',
      originalFilename: 'letterhead-v2.docx',
      changeSummary: null,
      anchors: [],
      reviewHistory: [],
      createdBy: '10000001',
      updatedBy: '10000001',
      createdAt: '2026-06-23T10:00:00Z',
      updatedAt: '2026-06-24T10:00:00Z',
    })
    vi.mocked(mastersApi.getMasterImpactAnalysis).mockResolvedValue({
      masterId: 'master-1',
      referencedTemplateIds: [],
      retestRequired: false,
    })
    vi.mocked(mastersApi.listMasterRevisionLines).mockResolvedValue({
      content: [
        {
          id: 'revision-2',
          lineLabel: 'CURRENT',
          status: 'DRAFT',
          originalFilename: 'letterhead-v2.docx',
          anchorCount: 2,
          updatedAt: '2026-06-24T10:00:00Z',
          updatedBy: '10000001',
          current: true,
          revisionSequence: 2,
        },
        {
          id: 'revision-1',
          lineLabel: 'HISTORICAL',
          status: 'APPROVED',
          originalFilename: 'letterhead-v1.docx',
          anchorCount: 1,
          updatedAt: '2026-06-23T10:00:00Z',
          updatedBy: '10000001',
          current: false,
          revisionSequence: 1,
        },
      ],
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
    })

    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })

    const wrapper = mount(MasterPackageHubView, {
      global: {
        plugins: [pinia, i18n, ElementPlus],
      },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('letterhead-v2.docx')
    expect(wrapper.text()).toContain('letterhead-v1.docx')
    expect(wrapper.text()).toContain('Update letterhead DOCX')
  })
})
