import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import en from '@/i18n/locales/en'
import * as mastersApi from '@/api/masters'
import AuthoringPathMasterPanel from '@/components/templates/AuthoringPathMasterPanel.vue'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import { useSessionStore } from '@/stores/session'

vi.mock('@/api/masters', () => ({
  getMaster: vi.fn(),
}))

describe('AuthoringPathMasterPanel', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(mastersApi.getMaster).mockReset()
    const session = useSessionStore()
    session.$patch({
      session: {
        username: 'author',
        displayName: 'Author',
        roles: ['TEMPLATE_AUTHOR'],
        capabilities: {},
        visibleRoutes: [ROUTE_KEYS.masterManagement],
      },
    } as never)
  })

  it('BDD-CE-U16-APC-003: shows master identity and anchor summary', async () => {
    vi.mocked(mastersApi.getMaster).mockResolvedValue({
      id: 'master-1',
      groupCode: 'G1',
      name: 'Letterhead Master',
      description: null,
      status: 'APPROVED',
      originalFilename: 'letter.docx',
      changeSummary: null,
      anchors: [
        { anchorId: 'a-2', displayLabel: 'Body', documentSequence: 1 },
        { anchorId: 'a-1', displayLabel: 'Header', documentSequence: 0 },
      ],
      reviewHistory: [],
      createdBy: 'u1',
      updatedBy: 'u1',
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
    })

    const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
    const pinia = createPinia()
    setActivePinia(pinia)
    const session = useSessionStore()
    session.$patch({
      session: {
        username: 'author',
        displayName: 'Author',
        roles: ['TEMPLATE_AUTHOR'],
        capabilities: {},
        visibleRoutes: [ROUTE_KEYS.masterManagement],
      },
    } as never)

    const wrapper = mount(AuthoringPathMasterPanel, {
      props: { masterId: 'master-1' },
      global: {
        plugins: [pinia, i18n, ElementPlus],
        stubs: { RouterLink: { template: '<a><slot /></a>' } },
      },
    })

    await flushPromises()

    expect(wrapper.find('[data-testid="authoring-path-master-panel"]').exists()).toBe(true)
    expect(wrapper.get('[data-testid="authoring-path-master-identity"]').text()).toContain(
      'Letterhead Master',
    )
    expect(wrapper.get('[data-testid="authoring-path-master-anchors"]').text()).toMatch(/2/)
    expect(wrapper.text()).toContain('Header')
    expect(wrapper.text()).toContain('Body')
  })
})
