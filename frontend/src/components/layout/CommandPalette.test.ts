import { mount, flushPromises } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import ElementPlus from 'element-plus'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import CommandPalette from '@/components/layout/CommandPalette.vue'
import en from '@/i18n/locales/en'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import { useSessionStore } from '@/stores/session'
import type { ManagementCapabilities } from '@/types/session'

const routerPush = vi.fn()
const listTemplates = vi.fn()
const listMasters = vi.fn()
const listContentModules = vi.fn()

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush }),
}))

vi.mock('@/api/templates', () => ({
  listTemplates: (...args: unknown[]) => listTemplates(...args),
}))
vi.mock('@/api/masters', () => ({
  listMasters: (...args: unknown[]) => listMasters(...args),
}))
vi.mock('@/api/contentModules', () => ({
  listContentModules: (...args: unknown[]) => listContentModules(...args),
}))

const capabilities: ManagementCapabilities = {
  manageMasters: true,
  reviewMasters: true,
  authorTemplates: true,
  decideTests: true,
  decideApprovals: true,
  decideLegalApprovals: false,
  publishTemplates: true,
  stopTemplates: true,
  restoreOrDeprecateTemplates: true,
  deleteTemplates: true,
  exportTemplates: true,
  viewCollaborationWorkItems: true,
  maintainCollaborationTimeoutConfig: true,
  authorContentModules: true,
  decideContentModuleReviews: true,
  manageContentModuleLifecycle: true,
  manageApiPolicy: true,
  readAudit: true,
  manageAssetLibrary: true,
  manageLegalHold: false,
}

function mountPalette(visibleRoutes: string[]) {
  const pinia = createPinia()
  setActivePinia(pinia)
  const sessionStore = useSessionStore()
  sessionStore.$patch({
    accessToken: 'token',
    session: {
      username: '10000001',
      displayName: 'Admin',
      email: 'admin@example.com',
      authSource: 'LOCAL',
      roles: ['GLOBAL_ADMIN'],
      authorizedGroupCodes: ['*'],
      defaultRoute: ROUTE_KEYS.dashboardHome,
      visibleRoutes,
      capabilities,
      expiresAt: new Date(Date.now() + 30 * 60_000).toISOString(),
    },
  })

  const i18n = createI18n({ legacy: false, locale: 'en', messages: { en } })
  return mount(CommandPalette, {
    attachTo: document.body,
    global: { plugins: [pinia, i18n, ElementPlus] },
  })
}

describe('CommandPalette', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    routerPush.mockReset()
    listTemplates.mockReset()
    listMasters.mockReset()
    listContentModules.mockReset()
    listTemplates.mockResolvedValue({ content: [], totalElements: 0, totalPages: 0, page: 0, size: 8 })
    listMasters.mockResolvedValue({ content: [], totalElements: 0, totalPages: 0, page: 0, size: 8 })
    listContentModules.mockResolvedValue({
      content: [],
      totalElements: 0,
      totalPages: 0,
      page: 0,
      size: 8,
    })
  })

  afterEach(() => {
    vi.useRealTimers()
    document.body.innerHTML = ''
  })

  it('opens on Ctrl+K with dialog a11y and focuses input (BDD-001 / C6-011)', async () => {
    const wrapper = mountPalette([ROUTE_KEYS.dashboardHome, ROUTE_KEYS.templateManagement])
    expect(wrapper.find('[data-testid="command-palette"]').exists()).toBe(false)

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'k', ctrlKey: true, bubbles: true }))
    await flushPromises()

    const dialog = wrapper.find('[data-testid="command-palette"]')
    expect(dialog.exists()).toBe(true)
    expect(dialog.attributes('role')).toBe('dialog')
    expect(dialog.attributes('aria-modal')).toBe('true')
    expect(dialog.attributes('aria-label')).toBe('Command palette')

    const input = wrapper.find('[data-testid="command-palette-input"]')
    expect(input.exists()).toBe(true)
    await flushPromises()
    expect(document.activeElement).toBe(input.element)

    expect(wrapper.findAll('[data-testid="command-palette-option"]').length).toBeGreaterThan(0)
    expect(listTemplates).not.toHaveBeenCalled()
  })

  it('closes on Escape and restores prior focus (BDD-005)', async () => {
    const trigger = document.createElement('button')
    trigger.textContent = 'prior'
    document.body.appendChild(trigger)
    trigger.focus()

    const wrapper = mountPalette([ROUTE_KEYS.dashboardHome])
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'k', ctrlKey: true, bubbles: true }))
    await flushPromises()
    expect(wrapper.find('[data-testid="command-palette"]').exists()).toBe(true)

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }))
    await flushPromises()
    expect(wrapper.find('[data-testid="command-palette"]').exists()).toBe(false)
    expect(document.activeElement).toBe(trigger)
  })

  it('closes on backdrop click (BDD-014)', async () => {
    const wrapper = mountPalette([ROUTE_KEYS.dashboardHome])
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'k', ctrlKey: true, bubbles: true }))
    await flushPromises()
    await wrapper.find('[data-testid="command-palette-backdrop"]').trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-testid="command-palette"]').exists()).toBe(false)
  })

  it('searches templates and navigates on option click (BDD-002)', async () => {
    listTemplates.mockResolvedValue({
      content: [
        {
          id: 'tpl-42',
          externalId: 'DEMO-T',
          groupCode: 'RETAIL',
          name: 'Demo Template T',
          lifecycleStatus: 'DRAFT',
          releaseVersion: null,
          releaseVersionCount: 0,
          masterId: 'm1',
          updatedBy: 'u',
          updatedAt: '2026-01-01T00:00:00Z',
        },
      ],
      totalElements: 1,
      totalPages: 1,
      page: 0,
      size: 8,
    })

    const wrapper = mountPalette([ROUTE_KEYS.dashboardHome, ROUTE_KEYS.templateManagement])
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'k', ctrlKey: true, bubbles: true }))
    await flushPromises()

    await wrapper.find('[data-testid="command-palette-input"]').setValue('Demo')
    await vi.advanceTimersByTimeAsync(250)
    await flushPromises()

    expect(listTemplates).toHaveBeenCalledWith(
      0,
      8,
      expect.objectContaining({ search: 'Demo' }),
    )

    const templateOption = wrapper
      .findAll('[data-testid="command-palette-option"]')
      .find((node) => node.attributes('data-kind') === 'template')
    expect(templateOption).toBeTruthy()
    await templateOption!.trigger('click')
    await flushPromises()

    expect(routerPush).toHaveBeenCalledWith({ path: '/templates/tpl-42' })
    expect(wrapper.find('[data-testid="command-palette"]').exists()).toBe(false)
  })

  it('does not show content-module group without route (BDD-003)', async () => {
    const wrapper = mountPalette([ROUTE_KEYS.dashboardHome, ROUTE_KEYS.templateManagement])
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'k', ctrlKey: true, bubbles: true }))
    await flushPromises()
    await wrapper.find('[data-testid="command-palette-input"]').setValue('Secret')
    await vi.advanceTimersByTimeAsync(250)
    await flushPromises()

    expect(listContentModules).not.toHaveBeenCalled()
    expect(wrapper.find('[data-testid="command-palette-group-content-modules"]').exists()).toBe(
      false,
    )
  })
})
