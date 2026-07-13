import type { ComputedRef, Ref } from 'vue'
import type { Router } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ROUTE_PATH_BY_KEY, ROUTE_KEYS } from '@/routing/routeKeys'
import type { useTemplatesStore } from '@/stores/templates'
import type { DeleteTemplatePayload } from '@/types/template'

type ConfirmAction = (options: {
  titleKey: string
  messageKey: string
  type: 'warning'
}) => Promise<boolean>

export function createTemplatePackageHubActions(deps: {
  t: (key: string) => string
  router: Router
  templatesStore: ReturnType<typeof useTemplatesStore>
  templateId: ComputedRef<string>
  errorMessage: ComputedRef<string>
  metadataEditOpen: Ref<boolean>
  confirmAction: ConfirmAction
  loadTemplate: () => Promise<void>
  workspaceRef: Ref<{ reloadVersionLines: () => Promise<void> | undefined } | null>
}) {
  const {
    t,
    router,
    templatesStore,
    templateId,
    errorMessage,
    metadataEditOpen,
    confirmAction,
    loadTemplate,
    workspaceRef,
  } = deps

  function backToList() {
    router.push(ROUTE_PATH_BY_KEY[ROUTE_KEYS.templateManagement])
  }

  async function handleMetadataUpdate(payload: { name: string; description: string | null }) {
    try {
      await templatesStore.updateTemplateMetadata(templateId.value, payload)
      metadataEditOpen.value = false
      ElMessage.success(t('templates.metadata.success'))
    } catch {
      ElMessage.error(errorMessage.value || t('templates.error.updateMetadata'))
    }
  }

  async function handleDeleteTemplate() {
    let reason = ''
    try {
      const result = await ElMessageBox.prompt(
        t('templates.deleteAction.reasonPrompt'),
        t('templates.deleteAction.title'),
        {
          confirmButtonText: t('common.confirm'),
          cancelButtonText: t('common.cancel'),
          inputValidator: (value) =>
            value.trim().length > 0 ? true : t('templates.deleteAction.reasonRequired'),
        },
      )
      reason = result.value.trim()
    } catch {
      return
    }

    const confirmed = await confirmAction({
      titleKey: 'templates.deleteAction.confirmTitle',
      messageKey: 'templates.deleteAction.confirmMessage',
      type: 'warning',
    })
    if (!confirmed) {
      return
    }

    try {
      const payload: DeleteTemplatePayload = { reason }
      await templatesStore.deleteTemplate(templateId.value, payload)
      ElMessage.success(t('templates.deleteAction.success'))
      router.push(ROUTE_PATH_BY_KEY[ROUTE_KEYS.templateManagement])
    } catch {
      ElMessage.error(errorMessage.value || t('templates.error.delete'))
    }
  }

  async function handleVersionLinesChanged() {
    await loadTemplate()
    await workspaceRef.value?.reloadVersionLines()
  }

  return {
    backToList,
    handleMetadataUpdate,
    handleDeleteTemplate,
    handleVersionLinesChanged,
  }
}
