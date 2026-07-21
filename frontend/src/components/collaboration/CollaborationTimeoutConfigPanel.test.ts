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
  let pinia: ReturnType<typeof createPinia>

  function mountPanel(props: { mode: 'global' | 'group'; groupCode?: string }) {
    return mount(CollaborationTimeoutConfigPanel, {
      props,
      global: {
        plugins: [
          pinia,
          createI18n({ legacy: false, locale: 'en', messages: { en } }),
          ElementPlus,
        ],
        stubs: {
          ElCard: { template: '<div class="timeout-card"><slot /></div>' },
          ElForm: { template: '<div><slot /></div>' },
          ElFormItem: { template: '<div class="form-item"><slot /></div>' },
          ElRadioGroup: { template: '<div class="scope-radio"><slot /></div>' },
          ElRadio: { template: '<label><slot /></label>' },
          ElInput: true,
          ElInputNumber: true,
        },
      },
    })
  }

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    vi.mocked(collaborationApi.getCollaborationTimeoutConfig).mockReset()
    vi.mocked(collaborationApi.upsertCollaborationTimeoutConfig).mockReset()

    const sessionStore = useSessionStore()
    sessionStore.session = {
      roles: ['GLOBAL_ADMIN'],
      authorizedGroupCodes: ['*'],
      visibleRoutes: [],
    } as never
  })

  it('loads global timeout config for global mode without scope switcher', async () => {
    vi.mocked(collaborationApi.getCollaborationTimeoutConfig).mockResolvedValue({
      scopeType: 'GLOBAL',
      groupCode: null,
      testThresholdHours: 72,
      approvalThresholdHours: 72,
      pendingReleaseThresholdHours: 48,
      remediationThresholdHours: 168,
      updatedAt: '2026-06-26T10:00:00Z',
    })

    const wrapper = mountPanel({ mode: 'global' })
    await flushPromises()

    expect(collaborationApi.getCollaborationTimeoutConfig).toHaveBeenCalledWith(undefined)
    expect(wrapper.find('.timeout-card').exists()).toBe(true)
    expect(wrapper.text()).toContain('Reminder timing')
    expect(wrapper.find('.scope-radio').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('Global default')
    expect(wrapper.text()).not.toContain('Group override')
  })

  it('saves global mode with scopeType GLOBAL and null groupCode', async () => {
    vi.mocked(collaborationApi.getCollaborationTimeoutConfig).mockResolvedValue({
      scopeType: 'GLOBAL',
      groupCode: null,
      testThresholdHours: 72,
      approvalThresholdHours: 72,
      pendingReleaseThresholdHours: 48,
      remediationThresholdHours: 168,
      updatedAt: '2026-06-26T10:00:00Z',
    })
    vi.mocked(collaborationApi.upsertCollaborationTimeoutConfig).mockResolvedValue({
      scopeType: 'GLOBAL',
      groupCode: null,
      testThresholdHours: 80,
      approvalThresholdHours: 72,
      pendingReleaseThresholdHours: 48,
      remediationThresholdHours: 168,
      updatedAt: '2026-06-26T11:00:00Z',
    })

    const wrapper = mountPanel({ mode: 'global' })
    await flushPromises()

    const saveButton = wrapper
      .findAll('button')
      .find((button) => button.text().includes('Save reminder timing'))
    expect(saveButton).toBeDefined()
    await saveButton!.trigger('click')
    await flushPromises()

    expect(collaborationApi.upsertCollaborationTimeoutConfig).toHaveBeenCalledWith(
      expect.objectContaining({
        scopeType: 'GLOBAL',
        groupCode: null,
      }),
    )
  })

  it('locks group mode to GROUP override for authorized group', async () => {
    const sessionStore = useSessionStore()
    sessionStore.session = {
      roles: ['GROUP_ADMIN'],
      authorizedGroupCodes: ['RETAIL'],
      visibleRoutes: [],
    } as never

    vi.mocked(collaborationApi.getCollaborationTimeoutConfig).mockResolvedValue({
      scopeType: 'GROUP',
      groupCode: 'RETAIL',
      testThresholdHours: 24,
      approvalThresholdHours: 24,
      pendingReleaseThresholdHours: 24,
      remediationThresholdHours: 48,
      updatedAt: '2026-06-26T10:00:00Z',
    })
    vi.mocked(collaborationApi.upsertCollaborationTimeoutConfig).mockResolvedValue({
      scopeType: 'GROUP',
      groupCode: 'RETAIL',
      testThresholdHours: 24,
      approvalThresholdHours: 24,
      pendingReleaseThresholdHours: 24,
      remediationThresholdHours: 48,
      updatedAt: '2026-06-26T11:00:00Z',
    })

    const wrapper = mountPanel({ mode: 'group', groupCode: 'RETAIL' })
    await flushPromises()

    expect(collaborationApi.getCollaborationTimeoutConfig).toHaveBeenCalledWith('RETAIL')
    expect(wrapper.find('.scope-radio').exists()).toBe(false)

    const saveButton = wrapper
      .findAll('button')
      .find((button) => button.text().includes('Save reminder timing'))
    await saveButton!.trigger('click')
    await flushPromises()

    expect(collaborationApi.upsertCollaborationTimeoutConfig).toHaveBeenCalledWith(
      expect.objectContaining({
        scopeType: 'GROUP',
        groupCode: 'RETAIL',
      }),
    )
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
    } as never

    const wrapper = mountPanel({ mode: 'global' })
    await flushPromises()

    expect(wrapper.find('.timeout-card').exists()).toBe(false)
    expect(collaborationApi.getCollaborationTimeoutConfig).not.toHaveBeenCalled()
  })
})
