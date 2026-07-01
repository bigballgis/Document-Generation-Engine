<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
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

const props = defineProps<{
  templateId: string
  variables: VariableSchema[]
}>()

const emit = defineEmits<{
  updated: []
}>()

const { t } = useI18n()
const templatesStore = useTemplatesStore()
const { confirmAction } = useConfirmAction()

const searchQuery = ref('')
const variableDialogOpen = ref(false)
const editingVariableKey = ref<string | null>(null)
const treeRef = ref<{ filter: (value: string) => void } | null>(null)

const variableTypes = ['TEXT', 'NUMBER', 'AMOUNT', 'DATE', 'ENUM', 'BOOLEAN', 'LIST', 'OBJECT']

const variableForm = reactive<UpsertVariablePayload>({
  variableKey: '',
  variableType: 'TEXT',
  required: true,
  defaultValue: '',
  description: '',
})

const sourceTree = computed(() => buildVariableSchemaTree(props.variables))
const filteredTree = computed(() => filterVariableTree(sourceTree.value, searchQuery.value))
const treeRenderKey = computed(() => searchQuery.value.trim())
const searchExpandedKeys = computed(() =>
  searchQuery.value.trim() ? collectVariableTreeExpandKeys(filteredTree.value) : [],
)
const totalVariableCount = computed(() => props.variables.length)

watch(searchQuery, (value) => {
  treeRef.value?.filter(value.trim())
})

function resetVariableForm() {
  variableForm.variableKey = ''
  variableForm.variableType = 'TEXT'
  variableForm.required = true
  variableForm.defaultValue = ''
  variableForm.description = ''
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
  variableDialogOpen.value = true
}

async function handleSaveVariable() {
  try {
    await templatesStore.upsertVariable(props.templateId, variableForm.variableKey, {
      variableKey: variableForm.variableKey,
      variableType: variableForm.variableType,
      required: variableForm.required,
      defaultValue: variableForm.defaultValue || null,
      description: variableForm.description || null,
    })
    variableDialogOpen.value = false
    ElMessage.success(t('templates.authoring.saveVariableSuccess'))
    emit('updated')
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
    await templatesStore.deleteVariable(props.templateId, variableKey)
    ElMessage.success(t('templates.authoring.deleteVariableSuccess'))
    emit('updated')
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
</script>

<template>
  <div class="variable-tree-panel">
    <div class="panel-toolbar">
      <el-input
        v-model="searchQuery"
        clearable
        class="search-input"
        :placeholder="t('templates.authoring.variableTreeSearchPlaceholder')"
      />
      <span class="variable-count">
        {{ t('templates.authoring.variableTreeCount', { count: totalVariableCount }) }}
      </span>
      <el-button type="primary" plain @click="openAddVariable">
        {{ t('templates.authoring.addVariable') }}
      </el-button>
    </div>

    <el-empty
      v-if="variables.length === 0"
      :description="t('templates.authoring.noVariables')"
    />

    <el-tree
      v-else
      :key="treeRenderKey"
      ref="treeRef"
      class="variable-tree"
      :data="filteredTree"
      node-key="id"
      :default-expand-all="false"
      :default-expanded-keys="searchExpandedKeys"
      :expand-on-click-node="true"
      :filter-node-method="filterTreeNode"
    >
      <template #default="{ data }">
        <div class="tree-node" :class="{ 'tree-node--leaf': data.isLeaf }">
          <span class="tree-node__label">{{ data.displayLabel }}</span>
          <template v-if="!data.isLeaf && data.containerType">
            <el-tag size="small" type="primary">{{ data.containerType }}</el-tag>
          </template>
          <template v-if="data.isLeaf && data.variable">
            <el-tag size="small" type="info">{{ data.variable.variableType }}</el-tag>
            <el-tag v-if="data.variable.required" size="small" type="warning">
              {{ t('templates.authoring.required') }}
            </el-tag>
            <span v-if="data.technicalKey" class="tree-node__technical-key">{{ data.technicalKey }}</span>
            <span class="tree-node__actions">
              <el-button link type="primary" @click.stop="openEditVariable(data.variable)">
                {{ t('common.edit') }}
              </el-button>
              <el-button link type="danger" @click.stop="handleDeleteVariable(data.variable.variableKey)">
                {{ t('common.delete') }}
              </el-button>
            </span>
          </template>
          <span v-else-if="!data.isLeaf" class="tree-node__folder-key">{{ data.label }}</span>
        </div>
      </template>
    </el-tree>

    <el-dialog
      v-model="variableDialogOpen"
      :title="editingVariableKey ? t('templates.authoring.editVariable') : t('templates.authoring.addVariable')"
      width="520px"
    >
      <el-form label-position="top">
        <el-form-item :label="t('templates.authoring.variableKey')">
          <el-input v-model="variableForm.variableKey" :disabled="Boolean(editingVariableKey)" />
        </el-form-item>
        <el-form-item :label="t('templates.authoring.variableType')">
          <AppSearchSelect v-model="variableForm.variableType" style="width: 100%">
            <el-option v-for="type in variableTypes" :key="type" :label="type" :value="type" />
          </AppSearchSelect>
        </el-form-item>
        <el-form-item :label="t('templates.authoring.required')">
          <el-switch v-model="variableForm.required" />
        </el-form-item>
        <el-form-item :label="t('templates.authoring.defaultValue')">
          <el-input v-model="variableForm.defaultValue" />
        </el-form-item>
        <el-form-item :label="t('templates.authoring.description')">
          <el-input v-model="variableForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="variableDialogOpen = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="templatesStore.submitting" @click="handleSaveVariable">
          {{ t('common.save') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.variable-tree-panel {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.panel-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
}

.search-input {
  flex: 1 1 16rem;
  max-width: 28rem;
}

.variable-count {
  color: var(--text-muted);
  font-size: 0.875rem;
}

.variable-tree {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 0.75rem;
  max-height: 36rem;
  overflow: auto;
}

.tree-node {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
  min-height: 1.75rem;
  padding-right: 0.5rem;

  &--leaf {
    width: 100%;
  }
}

.tree-node__label {
  font-weight: 500;
}

.tree-node__technical-key,
.tree-node__folder-key {
  color: var(--text-muted);
  font-size: 0.8125rem;
  font-family: var(--font-mono, ui-monospace, monospace);
}

.tree-node__folder-key {
  margin-left: 0.25rem;
}

.tree-node__actions {
  margin-left: auto;
  display: inline-flex;
  gap: 0.25rem;
}
</style>
