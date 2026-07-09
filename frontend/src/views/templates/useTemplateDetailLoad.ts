import { ref, watch, type ComputedRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { canViewCollaborationWorkItems } from '@/auth/roles'
import { useCapabilities } from '@/composables/useCapabilities'
import { useCollaborationStore } from '@/stores/collaboration'
import { ROUTE_PATH_BY_KEY, ROUTE_KEYS, templatePackageHubPath } from '@/routing/routeKeys'
import { useTemplatesStore } from '@/stores/templates'
import * as templatesApi from '@/api/templates'
import {
  resolveTemplateDetailTabFromQuery,
  type TemplateDetailTab,
} from '@/views/templates/templateDetailTabs'
import {
  resolveTemplateDevWorkspaceTabFromQuery,
  type TemplateDevWorkspaceTab,
} from '@/views/templates/templateDevWorkspaceTabs'
import type { PreviewRecord } from '@/types/template'
import type { Ref } from 'vue'

export interface TemplateDetailLoadPolicyDeps {
  showPolicyPanel: ComputedRef<boolean>
  loadPolicyData: () => Promise<void>
  resetPolicyCredentialsTransientState: () => void
}

export interface UseTemplateDetailLoadOptions {
  isDevEditor: ComputedRef<boolean>
  templateId: ComputedRef<string>
  devVersionId: ComputedRef<string>
  activeDetailTab: Ref<TemplateDetailTab>
  activeDevWorkspaceTab: Ref<TemplateDevWorkspaceTab>
  loadTemplateHolder: { fn: () => Promise<void> }
  policy: TemplateDetailLoadPolicyDeps
  lastPreview: Ref<PreviewRecord | null>
  resetLifecycleTransientState: () => void
  resetJourneyEvidenceState: () => void
}

export function useTemplateDetailLoad(options: UseTemplateDetailLoadOptions) {
  const {
    isDevEditor,
    templateId,
    devVersionId,
    activeDetailTab,
    activeDevWorkspaceTab,
    loadTemplateHolder,
    policy,
    lastPreview,
    resetLifecycleTransientState,
    resetJourneyEvidenceState,
  } = options

  const route = useRoute()
  const router = useRouter()
  const templatesStore = useTemplatesStore()
  const collaborationStore = useCollaborationStore()
  const { authorTemplates, context } = useCapabilities()

  const loadFailed = ref(false)

  async function loadAuthorRemediationWorkItems() {
    if (!authorTemplates.value || !canViewCollaborationWorkItems(context.value)) {
      return
    }
    try {
      await collaborationStore.fetchWorkItems({ queue: 'REMEDIATION' })
    } catch {
      /* degrade — remediation flag may be false until work items load */
    }
  }

  async function loadTemplate() {
    loadFailed.value = false
    try {
      if (isDevEditor.value && devVersionId.value) {
        const detail = await templatesApi.fetchDevVersionDetail(templateId.value, devVersionId.value)
        templatesStore.$patch({ selectedTemplate: detail })
      } else {
        await templatesStore.fetchTemplate(templateId.value)
      }
      if (policy.showPolicyPanel.value) {
        await policy.loadPolicyData()
      }
    } catch {
      loadFailed.value = true
    }
  }

  loadTemplateHolder.fn = loadTemplate

  function backToList() {
    if (isDevEditor.value) {
      router.push(templatePackageHubPath(templateId.value))
      return
    }
    router.push(ROUTE_PATH_BY_KEY[ROUTE_KEYS.templateManagement])
  }

  function resetTransientDetailState() {
    resetJourneyEvidenceState()
    resetLifecycleTransientState()
    policy.resetPolicyCredentialsTransientState()
    lastPreview.value = null
  }

  watch(
    () => devVersionId.value,
    () => {
      if (isDevEditor.value) {
        resetTransientDetailState()
        void loadTemplate()
      }
    },
  )

  watch(
    () => templateId.value,
    () => {
      resetTransientDetailState()
      activeDetailTab.value = isDevEditor.value
        ? 'authoring'
        : resolveTemplateDetailTabFromQuery(route.query)
      if (isDevEditor.value) {
        activeDevWorkspaceTab.value = resolveTemplateDevWorkspaceTabFromQuery(route.query)
      }
      void loadTemplate()
    },
  )

  return {
    loadFailed,
    loadTemplate,
    backToList,
    resetTransientDetailState,
    loadAuthorRemediationWorkItems,
  }
}
