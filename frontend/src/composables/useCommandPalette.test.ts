import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick, ref } from 'vue'
import {
  clearAuthoringEditorContext,
  registerAuthoringEditorContext,
} from '@/composables/authoringEditorContext'
import {
  buildPaletteRouteItems,
  COMMAND_PALETTE_DEBOUNCE_MS,
  COMMAND_PALETTE_PAGE_SIZE,
  filterPaletteRouteItems,
  moveHighlightIndex,
  useCommandPalette,
} from '@/composables/useCommandPalette'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import type { ManagementCapabilities } from '@/types/session'

const listTemplates = vi.fn()
const listMasters = vi.fn()
const listContentModules = vi.fn()

vi.mock('@/api/templates', () => ({
  listTemplates: (...args: unknown[]) => listTemplates(...args),
}))
vi.mock('@/api/masters', () => ({
  listMasters: (...args: unknown[]) => listMasters(...args),
}))
vi.mock('@/api/contentModules', () => ({
  listContentModules: (...args: unknown[]) => listContentModules(...args),
}))

const fullCapabilities: ManagementCapabilities = {
  manageMasters: true,
  reviewMasters: true,
  authorTemplates: true,
  decideTests: true,
  decideApprovals: true,
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

function translate(key: string): string {
  const map: Record<string, string> = {
    'nav.items.dashboard': 'My tasks',
    'nav.items.templates': 'Templates',
    'nav.items.masters': 'Letterhead templates',
    'nav.items.contentModules': 'Standard clauses',
    'nav.items.audit': 'Activity log',
    'nav.items.users': 'Users',
    'nav.items.groups': 'Groups',
    'nav.items.apiPolicies': 'External services overview',
    'commandPalette.actions.saveBinding': 'Save binding',
    'commandPalette.actions.refreshPreview': 'Refresh preview',
  }
  return map[key] ?? key
}

describe('command palette helpers', () => {
  it('locks page size and debounce constants (C6-C6 / C6-C7)', () => {
    expect(COMMAND_PALETTE_PAGE_SIZE).toBe(8)
    expect(COMMAND_PALETTE_DEBOUNCE_MS).toBe(250)
  })

  it('clamps highlight without wrapping (C6-C12)', () => {
    expect(moveHighlightIndex(0, -1, 3)).toBe(0)
    expect(moveHighlightIndex(2, 1, 3)).toBe(2)
    expect(moveHighlightIndex(1, 1, 3)).toBe(2)
    expect(moveHighlightIndex(-1, 1, 3)).toBe(0)
    expect(moveHighlightIndex(0, 1, 0)).toBe(-1)
  })

  it('builds route items only from visibleRoutes (fail-closed)', () => {
    const items = buildPaletteRouteItems(
      [ROUTE_KEYS.dashboardHome, ROUTE_KEYS.templateManagement],
      ['TEMPLATE_TESTER'],
      fullCapabilities,
      translate,
    )
    const paths = items.map((item) => item.target.path)
    expect(paths).toContain('/dashboard')
    expect(paths).toContain('/templates')
    expect(paths).not.toContain('/content-modules')
    expect(paths).not.toContain('/masters')
    expect(paths).not.toContain('/audit')
  })

  it('filters routes by label and path contains (case-insensitive)', () => {
    const items = buildPaletteRouteItems(
      [ROUTE_KEYS.dashboardHome, ROUTE_KEYS.templateManagement, ROUTE_KEYS.auditConsole],
      ['GLOBAL_ADMIN'],
      fullCapabilities,
      translate,
    )
    expect(filterPaletteRouteItems(items, 'temp').map((i) => i.target.path)).toEqual(['/templates'])
    expect(filterPaletteRouteItems(items, '/AUDIT').map((i) => i.target.path)).toEqual(['/audit'])
    expect(filterPaletteRouteItems(items, '   ').length).toBe(items.length)
  })
})

describe('useCommandPalette', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.useFakeTimers()
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
    clearAuthoringEditorContext()
  })

  function createPalette(visibleRoutes: string[]) {
    const navigate = vi.fn()
    const palette = useCommandPalette({
      visibleRoutes: ref(visibleRoutes),
      roles: ref(['GLOBAL_ADMIN']),
      capabilities: ref(fullCapabilities),
      translate,
      navigate,
      bindShortcut: false,
    })
    return { palette, navigate }
  }

  it('empty query shows routes only and does not call catalog APIs (C6-C4)', async () => {
    const { palette } = createPalette([
      ROUTE_KEYS.dashboardHome,
      ROUTE_KEYS.templateManagement,
      ROUTE_KEYS.masterManagement,
      ROUTE_KEYS.contentModuleManagement,
    ])
    palette.openPalette()
    palette.setQuery('  ')
    await vi.advanceTimersByTimeAsync(COMMAND_PALETTE_DEBOUNCE_MS + 10)
    await nextTick()
    expect(listTemplates).not.toHaveBeenCalled()
    expect(listMasters).not.toHaveBeenCalled()
    expect(listContentModules).not.toHaveBeenCalled()
    expect(palette.groups.value.map((g) => g.id)).toEqual(['routes'])
    expect(palette.flatItems.value.length).toBeGreaterThan(0)
  })

  it('searches authorized catalogs with page=0 size=8 search=Q (C6-C6 / C6-C8)', async () => {
    listTemplates.mockResolvedValue({
      content: [
        {
          id: 'tpl-1',
          externalId: 'DEMO-T',
          groupCode: 'RETAIL',
          name: 'Demo Template',
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
    const { palette } = createPalette([
      ROUTE_KEYS.dashboardHome,
      ROUTE_KEYS.templateManagement,
      ROUTE_KEYS.masterManagement,
      ROUTE_KEYS.contentModuleManagement,
    ])
    palette.openPalette()
    palette.setQuery('Demo')
    await vi.advanceTimersByTimeAsync(COMMAND_PALETTE_DEBOUNCE_MS)
    await nextTick()
    expect(listTemplates).toHaveBeenCalledWith(
      0,
      8,
      expect.objectContaining({ search: 'Demo', signal: expect.any(AbortSignal) }),
    )
    expect(listMasters).toHaveBeenCalledWith(
      0,
      8,
      expect.objectContaining({ search: 'Demo' }),
    )
    expect(listContentModules).toHaveBeenCalledWith(
      0,
      8,
      expect.objectContaining({ search: 'Demo' }),
    )
    const templateGroup = palette.groups.value.find((g) => g.id === 'templates')
    expect(templateGroup?.items[0]?.target.path).toBe('/templates/tpl-1')
  })

  it('does not request catalogs without route gate (C6-C5 / C6-C9)', async () => {
    const { palette } = createPalette([ROUTE_KEYS.dashboardHome, ROUTE_KEYS.templateManagement])
    palette.openPalette()
    palette.setQuery('X')
    await vi.advanceTimersByTimeAsync(COMMAND_PALETTE_DEBOUNCE_MS)
    await nextTick()
    expect(listTemplates).toHaveBeenCalled()
    expect(listMasters).not.toHaveBeenCalled()
    expect(listContentModules).not.toHaveBeenCalled()
    expect(palette.groups.value.map((g) => g.id)).toEqual(['routes', 'templates'])
  })

  it('shows catalog error without no-match disguise (C6-C15)', async () => {
    listTemplates.mockRejectedValue(new Error('network'))
    const { palette } = createPalette([ROUTE_KEYS.dashboardHome, ROUTE_KEYS.templateManagement])
    palette.openPalette()
    palette.setQuery('zzz-no-route-match-xyz')
    await vi.advanceTimersByTimeAsync(COMMAND_PALETTE_DEBOUNCE_MS)
    await Promise.resolve()
    await nextTick()
    const templateGroup = palette.groups.value.find((g) => g.id === 'templates')
    expect(templateGroup?.errorMessageKey).toBeTruthy()
    expect(palette.showNoMatch.value).toBe(false)
  })

  it('shows no-match when query has zero hits and no errors (C6-C16)', async () => {
    const { palette } = createPalette([ROUTE_KEYS.dashboardHome, ROUTE_KEYS.templateManagement])
    palette.openPalette()
    palette.setQuery('zzz-no-hit-xyz')
    await vi.advanceTimersByTimeAsync(COMMAND_PALETTE_DEBOUNCE_MS)
    await Promise.resolve()
    await nextTick()
    expect(palette.showNoMatch.value).toBe(true)
  })

  it('navigates on activate and closes (C6-C9)', async () => {
    const { palette, navigate } = createPalette([
      ROUTE_KEYS.dashboardHome,
      ROUTE_KEYS.templateManagement,
    ])
    palette.openPalette()
    expect(palette.open.value).toBe(true)
    const routeItem = palette.flatItems.value.find((i) => i.target.path === '/templates')
    expect(routeItem).toBeTruthy()
    await palette.activateItem(routeItem!)
    expect(palette.open.value).toBe(false)
    expect(navigate).toHaveBeenCalledWith({ path: '/templates' })
  })

  it('keyboard handlers move highlight and Enter activates', async () => {
    const { palette, navigate } = createPalette([
      ROUTE_KEYS.dashboardHome,
      ROUTE_KEYS.templateManagement,
      ROUTE_KEYS.auditConsole,
    ])
    palette.openPalette()
    expect(palette.flatItems.value.length).toBeGreaterThanOrEqual(2)
    palette.handleGlobalKeydown(
      new KeyboardEvent('keydown', { key: 'ArrowDown', bubbles: true }),
    )
    expect(palette.highlightIndex.value).toBe(0)
    palette.handleGlobalKeydown(
      new KeyboardEvent('keydown', { key: 'ArrowDown', bubbles: true }),
    )
    expect(palette.highlightIndex.value).toBe(1)
    palette.handleGlobalKeydown(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }))
    await nextTick()
    expect(navigate).toHaveBeenCalled()
    expect(palette.open.value).toBe(false)
  })

  it('Escape closes palette', () => {
    const { palette } = createPalette([ROUTE_KEYS.dashboardHome])
    palette.openPalette()
    palette.handleGlobalKeydown(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }))
    expect(palette.open.value).toBe(false)
  })

  it('Ctrl+K opens palette (preventDefault)', () => {
    const { palette } = createPalette([ROUTE_KEYS.dashboardHome])
    const event = new KeyboardEvent('keydown', { key: 'k', ctrlKey: true, bubbles: true })
    const prevent = vi.spyOn(event, 'preventDefault')
    palette.handleGlobalKeydown(event)
    expect(prevent).toHaveBeenCalled()
    expect(palette.open.value).toBe(true)
  })

  it('BDD-CE-U17-EKS-003: lists author Actions and executes Save binding then closes', async () => {
    const saveBinding = vi.fn()
    const refreshPreview = vi.fn()
    registerAuthoringEditorContext({
      saveBinding,
      refreshPreview,
      canSave: () => true,
      canRefresh: () => true,
      isSaving: () => false,
      isRefreshing: () => false,
    })
    const { palette, navigate } = createPalette([ROUTE_KEYS.dashboardHome])
    palette.openPalette()
    expect(palette.groups.value.map((g) => g.id)).toEqual(['actions', 'routes'])
    const saveItem = palette.flatItems.value.find((i) => i.id === 'action:save-binding')
    expect(saveItem?.title).toBe('Save binding')
    expect(saveItem?.optionTestId).toBe('command-palette-action-save-binding')
    await palette.activateItem(saveItem!)
    expect(palette.open.value).toBe(false)
    expect(saveBinding).toHaveBeenCalledOnce()
    expect(navigate).not.toHaveBeenCalled()
  })

  it('BDD-CE-U17-EKS-005: omits author Actions outside edit context', () => {
    const { palette } = createPalette([ROUTE_KEYS.dashboardHome])
    palette.openPalette()
    expect(palette.groups.value.map((g) => g.id)).toEqual(['routes'])
    expect(palette.flatItems.value.some((i) => i.kind === 'action')).toBe(false)
  })
})

