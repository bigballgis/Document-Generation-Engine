import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MasterRevisionDetailView from '@/views/masters/MasterRevisionDetailView.vue'
import en from '@/i18n/locales/en'
import * as mastersApi from '@/api/masters'
import { useSessionStore } from '@/stores/session'

vi.mock('@/api/masters', () => ({
  getMaster: vi.fn(),
  getMasterRevisionLine: vi.fn(),
}))

const routerPush = vi.fn()
const routerReplace = vi.fn()

vi.mock('vue-router', () => ({
  useRoute: () => ({
    params: { masterId: 'master-1', revisionLineId: 'revision-1' },
    query: {},
  }),
  useRouter: () => ({ push: routerPush, replace: routerReplace }),
}))

vi.mock('@/auth/roles', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/auth/roles')>()
  return {
    ...actual,
    canReviewMasters: () => false,
    sessionContext: () => ({ roles: ['MASTER_DESIGNER'], capabilities: { manageMasters: true } }),
  }
})

describe('MasterRevisionDetailView', () => {
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
    routerReplace.mockReset()
    vi.mocked(mastersApi.getMaster).mockReset()
    vi.mocked(mastersApi.getMasterRevisionLine).mockReset()
  })

  it('renders revision overview and hides workflow actions for non-current line', async () => {
    vi.mocked(mastersApi.getMaster).mockResolvedValue({
      id: 'master-1',
      groupCode: 'RETAIL',
      name: 'Retail letterhead',
      description: null,
      status: 'APPROVED',
      originalFilename: 'letterhead.docx',
      changeSummary: null,
      anchors: [],
      reviewHistory: [],
      createdBy: '10000001',
      updatedBy: '10000001',
      createdAt: '2026-06-23T10:00:00Z',
      updatedAt: '2026-06-23T10:00:00Z',
    })
    vi.mocked(mastersApi.getMasterRevisionLine).mockResolvedValue({
      id: 'revision-1',
      masterId: 'master-1',
      lineLabel: 'HISTORICAL',
      status: 'APPROVED',
      originalFilename: 'letterhead-v1.docx',
      changeSummary: 'Initial upload',
      current: false,
      revisionSequence: 1,
      anchors: [{ anchorId: 'HEADER', displayLabel: 'Header block', documentSequence: 0 }],
      reviewHistory: [],
      createdBy: '10000001',
      updatedBy: '10000001',
      createdAt: '2026-06-23T10:00:00Z',
      updatedAt: '2026-06-23T11:00:00Z',
    })

    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })

    const wrapper = mount(MasterRevisionDetailView, {
      global: {
        plugins: [pinia, i18n, ElementPlus],
      },
    })

    await flushPromises()

    expect(wrapper.text()).toContain('Historical')
    expect(wrapper.find('[data-testid="master-anchor-edit-label-HEADER"]').exists()).toBe(false)

    expect(wrapper.text()).toContain('Revision overview')
    expect(wrapper.text()).toContain('Revision 1')
    expect(wrapper.text()).toContain('Historical')
    expect(wrapper.text()).toContain('DOCX overview')
    expect(wrapper.text()).toContain('Header block')
    expect(wrapper.find('.workspace-tab-shell__actions').text()).not.toContain('Submit for review')
    expect(wrapper.find('[data-master-journey-cta]').exists()).toBe(false)
  })

  it('navigates back to package hub from historical revision detail', async () => {
    vi.mocked(mastersApi.getMaster).mockResolvedValue({
      id: 'master-1',
      groupCode: 'RETAIL',
      name: 'Retail letterhead',
      description: null,
      status: 'APPROVED',
      originalFilename: 'letterhead-v2.docx',
      changeSummary: null,
      anchors: [],
      reviewHistory: [],
      createdBy: '10000001',
      updatedBy: '10000001',
      createdAt: '2026-06-23T10:00:00Z',
      updatedAt: '2026-06-23T10:00:00Z',
    })
    vi.mocked(mastersApi.getMasterRevisionLine).mockResolvedValue({
      id: 'revision-1',
      masterId: 'master-1',
      lineLabel: 'HISTORICAL',
      status: 'APPROVED',
      originalFilename: 'letterhead-v1.docx',
      changeSummary: null,
      current: false,
      revisionSequence: 1,
      anchors: [],
      reviewHistory: [],
      createdBy: '10000001',
      updatedBy: '10000001',
      createdAt: '2026-06-23T10:00:00Z',
      updatedAt: '2026-06-23T11:00:00Z',
    })

    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })

    const wrapper = mount(MasterRevisionDetailView, {
      global: {
        plugins: [pinia, i18n, ElementPlus],
      },
    })

    await flushPromises()

    await wrapper.find('.page-header .el-button--primary.is-link').trigger('click')

    expect(routerPush).toHaveBeenCalledWith('/masters/master-1')
  })

  it('renders designer journey submit step for current draft with anchors', async () => {
    vi.mocked(mastersApi.getMaster).mockResolvedValue({
      id: 'master-1',
      groupCode: 'RETAIL',
      name: 'Retail letterhead',
      description: null,
      status: 'DRAFT',
      originalFilename: 'letterhead.docx',
      changeSummary: null,
      anchors: [{ anchorId: 'HEADER', displayLabel: 'Header block', documentSequence: 0 }],
      reviewHistory: [],
      createdBy: '10000005',
      updatedBy: '10000005',
      createdAt: '2026-06-23T10:00:00Z',
      updatedAt: '2026-06-23T10:00:00Z',
    })
    vi.mocked(mastersApi.getMasterRevisionLine).mockResolvedValue({
      id: 'revision-1',
      masterId: 'master-1',
      lineLabel: 'CURRENT',
      status: 'DRAFT',
      originalFilename: 'letterhead.docx',
      changeSummary: null,
      current: true,
      anchors: [{ anchorId: 'HEADER', displayLabel: 'Header block', documentSequence: 0 }],
      reviewHistory: [],
      createdBy: '10000005',
      updatedBy: '10000005',
      createdAt: '2026-06-23T10:00:00Z',
      updatedAt: '2026-06-23T11:00:00Z',
    })

    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })

    const wrapper = mount(MasterRevisionDetailView, {
      global: {
        plugins: [pinia, i18n, ElementPlus],
      },
    })

    await flushPromises()

    expect(wrapper.find('[data-testid="master-anchor-edit-label-HEADER"]').exists()).toBe(true)
    expect(wrapper.find('[data-journey-timeline]').exists()).toBe(true)
    expect(wrapper.find('[data-master-journey-cta]').exists()).toBe(false)
    const approvalTab = wrapper.findAll('.el-tabs__item').find((tab) => tab.text().includes('Letterhead review'))
    expect(approvalTab).toBeTruthy()
    await approvalTab!.trigger('click')
    expect(wrapper.find('.workspace-tab-shell__actions').text()).toContain('Submit for review')
  })

  it('BDD-CE-U06-MAC-007 — hides displayLabel edit when PENDING_REVIEW', async () => {
    vi.mocked(mastersApi.getMaster).mockResolvedValue({
      id: 'master-1',
      groupCode: 'RETAIL',
      name: 'Retail letterhead',
      description: null,
      status: 'PENDING_REVIEW',
      originalFilename: 'letterhead.docx',
      changeSummary: null,
      anchors: [{ anchorId: 'HEADER', displayLabel: 'Header block', documentSequence: 0 }],
      reviewHistory: [],
      createdBy: '10000005',
      updatedBy: '10000005',
      createdAt: '2026-06-23T10:00:00Z',
      updatedAt: '2026-06-23T10:00:00Z',
    })
    vi.mocked(mastersApi.getMasterRevisionLine).mockResolvedValue({
      id: 'revision-1',
      masterId: 'master-1',
      lineLabel: 'CURRENT',
      status: 'PENDING_REVIEW',
      originalFilename: 'letterhead.docx',
      changeSummary: null,
      current: true,
      anchors: [{ anchorId: 'HEADER', displayLabel: 'Header block', documentSequence: 0 }],
      reviewHistory: [],
      createdBy: '10000005',
      updatedBy: '10000005',
      createdAt: '2026-06-23T10:00:00Z',
      updatedAt: '2026-06-23T11:00:00Z',
    })

    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })

    const wrapper = mount(MasterRevisionDetailView, {
      global: {
        plugins: [pinia, i18n, ElementPlus],
      },
    })

    await flushPromises()

    expect(wrapper.find('[data-testid="master-anchor-position-overview"]').exists()).toBe(true)
    expect(wrapper.find('[data-testid="master-anchor-edit-label-HEADER"]').exists()).toBe(false)
  })
})
