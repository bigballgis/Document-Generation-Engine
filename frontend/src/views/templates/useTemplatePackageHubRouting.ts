import { onMounted, onUnmounted, ref, watch, type ComputedRef, type Ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  templateDevVersionPath,
  templatePackageHubPath,
} from '@/routing/routeKeys'
import { useTemplatesStore } from '@/stores/templates'
import { resolveLifecycleHubDeepLinkTarget } from '@/utils/templateJourneyWorkspaceLink'
import type { TemplateDevWorkspaceTab } from '@/views/templates/templateDevWorkspaceTabs'
import { resolveHubLegacyDeepLink } from '@/views/templates/hub/resolveHubLegacyDeepLink'

export interface UseTemplatePackageHubRoutingOptions {
  templateId: ComputedRef<string>
  template: ComputedRef<{
    id: string
    devVersionId?: string | null
    releaseVersion?: string | null
  } | null>
  showPolicyPanel: ComputedRef<boolean>
  loadPolicyData: () => Promise<void>
  loadFailed: Ref<boolean>
  propertiesOpen: Ref<boolean>
  dependenciesGuidanceVisible: Ref<boolean>
}

/** @deprecated Hub secondary tabs removed in Wave 2 — kept for test migration clarity. */
export function resolveHubSecondaryTab(value: unknown): undefined {
  void value
  return undefined
}

export function useTemplatePackageHubRouting(options: UseTemplatePackageHubRoutingOptions) {
  const {
    templateId,
    template,
    showPolicyPanel,
    loadPolicyData,
    loadFailed,
    propertiesOpen,
    dependenciesGuidanceVisible,
  } = options

  const route = useRoute()
  const router = useRouter()
  const templatesStore = useTemplatesStore()
  const deepLinkHandled = ref(false)

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

  async function applyLegacyHubDeepLinks() {
    if (
      route.query.tab === 'authoring' ||
      route.query.tab === 'lifecycle' ||
      route.query.focus === 'lifecycle'
    ) {
      return
    }

    const result = resolveHubLegacyDeepLink({
      templateId: templateId.value,
      query: route.query,
      hash: route.hash,
      preferredReleaseVersion: template.value?.releaseVersion,
      preferredDevVersionId: template.value?.devVersionId,
    })

    if (result.kind === 'apiSettings') {
      await router.replace(result.path)
      return
    }

    if (result.kind === 'properties') {
      propertiesOpen.value = true
      const query = { ...route.query }
      delete query.tab
      delete query.focus
      await router.replace({ path: route.path, query, hash: '' })
      return
    }

    if (result.kind === 'dependencies') {
      if (result.path) {
        await router.replace(result.path)
        return
      }
      dependenciesGuidanceVisible.value = true
      const query = { ...route.query }
      delete query.tab
      delete query.focus
      await router.replace({ path: route.path, query, hash: '' })
      return
    }

    if (result.kind === 'stripTab' || route.query.tab) {
      const query = { ...route.query }
      delete query.tab
      delete query.focus
      if (Object.keys(query).length !== Object.keys(route.query).length || route.hash) {
        await router.replace({ path: route.path, query, hash: '' })
      }
    }
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
    if (route.query.tab === 'authoring') {
      void redirectAuthoringDeepLink()
      return
    }
    if (route.query.tab === 'lifecycle' || route.query.focus === 'lifecycle') {
      void redirectLifecycleDeepLink()
      return
    }
    await loadTemplate()
    await applyLegacyHubDeepLinks()
    deepLinkHandled.value = true
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
    () => [route.query, route.hash] as const,
    async () => {
      if (!deepLinkHandled.value) {
        return
      }
      if (
        route.query.tab === 'authoring' ||
        route.query.tab === 'lifecycle' ||
        route.query.focus === 'lifecycle'
      ) {
        return
      }
      await applyLegacyHubDeepLinks()
    },
    { deep: true },
  )

  return {
    openDevEditor,
    loadTemplate,
  }
}
