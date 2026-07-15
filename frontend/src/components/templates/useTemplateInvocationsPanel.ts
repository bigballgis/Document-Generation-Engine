import { computed, onMounted, reactive, ref, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { exportInvocationsCsv, listInvocations } from '@/api/apiPolicy'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { SERVER_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import type { ManagementInvocationSummary } from '@/types/template'
import { downloadBlobExport } from '@/utils/downloadExport'

export interface UseTemplateInvocationsPanelOptions {
  templateId: Ref<string>
}

export function useTemplateInvocationsPanel(options: UseTemplateInvocationsPanelOptions) {
  const { t } = useI18n()
  const { formatDateTime } = useLocaleFormatters()

  const pageSize = SERVER_TABLE_PAGE_SIZE
  const currentPage = ref(1)
  const totalElements = ref(0)
  const loading = ref(true)
  const loadFailed = ref(false)
  const exporting = ref(false)
  const rows = ref<ManagementInvocationSummary[]>([])

  const filterDraft = reactive({
    status: '',
    invocationKind: '',
    requestId: '',
    resolvedReleaseVersion: '',
  })

  const appliedFilters = reactive({
    status: '',
    invocationKind: '',
    requestId: '',
    resolvedReleaseVersion: '',
  })

  const drawerVisible = ref(false)
  const selectedInvocationId = ref<string | null>(null)

  const statusFilterOptions = computed(() =>
    ['SUCCEEDED', 'FAILED', 'PARTIAL_SUCCEEDED', 'PROCESSING', 'ACCEPTED', 'EXPIRED', 'CANCELLED'].map(
      (value) => ({ label: value, value }),
    ),
  )

  const kindFilterOptions = computed(() =>
    ['SINGLE', 'BATCH_ROOT', 'ASYNC_TASK'].map((value) => ({ label: value, value })),
  )

  const uiPage = computed({
    get: () => currentPage.value,
    set: (page: number) => {
      currentPage.value = page
      void loadInvocations()
    },
  })

  function currentListFilters() {
    return {
      status: appliedFilters.status || undefined,
      invocationKind: appliedFilters.invocationKind || undefined,
      requestId: appliedFilters.requestId || undefined,
      resolvedReleaseVersion: appliedFilters.resolvedReleaseVersion || undefined,
    }
  }

  async function loadInvocations() {
    loading.value = true
    loadFailed.value = false
    try {
      const result = await listInvocations(
        options.templateId.value,
        currentPage.value - 1,
        pageSize,
        currentListFilters(),
      )
      rows.value = result.content
      totalElements.value = result.totalElements
    } catch {
      rows.value = []
      totalElements.value = 0
      loadFailed.value = true
    } finally {
      loading.value = false
    }
  }

  function applyFilters() {
    appliedFilters.status = filterDraft.status
    appliedFilters.invocationKind = filterDraft.invocationKind
    appliedFilters.requestId = filterDraft.requestId
    appliedFilters.resolvedReleaseVersion = filterDraft.resolvedReleaseVersion
    currentPage.value = 1
    void loadInvocations()
  }

  function clearFilters() {
    filterDraft.status = ''
    filterDraft.invocationKind = ''
    filterDraft.requestId = ''
    filterDraft.resolvedReleaseVersion = ''
    appliedFilters.status = ''
    appliedFilters.invocationKind = ''
    appliedFilters.requestId = ''
    appliedFilters.resolvedReleaseVersion = ''
    currentPage.value = 1
    void loadInvocations()
  }

  function openInvocationSummary(row: ManagementInvocationSummary) {
    selectedInvocationId.value = row.invocationId
    drawerVisible.value = true
  }

  async function copyTechnicalId(value: string) {
    if (!value) {
      return
    }
    try {
      await navigator.clipboard.writeText(value)
      ElMessage.success(t('common.copyToClipboardSuccess'))
    } catch {
      ElMessage.error(t('common.copyToClipboardError'))
    }
  }

  async function exportCsv() {
    exporting.value = true
    try {
      const result = await exportInvocationsCsv(options.templateId.value, currentListFilters())
      downloadBlobExport(result.filename, result.blob)
      if (result.truncated) {
        ElMessage.warning(t('templates.policy.invocations.exportTruncated'))
      } else {
        ElMessage.success(t('templates.policy.invocations.exportSuccess'))
      }
    } catch {
      ElMessage.error(t('templates.policy.invocations.exportFailed'))
    } finally {
      exporting.value = false
    }
  }

  onMounted(() => {
    void loadInvocations()
  })

  return {
    t,
    formatDateTime,
    pageSize,
    totalElements,
    loading,
    loadFailed,
    exporting,
    rows,
    filterDraft,
    drawerVisible,
    selectedInvocationId,
    statusFilterOptions,
    kindFilterOptions,
    uiPage,
    loadInvocations,
    applyFilters,
    clearFilters,
    openInvocationSummary,
    copyTechnicalId,
    exportCsv,
  }
}
