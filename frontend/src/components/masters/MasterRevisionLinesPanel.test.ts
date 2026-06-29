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

    const i18n = createI18n({
      legacy: false,
      locale: 'en',
      messages: { en },
    })

    const wrapper = mount(MasterRevisionLinesPanel, {
      props: { masterId: 'master-1' },
      global: {
        plugins: [i18n, ElementPlus],
      },
    })

    await flushPromises()

    await wrapper.find('.el-button--primary.is-link').trigger('click')

    expect(routerPush).toHaveBeenCalledWith('/masters/master-1/revisions/revision-1')
  })
})
