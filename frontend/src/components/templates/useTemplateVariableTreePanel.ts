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

  watch(searchQuery, (value) => {
    treeRef.value?.filter(value.trim())
  })

  function resetVariableForm() {
    variableForm.variableKey = ''
    variableForm.variableType = 'TEXT'
    variableForm.required = true
    variableForm.defaultValue = ''
    variableForm.description = ''
    variableForm.computeExpression = ''
    editingVariableKey.value = null
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
    variableDialogOpen.value = true
  }

  async function handleSaveVariable() {
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
    openAddVariable,
    openEditVariable,
    handleSaveVariable,
    handleDeleteVariable,
    filterTreeNode,
  }
}
