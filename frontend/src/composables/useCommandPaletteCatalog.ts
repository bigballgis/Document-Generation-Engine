import { ref, type Ref } from 'vue'
import { listContentModules } from '@/api/contentModules'
import { listMasters } from '@/api/masters'
import { listTemplates } from '@/api/templates'
import { resolveApiErrorMessageKey } from '@/api/errorEnvelope'
import {
  COMMAND_PALETTE_DEBOUNCE_MS,
  COMMAND_PALETTE_PAGE_SIZE,
  type PaletteItem,
} from '@/composables/commandPaletteTypes'
import { canQueryCatalog } from '@/composables/commandPaletteHelpers'
import {
  contentModuleDetailPath,
  masterDetailPath,
  ROUTE_KEYS,
  templatePackageHubPath,
} from '@/routing/routeKeys'

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