describe('useCommandPalette route gate matrix', () => {
  it('content-module absent from results when route missing (C6-C3 fail-closed)', async () => {
    setActivePinia(createPinia())
    vi.useFakeTimers()
    listContentModules.mockResolvedValue({
      content: [
        {
          moduleId: 'cm-secret',
          moduleCode: 'SECRET-M',
          name: 'Secret Module M',
          groupCode: 'RETAIL',
          createdAt: '2026-01-01T00:00:00Z',
          updatedAt: '2026-01-01T00:00:00Z',
        },
      ],
      totalElements: 1,
      totalPages: 1,
      page: 0,
      size: 8,
    })
    const palette = useCommandPalette({
      visibleRoutes: ref([ROUTE_KEYS.dashboardHome, ROUTE_KEYS.templateManagement]),
      roles: ref(['TEMPLATE_AUTHOR']),
      capabilities: ref(fullCapabilities),
      translate,
      navigate: vi.fn(),
      bindShortcut: false,
    })
    palette.openPalette()
    palette.setQuery('Secret')
    await vi.advanceTimersByTimeAsync(COMMAND_PALETTE_DEBOUNCE_MS)
    await Promise.resolve()
    await nextTick()
    expect(listContentModules).not.toHaveBeenCalled()
    expect(palette.flatItems.value.some((item) => item.id.includes('cm-secret'))).toBe(false)
    expect(palette.groups.value.some((g) => g.id === 'content-modules')).toBe(false)
    vi.useRealTimers()
  })
})
