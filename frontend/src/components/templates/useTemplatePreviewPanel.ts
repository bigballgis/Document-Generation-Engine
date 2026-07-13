import { computed, ref, toValue, watch, type MaybeRefOrGetter } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { useTemplatePanelDataStore } from '@/stores/templatePanelData'
import { downloadBlobExport } from '@/utils/downloadExport'
import type { PreviewComparisonItem, PreviewRecord } from '@/types/template'

export function useTemplatePreviewPanel(options: {
  templateId: MaybeRefOrGetter<string>
  preview: MaybeRefOrGetter<PreviewRecord | null>
}) {
  const { t, te } = useI18n()
  const panelDataStore = useTemplatePanelDataStore()
  const downloadingFormat = ref<'docx' | 'pdf' | null>(null)
  const latestPreview = ref<PreviewRecord | null>(toValue(options.preview))

  watch(
    () => toValue(options.preview),
    (value) => {
      latestPreview.value = value
    },
  )

  const loading = computed(() => {
    const previewId = latestPreview.value?.previewId
    if (!previewId) {
      return false
    }
    return panelDataStore.getEntry(toValue(options.templateId)).loadingPreviewById[previewId] ?? false
  })

  const comparisonItems = computed<PreviewComparisonItem[]>(
    () => latestPreview.value?.previewComparison?.items ?? [],
  )

  const comparisonSummary = computed(() => {
    const comparison = latestPreview.value?.previewComparison
    if (!comparison) {
      return null
    }
    return t('templates.preview.comparisonCounts', {
      total: comparison.totalDiffCount,
      blockers: comparison.blockerCount,
      warnings: comparison.warningCount,
    })
  })

  async function refreshPreview() {
    if (!latestPreview.value?.previewId) {
      return
    }
    try {
      latestPreview.value = await panelDataStore.fetchPreview(
        toValue(options.templateId),
        latestPreview.value.previewId,
      )
    } catch {
      ElMessage.error(t('templates.previewHistory.error.load'))
    }
  }

  function severityTagType(severity: string): 'danger' | 'warning' {
    return severity === 'BLOCKER' ? 'danger' : 'warning'
  }

  function locationLabel(locationType: string): string {
    const key = `templates.preview.locationTypes.${locationType}`
    return te(key) ? t(key) : locationType
  }

  const previewArtifact = computed(() => latestPreview.value?.artifactStorageKey ?? null)

  const canDownloadDocx = computed(
    () =>
      latestPreview.value?.status === 'SUCCEEDED' &&
      Boolean(latestPreview.value?.artifactStorageKey),
  )
  const canDownloadPdf = computed(
    () =>
      latestPreview.value?.status === 'SUCCEEDED' &&
      (Boolean(latestPreview.value?.pdfArtifactStorageKey) ||
        Boolean(latestPreview.value?.artifactStorageKey)),
  )

  async function downloadArtifact(format: 'docx' | 'pdf') {
    if (!latestPreview.value?.previewId) {
      return
    }
    downloadingFormat.value = format
    try {
      const { blob, filename } = await panelDataStore.downloadPreviewArtifact(
        toValue(options.templateId),
        latestPreview.value.previewId,
        format,
      )
      downloadBlobExport(filename, blob)
    } catch {
      ElMessage.error(t('templates.previewHistory.error.download'))
    } finally {
      downloadingFormat.value = null
    }
  }

  return {
    t,
    loading,
    downloadingFormat,
    latestPreview,
    comparisonItems,
    comparisonSummary,
    refreshPreview,
    severityTagType,
    locationLabel,
    previewArtifact,
    canDownloadDocx,
    canDownloadPdf,
    downloadArtifact,
  }
}
