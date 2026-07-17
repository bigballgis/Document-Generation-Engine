import { computed, type Ref } from 'vue'
import type { PaletteGroupView } from '@/composables/commandPaletteTypes'
import { canQueryCatalog, filterPaletteRouteItems } from '@/composables/commandPaletteHelpers'
import type { useCommandPaletteCatalog } from '@/composables/useCommandPaletteCatalog'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import type { PaletteItem } from '@/composables/commandPaletteTypes'

type CatalogApi = ReturnType<typeof useCommandPaletteCatalog>

export function createCommandPaletteDerivedState(options: {
  query: Ref<string>
  filteredRouteItems: Ref<PaletteItem[]>
  /** CE-U17 — author Actions (empty when no bindings edit surface). */
  filteredActionItems?: Ref<PaletteItem[]>
  catalog: CatalogApi
  visibleRoutes: () => readonly string[]
}) {
  const { query, filteredRouteItems, catalog, visibleRoutes } = options
  const filteredActionItems = options.filteredActionItems

  const groups = computed((): PaletteGroupView[] => {
    const trimmed = query.value.trim()
    const result: PaletteGroupView[] = []
    const actions = filteredActionItems?.value ?? []
    if (actions.length > 0) {
      result.push({
        id: 'actions',
        labelKey: 'commandPalette.groups.actions',
        items: actions,
        errorMessageKey: null,
        loading: false,
      })
    }
    result.push({
      id: 'routes',
      labelKey: 'commandPalette.groups.routes',
      items: filteredRouteItems.value,
      errorMessageKey: null,
      loading: false,
    })
    if (trimmed) {
      if (canQueryCatalog(visibleRoutes(), ROUTE_KEYS.templateManagement)) {
        result.push({
          id: 'templates',
          labelKey: 'commandPalette.groups.templates',
          items: catalog.templateItems.value,
          errorMessageKey: catalog.templateErrorKey.value,
          loading: catalog.templateLoading.value,
        })
      }
      if (canQueryCatalog(visibleRoutes(), ROUTE_KEYS.masterManagement)) {
        result.push({
          id: 'masters',
          labelKey: 'commandPalette.groups.masters',
          items: catalog.masterItems.value,
          errorMessageKey: catalog.masterErrorKey.value,
          loading: catalog.masterLoading.value,
        })
      }
      if (canQueryCatalog(visibleRoutes(), ROUTE_KEYS.contentModuleManagement)) {
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

  return { groups, flatItems, hasAnyError, showNoMatch }
}

export { filterPaletteRouteItems }
