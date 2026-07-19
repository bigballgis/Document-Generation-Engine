import { computed, onMounted, ref, toRef, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { rowSortMethod, useDataTableFilters } from '@/composables/useDataTableFilters'
import { useTemplatePanelDataStore } from '@/stores/templatePanelData'
import { downloadBlobExport } from '@/utils/downloadExport'
import {
  canOpenRenderedCompare,
  resolveRenderedCompareHintKey,
} from '@/components/templates/renderedCompareSelection'
import type { PreviewRunSummary } from '@/types/template'

function useTemplatePreviewRunHistoryPanel(options: {
  templateId: Ref<string>
  refreshToken: Ref<number | undefined>
  selectedPreviewId: Ref<string | null>
  emitSelected: (previewId: string | null) => void
}) {
  const { t } = useI18n()
  const { formatDateTime } = useLocaleFormatters()
  const panelDataStore = useTemplatePanelDataStore()
  const downloadingKey = ref<string | null>(null)
  const selectedCompareRuns = ref<PreviewRunSummary[]>([])
  const compareDialogVisible = ref(false)
  const entry = computed(() => panelDataStore.getEntry(options.templateId.value))
  const loading = computed(() => entry.value.loadingPreviewRuns)
  const runs = computed(() => entry.value.previewRuns)
  const runsSource = computed(() => runs.value)

  const { filteredRows: filteredRuns } = useDataTableFilters(runsSource, [
    { key: 'previewId', getValue: (row) => row.previewId },
    { key: 'testDataSetId', getValue: (row) => row.testDataSetId ?? '' },
    { key: 'createdAt', getValue: (row) => formatDateTime(row.createdAt) },
  ])

  const sortByCreatedAt = rowSortMethod<PreviewRunSummary>((row) => row.createdAt)

  const canCompareRendered = computed(() => canOpenRenderedCompare(selectedCompareRuns.value))
  const compareHintKey = computed(() => resolveRenderedCompareHintKey(selectedCompareRuns.value))
  const compareRunA = computed(() =>
    canCompareRendered.value ? (selectedCompareRuns.value[0] ?? null) : null,
  )
  const compareRunB = computed(() =>
    canCompareRendered.value ? (selectedCompareRuns.value[1] ?? null) : null,
  )

  async function loadRuns() {
    try {
      await panelDataStore.fetchPreviewRuns(options.templateId.value)
      if (options.selectedPreviewId.value) {
        const stillExists = runs.value.some(
          (row) => row.previewId === options.selectedPreviewId.value,
        )
        if (!stillExists) {
          options.emitSelected(null)
        }
      }
      const availableIds = new Set(runs.value.map((row) => row.previewId))
      selectedCompareRuns.value = selectedCompareRuns.value.filter((row) =>
        availableIds.has(row.previewId),
      )
    } catch {
      ElMessage.error(t('templates.previewHistory.error.load'))
    }
  }

  async function downloadArtifact(row: PreviewRunSummary, format: 'docx' | 'pdf') {
    const key = `${row.previewId}-${format}`
    downloadingKey.value = key
    try {
      const { blob, filename } = await panelDataStore.downloadPreviewArtifact(
        options.templateId.value,
        row.previewId,
        format,
      )
      downloadBlobExport(filename, blob)
    } catch {
      ElMessage.error(t('templates.previewHistory.error.download'))
    } finally {
      downloadingKey.value = null
    }
  }

  function selectRow(row: PreviewRunSummary) {
    options.emitSelected(row.previewId)
  }

  function onSelectionChange(rows: PreviewRunSummary[]) {
    selectedCompareRuns.value = rows
  }

  function openRenderedCompare() {
    if (!canCompareRendered.value) {
      return
    }
    compareDialogVisible.value = true
  }

  function statusTagType(
    status: PreviewRunSummary['status'],
  ): 'success' | 'warning' | 'danger' | 'info' {
    if (status === 'SUCCEEDED') return 'success'
    if (status === 'FAILED') return 'danger'
    if (status === 'PROCESSING') return 'warning'
    return 'info'
  }

  function rowClassName({ row }: { row: PreviewRunSummary }) {
    return options.selectedPreviewId.value === row.previewId
      ? 'preview-run-row is-selected'
      : 'preview-run-row'
  }

  onMounted(() => {
    void loadRuns()
  })

  watch(
    () => options.refreshToken.value,
    () => {
      void loadRuns()
    },
  )

  return {
    t,
    formatDateTime,
    downloadingKey,
    loading,
    filteredRuns,
    sortByCreatedAt,
    loadRuns,
    downloadArtifact,
    selectRow,
    onSelectionChange,
    canCompareRendered,
    compareHintKey,
    compareRunA,
    compareRunB,
    compareDialogVisible,
    openRenderedCompare,
    statusTagType,
    rowClassName,
  }
}

export function useTemplatePreviewRunHistoryPanelFromProps(props: {
  templateId: string
  refreshToken?: number
  selectedPreviewId: string | null
}, emitSelected: (previewId: string | null) => void) {
  return useTemplatePreviewRunHistoryPanel({
    templateId: toRef(props, 'templateId'),
    refreshToken: toRef(props, 'refreshToken'),
    selectedPreviewId: toRef(props, 'selectedPreviewId'),
    emitSelected,
  })
}
