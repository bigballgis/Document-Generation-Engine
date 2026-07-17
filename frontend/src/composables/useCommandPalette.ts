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
  type PaletteItem,
  type UseCommandPaletteOptions,
} from '@/composables/commandPaletteTypes'
import {
  buildPaletteRouteItems,
  filterPaletteRouteItems,
  moveHighlightIndex,
} from '@/composables/commandPaletteHelpers'
import { useAuthoringEditorContextRef } from '@/composables/authoringEditorContext'
import { buildAuthoringPaletteActions } from '@/composables/buildAuthoringPaletteActions'
import { useCommandPaletteCatalog } from '@/composables/useCommandPaletteCatalog'
import { createCommandPaletteDerivedState } from '@/composables/createCommandPaletteDerivedState'

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

  const authoringContext = useAuthoringEditorContextRef()
  const allActionItems = computed(() =>
    buildAuthoringPaletteActions(authoringContext.value, options.translate),
  )
  const filteredActionItems = computed(() =>
    filterPaletteRouteItems(allActionItems.value, query.value),
  )

  const { groups, flatItems, hasAnyError, showNoMatch } = createCommandPaletteDerivedState({
    query,
    filteredRouteItems,
    filteredActionItems,
    catalog,
    visibleRoutes: () => options.visibleRoutes.value,
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
    if (item.kind === 'action') {
      await item.execute?.()
      return
    }
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
