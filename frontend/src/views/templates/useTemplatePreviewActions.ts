import { ref, type ComputedRef } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { useTemplatesStore } from '@/stores/templates'
import * as templatesApi from '@/api/templates'
import type { PreviewRecord } from '@/types/template'
import type { TemplateDevWorkspaceTab } from '@/views/templates/templateDevWorkspaceTabs'

export interface UseTemplatePreviewActionsOptions {
  templateId: ComputedRef<string>
  errorMessage: ComputedRef<string>
  openDevWorkspaceTab: (tab: TemplateDevWorkspaceTab) => void
}

export function useTemplatePreviewActions(options: UseTemplatePreviewActionsOptions) {
  const { templateId, errorMessage, openDevWorkspaceTab } = options

  const { t } = useI18n()
  const templatesStore = useTemplatesStore()

  const lastPreview = ref<PreviewRecord | null>(null)
  const selectedPreviewId = ref<string | null>(null)
  const selectedTestDataSetId = ref<string | null>(null)
  const generatingPreview = ref(false)
  const generatingPreviewId = ref<string | null>(null)
  const coverageRefreshToken = ref(0)

  async function handleTestGenerate(testDataSetId?: string) {
    const resolvedId = testDataSetId ?? selectedTestDataSetId.value ?? undefined
    generatingPreview.value = true
    generatingPreviewId.value = resolvedId ?? null
    try {
      const preview = await templatesStore.testGenerate(templateId.value, {
        testDataSetId: resolvedId,
      })
      lastPreview.value = preview
      selectedPreviewId.value = preview.previewId
      if (resolvedId) {
        selectedTestDataSetId.value = resolvedId
      }
      openDevWorkspaceTab('testing')
      ElMessage.success(t('templates.testGenerate.success', { previewId: preview.previewId }))
    } catch {
      ElMessage.error(errorMessage.value || t('templates.error.testGenerate'))
    } finally {
      generatingPreview.value = false
      generatingPreviewId.value = null
    }
  }

  /** CE-U18: bump coverage/history refresh after async batch completes — never sync batch. */
  function bumpCoverageRefresh() {
    coverageRefreshToken.value += 1
  }

  async function handlePreviewSelected(previewId: string | null) {
    selectedPreviewId.value = previewId
    if (!previewId) {
      lastPreview.value = null
      return
    }
    try {
      lastPreview.value = await templatesApi.getPreview(templateId.value, previewId)
    } catch {
      ElMessage.error(errorMessage.value || t('templates.previewHistory.error.loadDetail'))
    }
  }

  function handlePreviewRefreshed(preview: PreviewRecord) {
    lastPreview.value = preview
    selectedPreviewId.value = preview.previewId
  }

  return {
    lastPreview,
    selectedPreviewId,
    selectedTestDataSetId,
    generatingPreview,
    generatingPreviewId,
    coverageRefreshToken,
    handleTestGenerate,
    bumpCoverageRefresh,
    handlePreviewSelected,
    handlePreviewRefreshed,
  }
}
