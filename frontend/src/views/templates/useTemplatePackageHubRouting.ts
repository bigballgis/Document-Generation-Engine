import { onMounted, onUnmounted, watch, type ComputedRef, type Ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  templateDevVersionPath,
  templatePackageHubPath,
} from '@/routing/routeKeys'
import { useTemplatesStore } from '@/stores/templates'
import { resolveLifecycleHubDeepLinkTarget } from '@/utils/templateJourneyWorkspaceLink'
import type { TemplateDevWorkspaceTab } from '@/views/templates/templateDevWorkspaceTabs'

const HUB_SECONDARY_TABS = ['overview', 'apiAccess'] as const
export type HubSecondaryTab = (typeof HUB_SECONDARY_TABS)[number]

export interface UseTemplatePackageHubRoutingOptions {
  templateId: ComputedRef<string>
  template: ComputedRef<{ id: string; devVersionId?: string | null } | null>
  secondaryTab: Ref<HubSecondaryTab | undefined>
  showPolicyPanel: ComputedRef<boolean>
  loadPolicyData: () => Promise<void>
  loadFailed: Ref<boolean>
}

export function resolveHubSecondaryTab(value: unknown): HubSecondaryTab | undefined {
  if (typeof value === 'string' && (HUB_SECONDARY_TABS as readonly string[]).includes(value)) {
    return value as HubSecondaryTab
  }
  return undefined
}

export function useTemplatePackageHubRouting(options: UseTemplatePackageHubRoutingOptions) {
  const { templateId, template, secondaryTab, showPolicyPanel, loadPolicyData, loadFailed } =
    options

  const route = useRoute()
  const router = useRouter()
  const templatesStore = useTemplatesStore()

  function openDevEditor(
    workspaceTab: TemplateDevWorkspaceTab = 'design',
    extraQuery?: Record<string, string>,
  ) {
    const devVersionId = template.value?.devVersionId
    if (!devVersionId) {
      return
    }
    router.push(
      templateDevVersionPath(templateId.value, devVersionId, undefined, {
        workspaceTab,
        ...extraQuery,
      }),
    )
  }

  async function redirectLifecycleDeepLink() {
    try {
      if (!template.value) {
        await templatesStore.fetchTemplate(templateId.value)
      }
      const target = resolveLifecycleHubDeepLinkTarget(route.query)
      openDevEditor(target.workspaceTab, target.extraQuery)
    } catch {
      await router.replace(templatePackageHubPath(templateId.value))
    }
  }

  async function redirectAuthoringDeepLink() {
    try {
      if (!template.value) {
        await templatesStore.fetchTemplate(templateId.value)
      }
      const devVersionId = templatesStore.selectedTemplate?.devVersionId
      if (devVersionId) {
        await router.replace(templateDevVersionPath(templateId.value, devVersionId))
      }
    } catch {
      await router.replace(templatePackageHubPath(templateId.value))
    }
  }

  function syncSecondaryTabFromRoute() {
    if (route.query.tab === 'authoring') {
      void redirectAuthoringDeepLink()
      return
    }
    if (route.query.tab === 'lifecycle' || route.query.focus === 'lifecycle') {
      void redirectLifecycleDeepLink()
      return
    }
    if (route.query.tab === 'releaseVersions') {
      secondaryTab.value = undefined
      void router.replace(templatePackageHubPath(templateId.value))
      return
    }
    secondaryTab.value = resolveHubSecondaryTab(route.query.tab)
  }

  async function loadTemplate() {
    loadFailed.value = false
    try {
      await templatesStore.fetchTemplate(templateId.value)
      if (showPolicyPanel.value) {
        await loadPolicyData()
      }
    } catch {
      loadFailed.value = true
    }
  }

  onMounted(async () => {
    syncSecondaryTabFromRoute()
    if (
      route.query.tab === 'authoring' ||
      route.query.tab === 'lifecycle' ||
      route.query.focus === 'lifecycle'
    ) {
      return
    }
    await loadTemplate()
  })

  onUnmounted(() => {
    templatesStore.clearSelected()
  })

  watch(
    () => templateId.value,
    () => {
      void loadTemplate()
    },
  )

  watch(
    () => route.query,
    () => {
      syncSecondaryTabFromRoute()
    },
    { deep: true },
  )

  watch(secondaryTab, (tab) => {
    const queryTab = resolveHubSecondaryTab(route.query.tab)
    if (queryTab === tab) {
      return
    }
    if (!tab) {
      const query = { ...route.query }
      delete query.tab
      delete query.focus
      void router.replace({ query })
      return
    }
    void router.replace({ query: { ...route.query, tab } })
  })

  return {
    openDevEditor,
    loadTemplate,
  }
}
