import { describe, expect, it, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import CollaborationTimeoutConfigPanel from '@/components/collaboration/CollaborationTimeoutConfigPanel.vue'
import * as collaborationApi from '@/api/collaboration'
import { useSessionStore } from '@/stores/session'
import en from '@/i18n/locales/en'

vi.mock('@/api/collaboration', () => ({
  getCollaborationTimeoutConfig: vi.fn(),
  upsertCollaborationTimeoutConfig: vi.fn(),
}))

describe('CollaborationTimeoutConfigPanel', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(collaborationApi.getCollaborationTimeoutConfig).mockReset()
    vi.mocked(collaborationApi.upsertCollaborationTimeoutConfig).mockReset()

    const sessionStore = useSessionStore()
    sessionStore.session = {
      roles: ['GLOBAL_ADMIN'],
      authorizedGroupCodes: ['*'],
      visibleRoutes: [],
      capabilities: {},
    } as never
  })

  it('loads global timeout config for global admin', async () => {
    vi.mocked(collaborationApi.getCollaborationTimeoutConfig).mockResolvedValue({
      scopeType: 'GLOBAL',
      groupCode: null,
      testThresholdHours: 72,
      approvalThresholdHours: 72,
      pendingReleaseThresholdHours: 48,
      remediationThresholdHours: 168,
      updatedAt: '2026-06-26T10:00:00Z',
    })

    const wrapper = mount(CollaborationTimeoutConfigPanel, {
      global: {
        plugins: [
          createI18n({ legacy: false, locale: 'en', messages: { en } }),
          ElementPlus,
        ],
        stubs: {
          ElCard: { template: '<div class="timeout-card"><slot /></div>' },
          ElButton: { template: '<button><slot /></button>' },
          ElForm: { template: '<div><slot /></div>' },
          ElFormItem: { template: '<div><slot /></div>' },
          ElRadioGroup: { template: '<div><slot /></div>' },
          ElRadio: { template: '<label><slot /></label>' },
          ElInput: true,
          ElInputNumber: true,
        },
      },
    })

    await flushPromises()

    expect(collaborationApi.getCollaborationTimeoutConfig).toHaveBeenCalled()
    expect(wrapper.find('.timeout-card').exists()).toBe(true)
    expect(wrapper.text()).toContain('Reminder timing')
  })

  it('uses business-friendly save success copy', () => {
    expect(en.collaboration.timeoutConfig.saveSuccess).toBe('Reminder timing saved.')
    expect(en.collaboration.timeoutConfig.saveSuccess).not.toMatch(/threshold/i)
  })

  it('does not render for users without maintain permission', async () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      roles: ['TEMPLATE_TESTER'],
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: [],
      capabilities: {},
    } as never

    const wrapper = mount(CollaborationTimeoutConfigPanel, {
      global: {
        plugins: [
          createI18n({ legacy: false, locale: 'en', messages: { en } }),
          ElementPlus,
        ],
        stubs: {
          ElCard: { template: '<div class="timeout-card"><slot /></div>' },
        },
      },
    })

    await flushPromises()

    expect(wrapper.find('.timeout-card').exists()).toBe(false)
    expect(collaborationApi.getCollaborationTimeoutConfig).not.toHaveBeenCalled()
  })
})
