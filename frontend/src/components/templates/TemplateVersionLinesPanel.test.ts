import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import TemplateVersionLinesPanel from '@/components/templates/TemplateVersionLinesPanel.vue'
import en from '@/i18n/locales/en'
import * as templatesApi from '@/api/templates'

vi.mock('@/api/templates', () => ({
  listTemplateVersionLines: vi.fn(),
  cloneReleaseVersion: vi.fn(),
}))

const routerPush = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush }),
}))

const inFlightLine = {
  devVersionId: 'dev-2',
  devVersionNumber: 2,
  releaseVersion: null,
  lifecycleStatus: 'DRAFT' as const,
  lineKind: 'IN_FLIGHT' as const,
  updatedAt: '2026-06-24T10:00:00Z',
  updatedBy: '10000003',
  defaultRouteTarget: null,
}

const publishedLine = {
  devVersionId: 'dev-1',
  devVersionNumber: 1,
  releaseVersion: '1.0.0',
  lifecycleStatus: 'PUBLISHED' as const,
  lineKind: 'PUBLISHED' as const,
  cloneable: true,
  updatedAt: '2026-06-23T10:00:00Z',
  updatedBy: '10000003',
  defaultRouteTarget: true,
}

function mountPanel(canAuthor = true) {
  const i18n = createI18n({
    legacy: false,
    locale: 'en',
    messages: { en },
  })

  return mount(TemplateVersionLinesPanel, {
    props: { templateId: 'tpl-1', canClone: canAuthor },
    global: {
      plugins: [i18n, ElementPlus],
    },
  })
}

describe('TemplateVersionLinesPanel', () => {
  beforeEach(() => {
    routerPush.mockReset()
    vi.mocked(templatesApi.listTemplateVersionLines).mockReset()
    vi.mocked(templatesApi.cloneReleaseVersion).mockReset()
  })

  it('navigates to dev editor on in-flight row click', async () => {
    vi.mocked(templatesApi.listTemplateVersionLines).mockResolvedValue({
      content: [inFlightLine],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })

    const wrapper = mountPanel()
    await flushPromises()

    await wrapper.find('.el-button--primary.is-link').trigger('click')

    expect(routerPush).toHaveBeenCalledWith('/templates/tpl-1/dev/dev-2')
  })

  it('navigates to release detail on published row click', async () => {
    vi.mocked(templatesApi.listTemplateVersionLines).mockResolvedValue({
      content: [publishedLine],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })

    const wrapper = mountPanel()
    await flushPromises()

    await wrapper.find('.el-button--primary.is-link').trigger('click')

    expect(routerPush).toHaveBeenCalledWith('/templates/tpl-1/releases/1.0.0')
  })

  it('renders in-flight and published rows with badges', async () => {
    vi.mocked(templatesApi.listTemplateVersionLines).mockResolvedValue({
      content: [inFlightLine, publishedLine],
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
    })

    const wrapper = mountPanel()
    await flushPromises()

    expect(wrapper.text()).toContain('Dev version 2')
    expect(wrapper.text()).toContain('Release 1.0.0')
    expect(wrapper.text()).toContain('In flight')
    expect(wrapper.text()).toContain('Published')
    expect(wrapper.text()).toContain('Default route')
  })

  it('shows clone control on published rows when author-capable', async () => {
    vi.mocked(templatesApi.listTemplateVersionLines).mockResolvedValue({
      content: [{ ...publishedLine, cloneable: true }],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })
    vi.mocked(templatesApi.cloneReleaseVersion).mockResolvedValue({
      devVersionId: 'dev-3',
      devVersionNumber: 3,
      lifecycleStatus: 'DRAFT',
    })

    const wrapper = mountPanel(true)
    await flushPromises()

    const cloneButton = wrapper.find('[data-version-line-clone]')
    expect(cloneButton.exists()).toBe(true)

    await cloneButton.trigger('click')
    await flushPromises()

    expect(templatesApi.cloneReleaseVersion).toHaveBeenCalledWith('tpl-1', '1.0.0')
    expect(routerPush).toHaveBeenCalledWith('/templates/tpl-1/dev/dev-3')
  })

  it('hides clone control when published row is not cloneable', async () => {
    vi.mocked(templatesApi.listTemplateVersionLines).mockResolvedValue({
      content: [{ ...publishedLine, cloneable: false }],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    })

    const wrapper = mountPanel(true)
    await flushPromises()

    expect(wrapper.find('[data-version-line-clone]').exists()).toBe(false)
  })

  it('shows pagination when totalPages exceeds one', async () => {
    vi.mocked(templatesApi.listTemplateVersionLines).mockResolvedValue({
      content: [inFlightLine],
      page: 0,
      size: 20,
      totalElements: 25,
      totalPages: 2,
    })

    const wrapper = mountPanel()
    await flushPromises()

    expect(wrapper.find('.list-pagination').exists()).toBe(true)
  })
})
