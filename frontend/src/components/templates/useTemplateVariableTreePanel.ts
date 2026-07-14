import { computed, reactive, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useConfirmAction } from '@/composables/useConfirmAction'
import { useTemplatesStore } from '@/stores/templates'
import type { UpsertVariablePayload, VariableSchema } from '@/types/template'
import {
  buildVariableSchemaTree,
  collectVariableTreeExpandKeys,
  filterVariableTree,
  type VariableTreeNode,
} from '@/utils/variableSchemaTree'
import { validateComputeExpressionClient } from '@/utils/computeExpressionValidate'
import {
  evaluateComputeExpression,
  validateComputeExpression,
} from '@/api/templatesBindings'
import { ElMessage } from 'element-plus'

export interface UseTemplateVariableTreePanelOptions {
  templateId: Ref<string>
  variables: Ref<VariableSchema[]>
  onUpdated: () => void
}

export function useTemplateVariableTreePanel(options: UseTemplateVariableTreePanelOptions) {
  const { t } = useI18n()
  const templatesStore = useTemplatesStore()
  const { confirmAction } = useConfirmAction()

  const searchQuery = ref('')
  const variableDialogOpen = ref(false)
  const editingVariableKey = ref<string | null>(null)
  const treeRef = ref<{ filter: (value: string) => void } | null>(null)
  const computeValidationError = ref<string | null>(null)
  const sampleJson = ref('{\n  "principal": 100\n}')
  const sampleResult = ref<string | null>(null)
  const sampleError = ref<string | null>(null)
  const sampleEvaluating = ref(false)

  const variableTypes = ['TEXT', 'NUMBER', 'AMOUNT', 'DATE', 'ENUM', 'BOOLEAN', 'LIST', 'OBJECT', 'COMPUTED']

  const variableForm = reactive<UpsertVariablePayload>({
    variableKey: '',
    variableType: 'TEXT',
    required: true,
    defaultValue: '',
    description: '',
    computeExpression: '',
  })

  const sourceTree = computed(() => buildVariableSchemaTree(options.variables.value))
  const filteredTree = computed(() => filterVariableTree(sourceTree.value, searchQuery.value))
  const treeRenderKey = computed(() => searchQuery.value.trim())
  const searchExpandedKeys = computed(() =>
    searchQuery.value.trim() ? collectVariableTreeExpandKeys(filteredTree.value) : [],
  )
  const totalVariableCount = computed(() => options.variables.value.length)

  const knownVariableKeys = computed(() => {
    const keys = options.variables.value.map((item) => item.variableKey)
    if (variableForm.variableKey.trim()) {
      keys.push(variableForm.variableKey.trim())
    }
    return [...new Set(keys)]
  })

  watch(searchQuery, (value) => {
    treeRef.value?.filter(value.trim())
  })

  watch(
    () => [variableForm.variableType, variableForm.computeExpression, knownVariableKeys.value] as const,
    () => {
      if (variableForm.variableType !== 'COMPUTED') {
        computeValidationError.value = null
        return
      }
      const client = validateComputeExpressionClient(
        variableForm.computeExpression ?? '',
        knownVariableKeys.value,
      )
      computeValidationError.value = client.valid
        ? null
        : t(client.messageKey ?? 'templates.authoring.computeExpressionInvalid')
    },
  )

  function resetVariableForm() {
    variableForm.variableKey = ''
    variableForm.variableType = 'TEXT'
    variableForm.required = true
    variableForm.defaultValue = ''
    variableForm.description = ''
    variableForm.computeExpression = ''
    editingVariableKey.value = null
    computeValidationError.value = null
    sampleResult.value = null
    sampleError.value = null
  }

  function openAddVariable() {
    resetVariableForm()
    variableDialogOpen.value = true
  }

  function openEditVariable(variable: VariableSchema) {
    editingVariableKey.value = variable.variableKey
    variableForm.variableKey = variable.variableKey
    variableForm.variableType = variable.variableType
    variableForm.required = variable.required
    variableForm.defaultValue = variable.defaultValue ?? ''
    variableForm.description = variable.description ?? ''
    variableForm.computeExpression = variable.computeExpression ?? ''
    sampleResult.value = null
    sampleError.value = null
    variableDialogOpen.value = true
  }

  async function handleSaveVariable() {
    if (variableForm.variableType === 'COMPUTED') {
      const client = validateComputeExpressionClient(
        variableForm.computeExpression ?? '',
        knownVariableKeys.value,
      )
      if (!client.valid) {
        computeValidationError.value = t(
          client.messageKey ?? 'templates.authoring.computeExpressionInvalid',
        )
        ElMessage.error(computeValidationError.value)
        return
      }
      try {
        const server = await validateComputeExpression(options.templateId.value, {
          variableKey: variableForm.variableKey,
          expression: variableForm.computeExpression ?? '',
          knownVariableKeys: knownVariableKeys.value,
        })
        if (!server.valid) {
          computeValidationError.value =
            server.message || t('templates.authoring.computeExpressionInvalid')
          ElMessage.error(computeValidationError.value)
          return
        }
      } catch {
        ElMessage.error(t('templates.error.saveVariable'))
        return
      }
    }
    try {
      await templatesStore.upsertVariable(options.templateId.value, variableForm.variableKey, {
        variableKey: variableForm.variableKey,
        variableType: variableForm.variableType,
        required: variableForm.required,
        defaultValue: variableForm.defaultValue || null,
        description: variableForm.description || null,
        computeExpression:
          variableForm.variableType === 'COMPUTED'
            ? variableForm.computeExpression?.trim() || null
            : null,
      })
      variableDialogOpen.value = false
      ElMessage.success(t('templates.authoring.saveVariableSuccess'))
      options.onUpdated()
    } catch {
      ElMessage.error(t('templates.error.saveVariable'))
    }
  }

  async function handleSampleEvaluate() {
    sampleResult.value = null
    sampleError.value = null
    let sampleVariables: Record<string, unknown>
    try {
      sampleVariables = JSON.parse(sampleJson.value) as Record<string, unknown>
    } catch {
      sampleError.value = t('templates.authoring.computeSampleJsonInvalid')
      return
    }
    sampleEvaluating.value = true
    try {
      const result = await evaluateComputeExpression(options.templateId.value, {
        variableKey: variableForm.variableKey || 'preview',
        expression: variableForm.computeExpression ?? '',
        sampleVariables,
        locale: 'zh-CN',
      })
      sampleResult.value = String(result.result ?? '')
    } catch (error: unknown) {
      const axiosError = error as {
        response?: { data?: { error?: { fieldErrors?: { message?: string }[]; message?: string } } }
      }
      const fieldMessages = axiosError.response?.data?.error?.fieldErrors
        ?.map((item) => item.message)
        .filter(Boolean)
        .join(', ')
      sampleError.value =
        fieldMessages ||
        axiosError.response?.data?.error?.message ||
        t('templates.authoring.computeSampleFailed')
    } finally {
      sampleEvaluating.value = false
    }
  }

  async function handleDeleteVariable(variableKey: string) {
    const confirmed = await confirmAction({
      titleKey: 'templates.authoring.confirmDeleteVariableTitle',
      messageKey: 'templates.authoring.confirmDeleteVariableMessage',
      type: 'warning',
    })
    if (!confirmed) {
      return
    }
    try {
      await templatesStore.deleteVariable(options.templateId.value, variableKey)
      ElMessage.success(t('templates.authoring.deleteVariableSuccess'))
      options.onUpdated()
    } catch {
      ElMessage.error(t('templates.error.deleteVariable'))
    }
  }

  function filterTreeNode(value: string, data: VariableTreeNode): boolean {
    const normalized = value.trim().toLowerCase()
    if (!normalized) {
      return true
    }
    if (data.isLeaf && data.variable) {
      const key = data.variable.variableKey.toLowerCase()
      const description = (data.variable.description ?? '').toLowerCase()
      const display = data.displayLabel.toLowerCase()
      return key.includes(normalized) || description.includes(normalized) || display.includes(normalized)
    }
    return data.displayLabel.toLowerCase().includes(normalized) || data.label.toLowerCase().includes(normalized)
  }

  return {
    t,
    templatesStore,
    searchQuery,
    variableDialogOpen,
    editingVariableKey,
    treeRef,
    variableTypes,
    variableForm,
    filteredTree,
    treeRenderKey,
    searchExpandedKeys,
    totalVariableCount,
    computeValidationError,
    sampleJson,
    sampleResult,
    sampleError,
    sampleEvaluating,
    openAddVariable,
    openEditVariable,
    handleSaveVariable,
    handleSampleEvaluate,
    handleDeleteVariable,
    filterTreeNode,
  }
}
