import {
  computed,
  onMounted,
  onUnmounted,
  ref,
  watch,
} from 'vue'
import {
  COMMAND_PALETTE_DEBOUNCE_MS,
  COMMAND_PALETTE_PAGE_SIZE,
  type PaletteGroupView,
  type PaletteItem,
  type UseCommandPaletteOptions,
} from '@/composables/commandPaletteTypes'
import {
  buildPaletteRouteItems,
  canQueryCatalog,
  filterPaletteRouteItems,
  moveHighlightIndex,
} from '@/composables/commandPaletteHelpers'
import { useCommandPaletteCatalog } from '@/composables/useCommandPaletteCatalog'
import { ROUTE_KEYS } from '@/routing/routeKeys'

export {
  COMMAND_PALETTE_DEBOUNCE_MS,
  COMMAND_PALETTE_PAGE_SIZE,
  type PaletteGroupView,
  type PaletteItem,
  type PaletteItemKind,
  type PaletteNavTarget,
  type UseCommandPaletteOptions,
} from '@/composables/commandPaletteTypes'

export {
  buildPaletteRouteItems,
  filterPaletteRouteItems,
  moveHighlightIndex,
} from '@/composables/commandPaletteHelpers'

export function useCommandPalette(options: UseCommandPaletteOptions) {
  const open = ref(false)
  const query = ref('')
  const highlightIndex = ref(-1)
  const focusNonce = ref(0)
  let previousFocus: HTMLElement | null = null

  const catalog = useCommandPaletteCatalog({
    visibleRoutes: () => options.visibleRoutes.value,
    onSearchSettled: () => {
      if (flatItems.value.length > 0 && highlightIndex.value < 0) {
        highlightIndex.value = 0
      }
    },
  })

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
          items: catalog.templateItems.value,
          errorMessageKey: catalog.templateErrorKey.value,
          loading: catalog.templateLoading.value,
        })
      }
      if (canQueryCatalog(options.visibleRoutes.value, ROUTE_KEYS.masterManagement)) {
        result.push({
          id: 'masters',
          labelKey: 'commandPalette.groups.masters',
          items: catalog.masterItems.value,
          errorMessageKey: catalog.masterErrorKey.value,
          loading: catalog.masterLoading.value,
        })
      }
      if (canQueryCatalog(options.visibleRoutes.value, ROUTE_KEYS.contentModuleManagement)) {
        result.push({
          id: 'content-modules',
          labelKey: 'commandPalette.groups.contentModules',
          items: catalog.contentModuleItems.value,
          errorMessageKey: catalog.contentModuleErrorKey.value,
          loading: catalog.contentModuleLoading.value,
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
    if (catalog.loading.value || groups.value.some((g) => g.loading)) {
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

  function openPalette() {
    if (!open.value) {
      const active = document.activeElement
      previousFocus = active instanceof HTMLElement ? active : null
      open.value = true
      query.value = ''
      highlightIndex.value = -1
      catalog.resetCatalogResults()
    }
    // Already open: keep open and bump focus nonce so UI refocuses input (C6-C2).
    focusNonce.value += 1
  }

  function closePalette() {
    if (!open.value) {
      return
    }
    catalog.clearDebounce()
    catalog.abortSearch()
    open.value = false
    query.value = ''
    highlightIndex.value = -1
    catalog.resetCatalogResults()
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

  watch(query, () => {
    if (!open.value) {
      return
    }
    catalog.scheduleSearch(query.value)
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
      catalog.clearDebounce()
      catalog.abortSearch()
    })
  }

  return {
    open,
    query,
    highlightIndex,
    groups,
    flatItems,
    loading: catalog.loading,
    showNoMatch,
    hasAnyError,
    focusNonce,
    openPalette,
    closePalette,
    setQuery,
    moveHighlight,
    activateHighlighted,
    activateItem,
    runCatalogSearch: catalog.runCatalogSearch,
    handleGlobalKeydown,
    COMMAND_PALETTE_PAGE_SIZE,
    COMMAND_PALETTE_DEBOUNCE_MS,
  }
}
