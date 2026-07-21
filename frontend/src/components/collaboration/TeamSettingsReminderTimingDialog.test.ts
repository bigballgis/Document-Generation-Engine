import { describe, expect, it, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import TeamSettingsReminderTimingDialog from '@/components/collaboration/TeamSettingsReminderTimingDialog.vue'
import * as collaborationApi from '@/api/collaboration'
import { useSessionStore } from '@/stores/session'
import en from '@/i18n/locales/en'

vi.mock('@/api/collaboration', () => ({
  getCollaborationTimeoutConfig: vi.fn(),
  upsertCollaborationTimeoutConfig: vi.fn(),
}))

describe('TeamSettingsReminderTimingDialog', () => {
  let pinia: ReturnType<typeof createPinia>

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    vi.mocked(collaborationApi.getCollaborationTimeoutConfig).mockResolvedValue({
      scopeType: 'GROUP',
      groupCode: 'RETAIL',
      testThresholdHours: 24,
      approvalThresholdHours: 24,
      pendingReleaseThresholdHours: 24,
      remediationThresholdHours: 48,
      updatedAt: '2026-06-26T10:00:00Z',
    })

    const sessionStore = useSessionStore()
    sessionStore.session = {
      roles: ['GROUP_ADMIN'],
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: [],
    } as never
  })

  it('opens group-scoped Reminder timing editor (BDD-RT-IA-003)', async () => {
    const wrapper = mount(TeamSettingsReminderTimingDialog, {
      props: { modelValue: true },
      global: {
        plugins: [
          pinia,
          createI18n({ legacy: false, locale: 'en', messages: { en } }),
          ElementPlus,
        ],
        stubs: {
          ElDialog: {
            template: '<div class="dialog"><slot /></div>',
            props: ['modelValue', 'title'],
          },
        },
      },
    })
    await flushPromises()

    expect(wrapper.find('.timeout-config-card').exists()).toBe(true)
    expect(wrapper.text()).toContain('Reminder timing')
    expect(wrapper.text()).not.toContain('Global default')
    expect(collaborationApi.getCollaborationTimeoutConfig).toHaveBeenCalledWith('RETAIL')
  })
})
