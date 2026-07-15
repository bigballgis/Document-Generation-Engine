import { reactive, ref, type Ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useConfirmAction } from '@/composables/useConfirmAction'
import { resolveApiError } from '@/api/http'
import type { useTemplatePanelDataStore } from '@/stores/templatePanelData'
import type { ApiFieldError } from '@/types/session'
import type { TestDataSet, VariableSchema } from '@/types/template'
import { buildSchemaSkeleton } from '@/utils/testDataSetSchemaForm'

type PanelDataStore = ReturnType<typeof useTemplatePanelDataStore>
type Translate = (key: string) => string

export function createTemplateTestDataSetPanelActions(options: {
  t: Translate
  panelDataStore: PanelDataStore
  templateId: () => string
  variables: () => VariableSchema[]
  selectedId: Ref<string | null>
  emitSelected: (testDataSetId: string | null) => void
}) {
  const { t, panelDataStore, templateId, variables, selectedId, emitSelected } = options
  const { confirmAction } = useConfirmAction()

  const saving = ref(false)
  const dialogVisible = ref(false)
  const editingId = ref<string | null>(null)
  const previewDialogVisible = ref(false)
  const previewDialogPreviewId = ref('')
  const previewDialogStreamUrl = ref('')
  const previewDialogDataSetName = ref('')
  const initialVariables = ref<Record<string, unknown>>({})
  const serverFieldErrors = ref<ApiFieldError[] | undefined>(undefined)

  const form = reactive({
    name: '',
    description: '',
    required: false,
    scenarioName: '',
  })
  const coverageTagsText = ref('')

  function resetForm() {
    form.name = ''
    form.description = ''
    form.required = false
    form.scenarioName = ''
    coverageTagsText.value = ''
    editingId.value = null
    initialVariables.value = buildSchemaSkeleton(variables())
    serverFieldErrors.value = undefined
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
    initialVariables.value = { ...(row.variables as Record<string, unknown>) }
    coverageTagsText.value = row.coverageTags.join(', ')
    serverFieldErrors.value = undefined
    dialogVisible.value = true
  }

  function clearServerErrors() {
    serverFieldErrors.value = undefined
  }

  async function handleSave(payload: {
  variables: Record<string, unknown>
  piiHandling?: string
  piiConfirmReason?: string
  secondaryConfirmed?: boolean
}) {
    if (!form.name.trim()) {
      ElMessage.error(t('templates.testDataSets.error.invalidForm'))
      return
    }
    const upsertPayload = {
      name: form.name.trim(),
      description: form.description.trim() || undefined,
      variables: payload.variables,
      required: form.required,
      scenarioName: form.scenarioName.trim() || undefined,
      coverageTags: parseCoverageTags(),
      ...(payload.piiHandling
        ? {
            piiHandling: payload.piiHandling as 'SYNTHETIC' | 'EXPLICIT_SENSITIVE',
            piiConfirmReason: payload.piiConfirmReason,
            secondaryConfirmed: payload.secondaryConfirmed,
          }
        : {}),
    }
    saving.value = true
    serverFieldErrors.value = undefined
    try {
      const id = templateId()
      if (editingId.value) {
        await panelDataStore.updateTestDataSet(id, editingId.value, upsertPayload)
        ElMessage.success(t('templates.testDataSets.updateSuccess'))
      } else {
        const created = await panelDataStore.createTestDataSet(id, upsertPayload)
        selectedId.value = created.testDataSetId
        emitSelected(created.testDataSetId)
        ElMessage.success(t('templates.testDataSets.createSuccess'))
      }
      dialogVisible.value = false
    } catch (error: unknown) {
      const resolved = resolveApiError(error)
      if (resolved?.error.fieldErrors?.length) {
        serverFieldErrors.value = resolved.error.fieldErrors
        ElMessage.error(t(resolved.error.messageKey || 'templates.testDataSets.error.save'))
      } else {
        ElMessage.error(
          t(resolved?.error.messageKey || 'templates.testDataSets.error.save'),
        )
      }
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
    coverageTagsText,
    initialVariables,
    serverFieldErrors,
    openCreateDialog,
    openEditDialog,
    handleSave,
    clearServerErrors,
    handleDerive,
    handleDelete,
    handleSelect,
    handleRunPreview,
  }
}
