import { reactive, ref, type Ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useConfirmAction } from '@/composables/useConfirmAction'
import type { useTemplatePanelDataStore } from '@/stores/templatePanelData'
import type { TestDataSet } from '@/types/template'

type PanelDataStore = ReturnType<typeof useTemplatePanelDataStore>
type Translate = (key: string) => string

export function createTemplateTestDataSetPanelActions(options: {
  t: Translate
  panelDataStore: PanelDataStore
  templateId: () => string
  selectedId: Ref<string | null>
  emitSelected: (testDataSetId: string | null) => void
}) {
  const { t, panelDataStore, templateId, selectedId, emitSelected } = options
  const { confirmAction } = useConfirmAction()

  const saving = ref(false)
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
      const id = templateId()
      if (editingId.value) {
        await panelDataStore.updateTestDataSet(id, editingId.value, payload)
        ElMessage.success(t('templates.testDataSets.updateSuccess'))
      } else {
        const created = await panelDataStore.createTestDataSet(id, payload)
        selectedId.value = created.testDataSetId
        emitSelected(created.testDataSetId)
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
      const derived = await panelDataStore.deriveTestDataSet(templateId(), testDataSetId)
      selectedId.value = derived.testDataSetId
      emitSelected(derived.testDataSetId)
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
      await panelDataStore.deleteTestDataSet(templateId(), testDataSetId)
      if (selectedId.value === testDataSetId) {
        selectedId.value = null
        emitSelected(null)
      }
      ElMessage.success(t('templates.testDataSets.deleteSuccess'))
    } catch {
      ElMessage.error(t('templates.testDataSets.error.delete'))
    }
  }

  function handleSelect(testDataSetId: string) {
    selectedId.value = testDataSetId
    emitSelected(testDataSetId)
  }

  async function handleRunPreview(row: TestDataSet) {
    selectedId.value = row.testDataSetId
    emitSelected(row.testDataSetId)
    try {
      const result = await panelDataStore.startAsyncPreview(templateId(), {
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

  return {
    saving,
    dialogVisible,
    editingId,
    previewDialogVisible,
    previewDialogPreviewId,
    previewDialogStreamUrl,
    previewDialogDataSetName,
    form,
    variablesJson,
    coverageTagsText,
    openCreateDialog,
    openEditDialog,
    handleSave,
    handleDerive,
    handleDelete,
    handleSelect,
    handleRunPreview,
  }
}
