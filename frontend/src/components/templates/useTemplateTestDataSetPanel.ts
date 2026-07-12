import { computed, onMounted, reactive, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { useConfirmAction } from '@/composables/useConfirmAction'
import { useCatalogPagination } from '@/composables/useCatalogPagination'
import { rowSortMethod, useDataTableFilters } from '@/composables/useDataTableFilters'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { CLIENT_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import { useTemplatePanelDataStore } from '@/stores/templatePanelData'
import type { TestDataSet } from '@/types/template'

export interface UseTemplateTestDataSetPanelOptions {
  templateId: Ref<string> | (() => string)
  refreshToken: Ref<number | undefined> | (() => number | undefined)
  emitSelected: (testDataSetId: string | null) => void
  emitLoaded: (count: number) => void
}

function readValue<T>(source: Ref<T> | (() => T)): T {
  return typeof source === 'function' ? source() : source.value
}

export function useTemplateTestDataSetPanel(options: UseTemplateTestDataSetPanelOptions) {
  const { t } = useI18n()
  const { confirmAction } = useConfirmAction()
  const { formatDateTime } = useLocaleFormatters()
  const panelDataStore = useTemplatePanelDataStore()
  const saving = ref(false)
  const dataSets = computed(() => panelDataStore.getEntry(readValue(options.templateId)).testDataSets)
  const loading = computed(() => panelDataStore.getEntry(readValue(options.templateId)).loadingTestDataSets)
  const dataSetsSource = computed(() => dataSets.value)
  const { filters: columnFilters, filteredRows: filteredDataSets } = useDataTableFilters(
    dataSetsSource,
    [
      { key: 'name', getValue: (row) => row.name },
      { key: 'testDataSetId', getValue: (row) => row.testDataSetId },
      { key: 'updatedAt', getValue: (row) => formatDateTime(row.updatedAt) },
    ],
  )
  const dataSetsCurrentPage = ref(1)
  const { paginatedRows: paginatedDataSets, totalRows: totalDataSetRows } = useCatalogPagination(
    filteredDataSets,
    dataSetsCurrentPage,
    CLIENT_TABLE_PAGE_SIZE,
  )
  const sortByUpdatedAt = rowSortMethod<TestDataSet>((row) => row.updatedAt)
  const selectedId = ref<string | null>(null)
  const dialogVisible = ref(false)
  const editingId = ref<string | null>(null)

  const previewDialogVisible = ref(false)
  const previewDialogPreviewId = ref('')
  const previewDialogStreamUrl = ref('')
  const previewDialogDataSetName = ref('')

  const form = reactive({
    name: '',
    description: '',
    required: false,
    scenarioName: '',
  })
  const variablesJson = ref('{\n  "customerName": "Sample"\n}')
  const coverageTagsText = ref('')

  function resetForm() {
    form.name = ''
    form.description = ''
    form.required = false
    form.scenarioName = ''
    variablesJson.value = '{\n  "customerName": "Sample"\n}'
    coverageTagsText.value = ''
    editingId.value = null
  }

  function parseCoverageTags(): string[] {
    return coverageTagsText.value
      .split(',')
      .map((tag) => tag.trim())
      .filter((tag) => tag.length > 0)
  }

  async function loadDataSets() {
    try {
      await panelDataStore.fetchTestDataSets(readValue(options.templateId))
      options.emitLoaded(dataSets.value.length)
    } catch {
      ElMessage.error(t('templates.testDataSets.error.load'))
    }
  }

  watch(
    () => readValue(options.refreshToken),
    () => {
      void loadDataSets()
    },
  )

  function openCreateDialog() {
    resetForm()
    dialogVisible.value = true
  }

  function openEditDialog(row: TestDataSet) {
    if (row.locked) {
      return
    }
    editingId.value = row.testDataSetId
    form.name = row.name
    form.description = row.description ?? ''
    form.required = row.required
    form.scenarioName = row.scenarioName ?? ''
    variablesJson.value = JSON.stringify(row.variables, null, 2)
    coverageTagsText.value = row.coverageTags.join(', ')
    dialogVisible.value = true
  }

  function parseVariables(): Record<string, unknown> | null {
    try {
      const parsed: unknown = JSON.parse(variablesJson.value)
      if (parsed === null || typeof parsed !== 'object' || Array.isArray(parsed)) {
        return null
      }
      return parsed as Record<string, unknown>
    } catch {
      return null
    }
  }

  function buildPayload() {
    const variables = parseVariables()
    if (!form.name.trim() || variables === null) {
      return null
    }
    return {
      name: form.name.trim(),
      description: form.description.trim() || undefined,
      variables,
      required: form.required,
      scenarioName: form.scenarioName.trim() || undefined,
      coverageTags: parseCoverageTags(),
    }
  }

  async function handleSave() {
    const payload = buildPayload()
    if (!payload) {
      ElMessage.error(t('templates.testDataSets.error.invalidForm'))
      return
    }
    saving.value = true
    try {
      const templateId = readValue(options.templateId)
      if (editingId.value) {
        await panelDataStore.updateTestDataSet(templateId, editingId.value, payload)
        ElMessage.success(t('templates.testDataSets.updateSuccess'))
      } else {
        const created = await panelDataStore.createTestDataSet(templateId, payload)
        selectedId.value = created.testDataSetId
        options.emitSelected(created.testDataSetId)
        ElMessage.success(t('templates.testDataSets.createSuccess'))
      }
      dialogVisible.value = false
    } catch {
      ElMessage.error(t('templates.testDataSets.error.save'))
    } finally {
      saving.value = false
    }
  }

  async function handleDerive(testDataSetId: string) {
    try {
      const derived = await panelDataStore.deriveTestDataSet(
        readValue(options.templateId),
        testDataSetId,
      )
      selectedId.value = derived.testDataSetId
      options.emitSelected(derived.testDataSetId)
      ElMessage.success(t('templates.testDataSets.deriveSuccess'))
    } catch {
      ElMessage.error(t('templates.testDataSets.error.save'))
    }
  }

  async function handleDelete(testDataSetId: string) {
    const confirmed = await confirmAction({
      titleKey: 'templates.testDataSets.confirmDeleteTitle',
      messageKey: 'templates.testDataSets.confirmDeleteMessage',
      type: 'warning',
    })
    if (!confirmed) {
      return
    }
    try {
      await panelDataStore.deleteTestDataSet(readValue(options.templateId), testDataSetId)
      if (selectedId.value === testDataSetId) {
        selectedId.value = null
        options.emitSelected(null)
      }
      ElMessage.success(t('templates.testDataSets.deleteSuccess'))
    } catch {
      ElMessage.error(t('templates.testDataSets.error.delete'))
    }
  }

  function handleSelect(testDataSetId: string) {
    selectedId.value = testDataSetId
    options.emitSelected(testDataSetId)
  }

  async function handleRunPreview(row: TestDataSet) {
    selectedId.value = row.testDataSetId
    options.emitSelected(row.testDataSetId)
    try {
      const result = await panelDataStore.startAsyncPreview(readValue(options.templateId), {
        testDataSetId: row.testDataSetId,
      })
      previewDialogPreviewId.value = result.previewId
      previewDialogStreamUrl.value = result.streamUrl
      previewDialogDataSetName.value = row.name
      previewDialogVisible.value = true
    } catch (err: unknown) {
      const axiosError = err as { response?: { status?: number } }
      if (axiosError?.response?.status === 429) {
        ElMessage.error(t('templates.previewProgress.error.concurrencyLimit'))
      } else {
        ElMessage.error(t('templates.previewProgress.error.generic'))
      }
    }
  }

  function handlePreviewRetry() {
    const row = dataSets.value.find((dataSet) => dataSet.testDataSetId === selectedId.value)
    if (row) {
      void handleRunPreview(row)
    }
  }

  onMounted(() => {
    void loadDataSets()
  })

  function rowClassName({ row }: { row: TestDataSet }): string {
    return row.testDataSetId === selectedId.value ? 'is-selected-row' : ''
  }

  return {
    t,
    formatDateTime,
    CLIENT_TABLE_PAGE_SIZE,
    saving,
    loading,
    columnFilters,
    paginatedDataSets,
    totalDataSetRows,
    dataSetsCurrentPage,
    sortByUpdatedAt,
    dialogVisible,
    editingId,
    previewDialogVisible,
    previewDialogPreviewId,
    previewDialogStreamUrl,
    previewDialogDataSetName,
    form,
    variablesJson,
    coverageTagsText,
    loadDataSets,
    dataSets,
    openCreateDialog,
    openEditDialog,
    handleSave,
    handleDerive,
    handleDelete,
    handleSelect,
    handleRunPreview,
    handlePreviewRetry,
    rowClassName,
  }
}
