import { computed, nextTick, watch, type ComputedRef, type Ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  normalizeTemplateDetailQuery,
  resolveTemplateDetailTab,
  resolveTemplateDetailTabFromQuery,
  templateDetailTabLabelKey,
  type TemplateDetailTab,
} from '@/views/templates/templateDetailTabs'
import {
  buildDevWorkspaceQuery,
  resolveTemplateDevWorkspaceTabFromQuery,
  type TemplateDevWorkspaceTab,
} from '@/views/templates/templateDevWorkspaceTabs'

export interface UseTemplateDetailTabsOptions {
  isDevEditor: ComputedRef<boolean>
  showAuthoringSection: ComputedRef<boolean>
  showPolicyPanel: ComputedRef<boolean>
  activeDetailTab: Ref<TemplateDetailTab>
  activeDevWorkspaceTab: Ref<TemplateDevWorkspaceTab>
}

export function useTemplateDetailTabs(options: UseTemplateDetailTabsOptions) {
  const { isDevEditor, showAuthoringSection, showPolicyPanel, activeDetailTab, activeDevWorkspaceTab } =
    options

  const route = useRoute()
  const router = useRouter()

  const detailTabs = computed(() => {
    const tabs = [
      { name: 'overview', labelKey: templateDetailTabLabelKey('overview') },
      { name: 'lifecycle', labelKey: templateDetailTabLabelKey('lifecycle') },
    ] as Array<{ name: string; labelKey: string }>
    if (showAuthoringSection.value) {
      tabs.push({ name: 'authoring', labelKey: templateDetailTabLabelKey('authoring') })
    }
    tabs.push({ name: 'releaseVersions', labelKey: templateDetailTabLabelKey('releaseVersions') })
    if (showPolicyPanel.value) {
      tabs.push({ name: 'apiAccess', labelKey: templateDetailTabLabelKey('apiAccess') })
    }
    return tabs
  })

  function scrollToLifecyclePanel() {
    void nextTick(() => {
      document.getElementById('template-lifecycle-panel')?.scrollIntoView({
        behavior: 'smooth',
        block: 'start',
      })
    })
  }

  function scrollToDevWorkspace() {
    void nextTick(() => {
      document.getElementById('dev-workspace')?.scrollIntoView({
        behavior: 'smooth',
        block: 'start',
      })
    })
  }

  function openDevWorkspaceTab(tab: TemplateDevWorkspaceTab) {
    activeDevWorkspaceTab.value = tab
    void router.replace({ query: buildDevWorkspaceQuery(route.query, tab) })
    scrollToDevWorkspace()
  }

  function openLifecyclePanel() {
    if (isDevEditor.value) {
      openDevWorkspaceTab('approval')
      return
    }
    activeDetailTab.value = 'lifecycle'
    scrollToLifecyclePanel()
  }

  function openTestPreviewTab() {
    openDevWorkspaceTab('testing')
  }

  function syncTabFromRoute() {
    const normalized = normalizeTemplateDetailQuery(route.query)
    if (normalized) {
      if (isDevEditor.value) {
        activeDevWorkspaceTab.value = 'approval'
        scrollToDevWorkspace()
        void router.replace({
          query: buildDevWorkspaceQuery(normalized.query, 'approval'),
        })
        return
      }
      activeDetailTab.value = normalized.tab
      scrollToLifecyclePanel()
      void router.replace({ query: normalized.query })
      return
    }

    if (isDevEditor.value) {
      const workspaceTab = resolveTemplateDevWorkspaceTabFromQuery(route.query)
      if (activeDevWorkspaceTab.value !== workspaceTab) {
        activeDevWorkspaceTab.value = workspaceTab
      }
      if (route.query.focus === 'workflow') {
        scrollToDevWorkspace()
      }
      return
    }

    const tab = resolveTemplateDetailTabFromQuery(route.query)
    if (activeDetailTab.value !== tab) {
      activeDetailTab.value = tab
    }
  }

  watch(
    () => route.query,
    () => {
      syncTabFromRoute()
    },
    { deep: true },
  )

  watch(activeDetailTab, (tab) => {
    if (route.query.focus === 'lifecycle' || route.query.focus === 'workflow') {
      return
    }
    if (isDevEditor.value && tab === 'lifecycle') {
      return
    }
    if (resolveTemplateDetailTab(route.query.tab) === tab) {
      return
    }
    const query = { ...route.query }
    delete query.focus
    router.replace({ query: { ...query, tab } })
  })

  return {
    detailTabs,
    syncTabFromRoute,
    openDevWorkspaceTab,
    openLifecyclePanel,
    openTestPreviewTab,
    scrollToLifecyclePanel,
    scrollToDevWorkspace,
  }
}
