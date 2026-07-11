import {
  computed,
  onMounted,
  onUnmounted,
  ref,
  watch,
  type ComputedRef,
  type Ref,
} from 'vue'
import { listContentModules } from '@/api/contentModules'
import { listMasters } from '@/api/masters'
import { listTemplates } from '@/api/templates'
import { resolveApiErrorMessageKey } from '@/api/errorEnvelope'
import {
  buildVisibleNavGroups,
  resolveNavItemTarget,
  type NavItemDefinition,
} from '@/navigation/navStructure'
import {
  contentModuleDetailPath,
  masterDetailPath,
  ROUTE_KEYS,
  templatePackageHubPath,
} from '@/routing/routeKeys'
import type { ManagementCapabilities } from '@/types/session'

/** Locked by BDD C6-C6 / Vitest. */
export const COMMAND_PALETTE_PAGE_SIZE = 8

/** Locked by BDD C6-C7 / Vitest. */
export const COMMAND_PALETTE_DEBOUNCE_MS = 250

export type PaletteItemKind = 'route' | 'template' | 'master' | 'content-module'

export interface PaletteNavTarget {
  path: string
  query?: Record<string, string>
  hash?: string
}

export interface PaletteItem {
  id: string
  kind: PaletteItemKind
  title: string
  subtitle: string
  target: PaletteNavTarget
}

export interface PaletteGroupView {
  id: 'routes' | 'templates' | 'masters' | 'content-modules'
  labelKey: string
  items: PaletteItem[]
  errorMessageKey: string | null
  loading: boolean
}

type ReadonlyStringList = Ref<readonly string[]> | ComputedRef<readonly string[]>
type CapabilitiesRef =
  | Ref<ManagementCapabilities | undefined>
  | ComputedRef<ManagementCapabilities | undefined>

export interface UseCommandPaletteOptions {
  visibleRoutes: ReadonlyStringList
  roles: ReadonlyStringList
  capabilities: CapabilitiesRef
  translate: (key: string) => string
  navigate: (target: PaletteNavTarget) => void | Promise<void>
  /** Injected for tests; defaults to document keydown. */
  bindShortcut?: boolean
}

function containsIgnoreCase(haystack: string, needle: string): boolean {
  return haystack.toLocaleLowerCase().includes(needle.toLocaleLowerCase())
}

/** Flatten sidebar-aligned nav items from visibleRoutes (legacy keys deduped via path). */
export function buildPaletteRouteItems(
  visibleRoutes: readonly string[],
  roles: readonly string[],
  capabilities: ManagementCapabilities | undefined,
  translate: (key: string) => string,
): PaletteItem[] {
  const groups = buildVisibleNavGroups([...visibleRoutes], [...roles], capabilities)
  const seenPaths = new Set<string>()
  const items: PaletteItem[] = []
  for (const group of groups) {
    for (const navItem of group.items) {
      const target = resolveNavItemTarget(navItem)
      const pathKey = `${target.path}|${target.hash ?? ''}|${JSON.stringify(target.query ?? {})}`
      if (seenPaths.has(pathKey)) {
        continue
      }
      seenPaths.add(pathKey)
      items.push(navItemToPaletteItem(navItem, translate))
    }
  }
  return items
}

function navItemToPaletteItem(
  navItem: NavItemDefinition,
  translate: (key: string) => string,
): PaletteItem {
  const target = resolveNavItemTarget(navItem)
  return {
    id: `route:${navItem.id}`,
    kind: 'route',
    title: translate(navItem.labelKey),
    subtitle: target.path,
    target,
  }
}

export function filterPaletteRouteItems(items: PaletteItem[], query: string): PaletteItem[] {
  const trimmed = query.trim()
  if (!trimmed) {
    return items
  }
  return items.filter(
    (item) =>
      containsIgnoreCase(item.title, trimmed) || containsIgnoreCase(item.subtitle, trimmed),
  )
}

/** Clamp highlight (no wrap) — BDD C6-C12. */
export function moveHighlightIndex(current: number, delta: number, length: number): number {
  if (length <= 0) {
    return -1
  }
  const next = current < 0 ? (delta > 0 ? 0 : length - 1) : current + delta
  return Math.max(0, Math.min(length - 1, next))
}

function canQueryCatalog(visibleRoutes: readonly string[], routeKey: string): boolean {
  return visibleRoutes.includes(routeKey)
}

