import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import MasterRevisionLinesPanel from '@/components/masters/MasterRevisionLinesPanel.vue'
import en from '@/i18n/locales/en'
import * as mastersApi from '@/api/masters'

vi.mock('@/api/masters', () => ({
  listMasterRevisionLines: vi.fn(),
}))

const routerPush = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush }),
}))

const currentLine = {
  id: 'revision-2',
  lineLabel: 'CURRENT' as const,
  status: 'DRAFT' as const,
  originalFilename: 'letterhead-v2.docx',
  anchorCount: 3,
  updatedAt: '2026-06-24T10:00:00Z',
  updatedBy: '10000001',
  current: true,
  revisionSequence: 2,
}

const historicalLine = {
  id: 'revision-1',
  lineLabel: 'HISTORICAL' as const,
  status: 'APPROVED' as const,
  originalFilename: 'letterhead-v1.docx',
  anchorCount: 2,
  updatedAt: '2026-06-23T10:00:00Z',
  updatedBy: '10000001',
  current: false,
  revisionSequence: 1,
}

function mountPanel() {
  const i18n = createI18n({
    legacy: false,
    locale: 'en',
    messages: { en },
  })

  return mount(MasterRevisionLinesPanel, {
    props: { masterId: 'master-1' },
    global: {
      plugins: [i18n, ElementPlus],
    },
  })
}

describe('MasterRevisionLinesPanel', () => {
  beforeEach(() => {
    routerPush.mockReset()
    vi.mocked(mastersApi.listMasterRevisionLines).mockReset()
  })

  it('navigates to revision detail on row click', async () => {
    vi.mocked(mastersApi.listMasterRevisionLines).mockResolvedValue({
      content: [
        {
          id: 'revision-1',
          lineLabel: 'CURRENT',
          status: 'APPROVED',
          originalFilename: 'letterhead.docx',
          anchorCount: 2,
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

    const wrapper = mountPanel()
    await flushPromises()

    await wrapper.find('.el-button--primary.is-link').trigger('click')

    expect(routerPush).toHaveBeenCalledWith('/masters/master-1/revisions/revision-1')
  })

  it('renders current and historical rows with badges after replace', async () => {
    vi.mocked(mastersApi.listMasterRevisionLines).mockResolvedValue({
      content: [currentLine, historicalLine],
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
    })

    const wrapper = mountPanel()
    await flushPromises()

    expect(wrapper.text()).toContain('Revision 2')
    expect(wrapper.text()).toContain('Revision 1')
    expect(wrapper.text()).toContain('Current')
    expect(wrapper.text()).toContain('Historical')
    expect(wrapper.text()).toContain('letterhead-v2.docx')
    expect(wrapper.text()).toContain('letterhead-v1.docx')
  })

  it('shows pagination when totalPages exceeds one and fetches the requested page', async () => {
    vi.mocked(mastersApi.listMasterRevisionLines).mockResolvedValue({
      content: [currentLine],
      page: 0,
      size: 20,
      totalElements: 25,
      totalPages: 2,
    })

    const wrapper = mountPanel()
    await flushPromises()

    expect(wrapper.find('.list-pagination').exists()).toBe(true)

    vi.mocked(mastersApi.listMasterRevisionLines).mockResolvedValue({
      content: [historicalLine],
      page: 1,
      size: 20,
      totalElements: 25,
      totalPages: 2,
    })

    await wrapper.find('.el-pagination .btn-next').trigger('click')
    await flushPromises()

    expect(mastersApi.listMasterRevisionLines).toHaveBeenLastCalledWith('master-1', 1, 20)
  })

  it('does not show pagination for a single revision line', async () => {
    vi.mocked(mastersApi.listMasterRevisionLines).mockResolvedValue({
      content: [currentLine],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })

    const wrapper = mountPanel()
    await flushPromises()

    expect(wrapper.find('.list-pagination').exists()).toBe(false)
  })
})
