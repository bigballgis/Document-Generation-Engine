import { computed } from 'vue'
import type { RouteLocationRaw } from 'vue-router'
import {
  ROUTE_KEYS,
  contentModuleDetailPath,
  masterDetailPath,
  templateDetailPath,
} from '@/routing/routeKeys'
import { useSessionStore } from '@/stores/session'

export function useEntityLinkTargets() {
  const sessionStore = useSessionStore()

  const canLinkTemplates = computed(() =>
    sessionStore.canAccessRoute(ROUTE_KEYS.templateManagement),
  )
  const canLinkMasters = computed(() => sessionStore.canAccessRoute(ROUTE_KEYS.masterManagement))
  const canLinkContentModules = computed(() =>
    sessionStore.canAccessRoute(ROUTE_KEYS.contentModuleManagement),
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

  return {
    canLinkTemplates,
    canLinkMasters,
    canLinkContentModules,
    templateDetailLink,
    masterDetailLink,
    contentModuleDetailLink,
  }
}
