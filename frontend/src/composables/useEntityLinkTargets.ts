import { computed } from 'vue'
import type { RouteLocationRaw } from 'vue-router'
import {
  ROUTE_KEYS,
  contentModuleDetailPath,
  masterDetailPath,
  templateDetailPath,
} from '@/routing/routeKeys'
import { useSessionStore } from '@/stores/session'

export const GROUPS_CATALOG_PATH = '/entitlement/groups'

export function useEntityLinkTargets() {
  const sessionStore = useSessionStore()

  const canLinkTemplates = computed(() =>
    sessionStore.canAccessRoute(ROUTE_KEYS.templateManagement),
  )
  const canLinkMasters = computed(() => sessionStore.canAccessRoute(ROUTE_KEYS.masterManagement))
  const canLinkContentModules = computed(() =>
    sessionStore.canAccessRoute(ROUTE_KEYS.contentModuleManagement),
  )
  const canLinkGroups = computed(() =>
    sessionStore.canAccessRoute(ROUTE_KEYS.identityAdministration),
  )

  function templateDetailLink(templateId: string): RouteLocationRaw | undefined {
    return canLinkTemplates.value ? templateDetailPath(templateId) : undefined
  }

  function masterDetailLink(masterId: string): RouteLocationRaw | undefined {
    return canLinkMasters.value ? masterDetailPath(masterId) : undefined
  }

  function contentModuleDetailLink(moduleId: string): RouteLocationRaw | undefined {
    return canLinkContentModules.value ? contentModuleDetailPath(moduleId) : undefined
  }

  /**
   * Groups catalog link (fail-closed). Optional `q` prefill for search.
   * Wildcard `*` is never linked (not a real group code).
   */
  function groupCatalogLink(groupCode?: string | null): RouteLocationRaw | undefined {
    if (!canLinkGroups.value) {
      return undefined
    }
    const code = groupCode?.trim()
    if (!code || code === '*') {
      return code === '*' ? undefined : GROUPS_CATALOG_PATH
    }
    return { path: GROUPS_CATALOG_PATH, query: { q: code } }
  }

  /** Task hub Item cell — gate by entity domain before linking to task.path (N1). */
  function taskEntityLink(task: {
    path: string
    source?: 'master' | 'collaboration' | 'template' | 'content-module'
  }): RouteLocationRaw | undefined {
    if (!task.path) {
      return undefined
    }
    switch (task.source) {
      case 'master':
        return canLinkMasters.value ? task.path : undefined
      case 'content-module':
        return canLinkContentModules.value ? task.path : undefined
      case 'template':
      case 'collaboration':
        return canLinkTemplates.value ? task.path : undefined
      default:
        return canLinkTemplates.value || canLinkMasters.value || canLinkContentModules.value
          ? task.path
          : undefined
    }
  }

  return {
    canLinkTemplates,
    canLinkMasters,
    canLinkContentModules,
    canLinkGroups,
    templateDetailLink,
    masterDetailLink,
    contentModuleDetailLink,
    groupCatalogLink,
    taskEntityLink,
  }
}
