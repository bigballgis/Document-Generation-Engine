import { ref, type Ref } from 'vue'
import {
  COMMAND_PALETTE_DEBOUNCE_MS,
  type PaletteItem,
} from '@/composables/commandPaletteTypes'
import { createCommandPaletteCatalogSearchTasks } from '@/composables/createCommandPaletteCatalogSearchTasks'

export interface CommandPaletteCatalogApi {
  loading: Ref<boolean>
  templateItems: Ref<PaletteItem[]>
  masterItems: Ref<PaletteItem[]>
  contentModuleItems: Ref<PaletteItem[]>
  templateErrorKey: Ref<string | null>
  masterErrorKey: Ref<string | null>
  contentModuleErrorKey: Ref<string | null>
  templateLoading: Ref<boolean>
  masterLoading: Ref<boolean>
  contentModuleLoading: Ref<boolean>
  clearDebounce: () => void
  abortSearch: () => void
  resetCatalogResults: () => void
  runCatalogSearch: (searchQuery: string) => Promise<void>
  scheduleSearch: (query: string) => void
}

export function useCommandPaletteCatalog(options: {
  visibleRoutes: () => readonly string[]
  onSearchSettled: () => void
}): CommandPaletteCatalogApi {
  const loading = ref(false)
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

  async function runCatalogSearch(searchQuery: string) {
    abortSearch()
    const controller = new AbortController()
    searchAbort = controller
    const signal = controller.signal
    const routes = options.visibleRoutes()
    const trimmed = searchQuery.trim()

    if (!trimmed) {
      resetCatalogResults()
      return
    }

    loading.value = true
    const tasks = createCommandPaletteCatalogSearchTasks({
      routes,
      trimmed,
      signal,
      templateItems,
      masterItems,
      contentModuleItems,
      templateErrorKey,
      masterErrorKey,
      contentModuleErrorKey,
      templateLoading,
      masterLoading,
      contentModuleLoading,
    })

    await Promise.all(tasks)
    if (!signal.aborted) {
      loading.value = false
      options.onSearchSettled()
    }
  }

  function scheduleSearch(query: string) {
    clearDebounce()
    const trimmed = query.trim()
    if (!trimmed) {
      abortSearch()
      resetCatalogResults()
      return
    }
    debounceTimer = setTimeout(() => {
      debounceTimer = null
      void runCatalogSearch(query)
    }, COMMAND_PALETTE_DEBOUNCE_MS)
  }

  return {
    loading,
    templateItems,
    masterItems,
    contentModuleItems,
    templateErrorKey,
    masterErrorKey,
    contentModuleErrorKey,
    templateLoading,
    masterLoading,
    contentModuleLoading,
    clearDebounce,
    abortSearch,
    resetCatalogResults,
    runCatalogSearch,
    scheduleSearch,
  }
}
