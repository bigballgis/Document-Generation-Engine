import { computed, reactive, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useConfirmAction } from '@/composables/useConfirmAction'
import { useCapabilities } from '@/composables/useCapabilities'
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
  analyzeVariableRenameImpact,
  executeVariableRenameCascade,
  validateRenameVariableKey,
} from '@/utils/variableRenameCascade'
import {
  evaluateComputeExpression,
  validateComputeExpression,
  listTestDataSets,
  updateTestDataSet,
  upsertVariable as upsertVariableApi,
  deleteVariable as deleteVariableApi,
  upsertBinding as upsertBindingApi,
  saveRules as saveRulesApi,
} from '@/api/templates'
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
  const { authorTemplates } = useCapabilities()

  const searchQuery = ref('')
  const variableDialogOpen = ref(false)
  const editingVariableKey = ref<string | null>(null)
  const treeRef = ref<{ filter: (value: string) => void } | null>(null)
  const computeValidationError = ref<string | null>(null)
  const sampleJson = ref('{\n  "principal": 100\n}')
  const sampleResult = ref<string | null>(null)
  const sampleError = ref<string | null>(null)
  const sampleEvaluating = ref(false)
  const renaming = ref(false)

  const variableTypes = ['TEXT', 'NUMBER', 'AMOUNT', 'DATE', 'ENUM', 'BOOLEAN', 'LIST', 'OBJECT', 'COMPUTED']

  const variableForm = reactive<UpsertVariablePayload>({
    variableKey: '',
    variableType: 'TEXT',
    required: true,
    defaultValue: '',
    description: '',
    computeExpression: '',
    piiCategory: 'NONE',
  })

  const piiCategories = [
    'NONE',
    'PERSONAL_NAME',
    'GOVERNMENT_ID',
    'FINANCIAL_ACCOUNT',
    'CONTACT',
    'ADDRESS',
    'OTHER_SENSITIVE',
  ] as const

  const canWriteVariables = computed(() => authorTemplates.value)
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
    variableForm.piiCategory = 'NONE'
    editingVariableKey.value = null
    computeValidationError.value = null
    sampleResult.value = null
    sampleError.value = null
  }

  function openAddVariable() {
    if (!canWriteVariables.value) {
      return
    }
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
    variableForm.piiCategory = variable.piiCategory ?? 'NONE'
    sampleResult.value = null
    sampleError.value = null
    variableDialogOpen.value = true
  }

  function buildPayload(): UpsertVariablePayload {
    return {
      variableKey: variableForm.variableKey.trim(),
      variableType: variableForm.variableType,
      required: variableForm.required,
      defaultValue: variableForm.defaultValue || null,
      description: variableForm.description || null,
      computeExpression:
        variableForm.variableType === 'COMPUTED'
          ? variableForm.computeExpression?.trim() || null
          : null,
      piiCategory: variableForm.piiCategory || 'NONE',
    }
  }

  async function validateComputedIfNeeded(payload: UpsertVariablePayload): Promise<boolean> {
    if (payload.variableType !== 'COMPUTED') {
      return true
    }
    const client = validateComputeExpressionClient(
      payload.computeExpression ?? '',
      knownVariableKeys.value,
    )
    if (!client.valid) {
      computeValidationError.value = t(
        client.messageKey ?? 'templates.authoring.computeExpressionInvalid',
      )
      ElMessage.error(computeValidationError.value)
      return false
    }
    try {
      const server = await validateComputeExpression(options.templateId.value, {
        variableKey: payload.variableKey,
        expression: payload.computeExpression ?? '',
        knownVariableKeys: knownVariableKeys.value,
      })
      if (!server.valid) {
        computeValidationError.value =
          server.message || t('templates.authoring.computeExpressionInvalid')
        ElMessage.error(computeValidationError.value)
        return false
      }
    } catch {
      ElMessage.error(t('templates.error.saveVariable'))
      return false
    }
    return true
  }

  async function handleRenameVariable(oldKey: string, payload: UpsertVariablePayload): Promise<boolean> {
    const selected = templatesStore.selectedTemplate
    const bindings = selected?.bindings ?? []
    const rules = selected?.rules ?? []
    const variables = options.variables.value
    let testDataSets
    try {
      testDataSets = await listTestDataSets(options.templateId.value)
    } catch {
      ElMessage.error(t('templates.error.saveVariable'))
      return false
    }

    const impact = analyzeVariableRenameImpact(oldKey, bindings, rules, variables, testDataSets)
    const confirmed = await confirmAction({
      titleKey: 'templates.authoring.rename.confirmTitle',
      messageKey: 'templates.authoring.rename.confirmMessage',
      messageParams: {
        oldKey,
        newKey: payload.variableKey,
        bindingCount: impact.bindingAnchorCount,
        ruleCount: impact.ruleCount,
        unlockedCount: impact.unlockedTestSetCount,
        lockedCount: impact.lockedTestSetSkippedCount,
        computeCount: impact.computeReferenceCount,
      },
      type: 'warning',
    })
    if (!confirmed) {
      return false
    }

    renaming.value = true
    templatesStore.submitting = true
    try {
      const result = await executeVariableRenameCascade({
        templateId: options.templateId.value,
        oldKey,
        newKey: payload.variableKey,
        variablePayload: payload,
        bindings,
        rules,
        variables,
        testDataSets,
        upsertVariable: upsertVariableApi,
        deleteVariable: deleteVariableApi,
        upsertBinding: upsertBindingApi,
        saveRules: saveRulesApi,
        updateTestDataSet,
        refreshTemplate: (templateId) => templatesStore.fetchTemplate(templateId),
      })
      variableDialogOpen.value = false
      ElMessage.success(t('templates.authoring.renameVariableSuccess'))
      if (result.lockedSkippedCount > 0) {
        ElMessage.warning(
          t('templates.authoring.renameVariableLockedWarning', {
            count: result.lockedSkippedCount,
          }),
        )
      }
      options.onUpdated()
      return true
    } catch {
      ElMessage.error(t('templates.error.saveVariable'))
      return false
    } finally {
      renaming.value = false
      templatesStore.submitting = false
    }
  }

  async function handleSaveVariable() {
    if (!canWriteVariables.value) {
      return
    }
    const payload = buildPayload()
    const oldKey = editingVariableKey.value
    const isRename = Boolean(oldKey && payload.variableKey !== oldKey)

    if (isRename && oldKey) {
      const validation = validateRenameVariableKey(
        payload.variableKey,
        oldKey,
        options.variables.value.map((item) => item.variableKey),
      )
      if (!validation.valid) {
        ElMessage.error(t(validation.messageKey ?? 'templates.error.saveVariable'))
        return
      }
    } else if (!payload.variableKey.trim()) {
      ElMessage.error(t('templates.authoring.rename.variableKeyRequired'))
      return
    }

    if (!(await validateComputedIfNeeded(payload))) {
      return
    }

    if (isRename && oldKey) {
      await handleRenameVariable(oldKey, payload)
      return
    }

    try {
      await templatesStore.upsertVariable(options.templateId.value, payload.variableKey, payload)
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
    if (!canWriteVariables.value) {
      return
    }
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
    canWriteVariables,
    searchQuery,
    variableDialogOpen,
    editingVariableKey,
    treeRef,
    variableTypes,
    piiCategories,
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
    renaming,
    openAddVariable,
    openEditVariable,
    handleSaveVariable,
    handleSampleEvaluate,
    handleDeleteVariable,
    filterTreeNode,
  }
}
