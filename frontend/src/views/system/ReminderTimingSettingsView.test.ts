import { describe, expect, it, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import ReminderTimingSettingsView from '@/views/system/ReminderTimingSettingsView.vue'
import * as collaborationApi from '@/api/collaboration'
import { useSessionStore } from '@/stores/session'
import en from '@/i18n/locales/en'

vi.mock('@/api/collaboration', () => ({
  getCollaborationTimeoutConfig: vi.fn(),
  upsertCollaborationTimeoutConfig: vi.fn(),
}))

describe('ReminderTimingSettingsView', () => {
  let pinia: ReturnType<typeof createPinia>

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    vi.mocked(collaborationApi.getCollaborationTimeoutConfig).mockResolvedValue({
      scopeType: 'GLOBAL',
      groupCode: null,
      testThresholdHours: 72,
      approvalThresholdHours: 72,
      pendingReleaseThresholdHours: 48,
      remediationThresholdHours: 168,
      updatedAt: '2026-06-26T10:00:00Z',
    })

    const sessionStore = useSessionStore()
    sessionStore.session = {
      roles: ['GLOBAL_ADMIN'],
      authorizedGroupCodes: ['*'],
      visibleRoutes: [],
    } as never
  })

  it('renders full-page Reminder timing locked to global default (BDD-RT-IA-001/002)', async () => {
    const wrapper = mount(ReminderTimingSettingsView, {
      global: {
        plugins: [
          pinia,
          createI18n({ legacy: false, locale: 'en', messages: { en } }),
          ElementPlus,
        ],
        stubs: {
          AppPageLayout: { template: '<div class="page-layout"><slot /></div>' },
          PageHeader: {
            props: ['title', 'description'],
            template: '<header><h1>{{ title }}</h1><p>{{ description }}</p></header>',
          },
        },
      },
    })
    await flushPromises()

    expect(wrapper.find('h1').text()).toBe('Reminder timing')
    expect(wrapper.text()).toContain('notifications only')
    expect(wrapper.find('.timeout-config-card').exists()).toBe(true)
    expect(wrapper.text()).not.toContain('Global default')
    expect(wrapper.text()).not.toContain('Group override')
    expect(collaborationApi.getCollaborationTimeoutConfig).toHaveBeenCalledWith(undefined)
  })
})