export function useCommandPalette(options: UseCommandPaletteOptions) {
  const open = ref(false)
  const query = ref('')
  const highlightIndex = ref(-1)
  const loading = ref(false)
  const focusNonce = ref(0)
  const templateItems = ref<PaletteItem[]>([])
  const masterItems = ref<PaletteItem[]>([])
  const contentModuleItems = ref<PaletteItem[]>([])
  const templateErrorKey = ref<string | null>(null)
  const masterErrorKey = ref<string | null>(null)
  const contentModuleErrorKey = ref<string | null>(null)
  const templateLoading = ref(false)
  const masterLoading = ref(false)
  const contentModuleLoading = ref(false)

  let debounceTimer: ReturnType<typeof setTimeout> | null = null
  let searchAbort: AbortController | null = null
  let previousFocus: HTMLElement | null = null

  const allRouteItems = computed(() =>
    buildPaletteRouteItems(
      options.visibleRoutes.value,
      options.roles.value,
      options.capabilities.value,
      options.translate,
    ),
  )

  const filteredRouteItems = computed(() =>
    filterPaletteRouteItems(allRouteItems.value, query.value),
  )

  const groups = computed((): PaletteGroupView[] => {
    const trimmed = query.value.trim()
    const result: PaletteGroupView[] = [
      {
        id: 'routes',
        labelKey: 'commandPalette.groups.routes',
        items: filteredRouteItems.value,
        errorMessageKey: null,
        loading: false,
      },
    ]
    if (trimmed) {
      if (canQueryCatalog(options.visibleRoutes.value, ROUTE_KEYS.templateManagement)) {
        result.push({
          id: 'templates',
          labelKey: 'commandPalette.groups.templates',
          items: templateItems.value,
          errorMessageKey: templateErrorKey.value,
          loading: templateLoading.value,
        })
      }
      if (canQueryCatalog(options.visibleRoutes.value, ROUTE_KEYS.masterManagement)) {
        result.push({
          id: 'masters',
          labelKey: 'commandPalette.groups.masters',
          items: masterItems.value,
          errorMessageKey: masterErrorKey.value,
          loading: masterLoading.value,
        })
      }
      if (canQueryCatalog(options.visibleRoutes.value, ROUTE_KEYS.contentModuleManagement)) {
        result.push({
          id: 'content-modules',
          labelKey: 'commandPalette.groups.contentModules',
          items: contentModuleItems.value,
          errorMessageKey: contentModuleErrorKey.value,
          loading: contentModuleLoading.value,
        })
      }
    }
    return result
  })

  const flatItems = computed(() => groups.value.flatMap((group) => group.items))

  const hasAnyError = computed(() =>
    groups.value.some((group) => Boolean(group.errorMessageKey)),
  )

  const showNoMatch = computed(() => {
    const trimmed = query.value.trim()
    if (!trimmed) {
      return false
    }
    if (loading.value || groups.value.some((g) => g.loading)) {
      return false
    }
    if (flatItems.value.length > 0) {
      return false
    }
    // Errors must not be disguised as no-match (C6-C15 / C6-C16).
    if (hasAnyError.value) {
      return false
    }
    return true
  })

  function clearDebounce() {
    if (debounceTimer !== null) {
      clearTimeout(debounceTimer)
      debounceTimer = null
    }
  }

  function abortSearch() {
    if (searchAbort) {
      searchAbort.abort()
      searchAbort = null
    }
  }

  function resetCatalogResults() {
    templateItems.value = []
    masterItems.value = []
    contentModuleItems.value = []
    templateErrorKey.value = null
    masterErrorKey.value = null
    contentModuleErrorKey.value = null
    templateLoading.value = false
    masterLoading.value = false
    contentModuleLoading.value = false
    loading.value = false
  }

  function openPalette() {
    if (!open.value) {
      const active = document.activeElement
      previousFocus = active instanceof HTMLElement ? active : null
      open.value = true
      query.value = ''
      highlightIndex.value = -1
      resetCatalogResults()
    }
    // Already open: keep open and bump focus nonce so UI refocuses input (C6-C2).
    focusNonce.value += 1
  }

  function closePalette() {
    if (!open.value) {
      return
    }
    clearDebounce()
    abortSearch()
    open.value = false
    query.value = ''
    highlightIndex.value = -1
    resetCatalogResults()
    const restore = previousFocus
    previousFocus = null
    if (restore && document.contains(restore)) {
      restore.focus()
    }
  }

  function setQuery(value: string) {
    query.value = value
    highlightIndex.value = -1
  }

  async function runCatalogSearch(searchQuery: string) {
    abortSearch()
    const controller = new AbortController()
    searchAbort = controller
    const signal = controller.signal
    const routes = options.visibleRoutes.value
    const trimmed = searchQuery.trim()

    if (!trimmed) {
      resetCatalogResults()
      return
    }

    const tasks: Array<Promise<void>> = []
    loading.value = true

    if (canQueryCatalog(routes, ROUTE_KEYS.templateManagement)) {
      templateLoading.value = true
      templateErrorKey.value = null
      tasks.push(
        listTemplates(0, COMMAND_PALETTE_PAGE_SIZE, { search: trimmed, signal })
          .then((page) => {
            if (signal.aborted) {
              return
            }
            templateItems.value = page.content.map((row) => ({
              id: `template:${row.id}`,
              kind: 'template' as const,
              title: row.name,
              subtitle: [row.externalId, row.groupCode].filter(Boolean).join(' · '),
              target: { path: templatePackageHubPath(row.id) },
            }))
            templateErrorKey.value = null
          })
          .catch((error: unknown) => {
            if (signal.aborted) {
              return
            }
            templateItems.value = []
            templateErrorKey.value = resolveApiErrorMessageKey(
              error,
              'commandPalette.errors.templates',
            )
          })
          .finally(() => {
            if (!signal.aborted) {
              templateLoading.value = false
            }
          }),
      )
    } else {
      templateItems.value = []
      templateErrorKey.value = null
      templateLoading.value = false
    }

    if (canQueryCatalog(routes, ROUTE_KEYS.masterManagement)) {
      masterLoading.value = true
      masterErrorKey.value = null
      tasks.push(
        listMasters(0, COMMAND_PALETTE_PAGE_SIZE, { search: trimmed, signal })
          .then((page) => {
            if (signal.aborted) {
              return
            }
            masterItems.value = page.content.map((row) => ({
              id: `master:${row.id}`,
              kind: 'master' as const,
              title: row.name,
              subtitle: row.groupCode,
              target: { path: masterDetailPath(row.id) },
            }))
            masterErrorKey.value = null
          })
          .catch((error: unknown) => {
            if (signal.aborted) {
              return
            }
            masterItems.value = []
            masterErrorKey.value = resolveApiErrorMessageKey(error, 'commandPalette.errors.masters')
          })
          .finally(() => {
            if (!signal.aborted) {
              masterLoading.value = false
            }
          }),
      )
    } else {
      masterItems.value = []
      masterErrorKey.value = null
      masterLoading.value = false
    }

    if (canQueryCatalog(routes, ROUTE_KEYS.contentModuleManagement)) {
      contentModuleLoading.value = true
      contentModuleErrorKey.value = null
      tasks.push(
        listContentModules(0, COMMAND_PALETTE_PAGE_SIZE, { search: trimmed, signal })
          .then((page) => {
            if (signal.aborted) {
              return
            }
            contentModuleItems.value = page.content.map((row) => ({
              id: `content-module:${row.moduleId}`,
              kind: 'content-module' as const,
              title: row.name,
              subtitle: row.moduleCode,
              target: { path: contentModuleDetailPath(row.moduleId) },
            }))
            contentModuleErrorKey.value = null
          })
          .catch((error: unknown) => {
            if (signal.aborted) {
              return
            }
            contentModuleItems.value = []
            contentModuleErrorKey.value = resolveApiErrorMessageKey(
              error,
              'commandPalette.errors.contentModules',
            )
          })
          .finally(() => {
            if (!signal.aborted) {
              contentModuleLoading.value = false
            }
          }),
      )
    } else {
      contentModuleItems.value = []
      contentModuleErrorKey.value = null
      contentModuleLoading.value = false
    }

    await Promise.all(tasks)
    if (!signal.aborted) {
      loading.value = false
      if (flatItems.value.length > 0 && highlightIndex.value < 0) {
        highlightIndex.value = 0
      }
    }
  }

  function scheduleSearch() {
    clearDebounce()
    const trimmed = query.value.trim()
    if (!trimmed) {
      abortSearch()
      resetCatalogResults()
      return
    }
    debounceTimer = setTimeout(() => {
      debounceTimer = null
      void runCatalogSearch(query.value)
    }, COMMAND_PALETTE_DEBOUNCE_MS)
  }

  watch(query, () => {
    if (!open.value) {
      return
    }
    scheduleSearch()
  })

  function moveHighlight(delta: number) {
    highlightIndex.value = moveHighlightIndex(highlightIndex.value, delta, flatItems.value.length)
  }

  async function activateHighlighted() {
    const items = flatItems.value
    if (items.length === 0) {
      return
    }
    const index = highlightIndex.value < 0 ? 0 : highlightIndex.value
    const item = items[index]
    if (!item) {
      return
    }
    await activateItem(item)
  }

  async function activateItem(item: PaletteItem) {
    closePalette()
    await options.navigate(item.target)
  }

  function handleGlobalKeydown(event: KeyboardEvent) {
    const isModK =
      (event.key === 'k' || event.key === 'K') && (event.ctrlKey || event.metaKey) && !event.altKey
    if (isModK) {
      event.preventDefault()
      openPalette()
      return
    }
    if (!open.value) {
      return
    }
    if (event.key === 'Escape') {
      event.preventDefault()
      closePalette()
      return
    }
    if (event.key === 'ArrowDown') {
      event.preventDefault()
      moveHighlight(1)
      return
    }
    if (event.key === 'ArrowUp') {
      event.preventDefault()
      moveHighlight(-1)
      return
    }
    if (event.key === 'Enter') {
      event.preventDefault()
      void activateHighlighted()
    }
  }

  if (options.bindShortcut !== false) {
    onMounted(() => {
      document.addEventListener('keydown', handleGlobalKeydown)
    })
    onUnmounted(() => {
      document.removeEventListener('keydown', handleGlobalKeydown)
      clearDebounce()
      abortSearch()
    })
  }

  return {
    open,
    query,
    highlightIndex,
    groups,
    flatItems,
    loading,
    showNoMatch,
    hasAnyError,
    focusNonce,
    openPalette,
    closePalette,
    setQuery,
    moveHighlight,
    activateHighlighted,
    activateItem,
    runCatalogSearch,
    handleGlobalKeydown,
    COMMAND_PALETTE_PAGE_SIZE,
    COMMAND_PALETTE_DEBOUNCE_MS,
  }
}
