<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import { rowSortMethod, useDataTableFilters } from '@/composables/useDataTableFilters'
import { useConfirmAction } from '@/composables/useConfirmAction'
import { useTemplatesStore } from '@/stores/templates'
import type { AnchorBinding, UpsertBindingPayload, UpsertVariablePayload, VariableSchema } from '@/types/template'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  templateId: string
  variables: VariableSchema[]
  bindings: AnchorBinding[]
}>()

const emit = defineEmits<{
  updated: []
}>()

const { t } = useI18n()
const templatesStore = useTemplatesStore()
const { confirmAction } = useConfirmAction()

const validating = ref(false)
const variableDialogOpen = ref(false)
const bindingDialogOpen = ref(false)
const editingVariableKey = ref<string | null>(null)
const editingAnchorId = ref<string | null>(null)

const variableTypes = ['TEXT', 'NUMBER', 'AMOUNT', 'DATE', 'ENUM', 'BOOLEAN', 'LIST', 'OBJECT']
const contentTypes = ['TEXT', 'RICH_TEXT', 'TABLE', 'IMAGE', 'CLAUSE', 'SEAL', 'QR_CODE', 'ATTACHMENT_LIST']

const variableForm = reactive<UpsertVariablePayload>({
  variableKey: '',
  variableType: 'TEXT',
  required: true,
  defaultValue: '',
  description: '',
})

const bindingForm = reactive<UpsertBindingPayload>({
  anchorId: '',
  declaredContentType: 'TEXT',
  structuredContentJson: '{"nodes":[{"type":"paragraph","children":[{"type":"text","text":""}]}]}',
})

const hasBindings = computed(() => props.bindings.length > 0)

const variablesSource = computed(() => props.variables)
const { filters: variableColumnFilters, filteredRows: filteredVariables } = useDataTableFilters(
  variablesSource,
  [
    { key: 'variableKey', getValue: (row) => row.variableKey },
    { key: 'variableType', getValue: (row) => row.variableType },
    {
      key: 'required',
      getValue: (row) => (row.required ? t('common.yes') : t('common.no')),
    },
  ],
)

const bindingsSource = computed(() => props.bindings)
const { filters: bindingColumnFilters, filteredRows: filteredBindings } = useDataTableFilters(
  bindingsSource,
  [
    { key: 'anchorId', getValue: (row) => row.anchorId },
    { key: 'declaredContentType', getValue: (row) => row.declaredContentType },
  ],
)

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

function resetBindingForm() {
  bindingForm.anchorId = ''
  bindingForm.declaredContentType = 'TEXT'
  bindingForm.structuredContentJson =
    '{"nodes":[{"type":"paragraph","children":[{"type":"text","text":""}]}]}'
  editingAnchorId.value = null
}

function openAddBinding() {
  resetBindingForm()
  bindingDialogOpen.value = true
}

function openEditBinding(binding: AnchorBinding) {
  editingAnchorId.value = binding.anchorId
  bindingForm.anchorId = binding.anchorId
  bindingForm.declaredContentType = binding.declaredContentType
  bindingForm.structuredContentJson =
    binding.structuredContentJson ??
    '{"nodes":[{"type":"paragraph","children":[{"type":"text","text":""}]}]}'
  bindingDialogOpen.value = true
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

async function handleSaveBinding() {
  try {
    await templatesStore.upsertBinding(props.templateId, bindingForm.anchorId, { ...bindingForm })
    bindingDialogOpen.value = false
    ElMessage.success(t('templates.authoring.saveBindingSuccess'))
    emit('updated')
  } catch {
    ElMessage.error(t('templates.error.saveBinding'))
  }
}

async function handleValidateBindings() {
  validating.value = true
  try {
    const result = await templatesStore.validateBindings(props.templateId)
    if (result.summary.blocking) {
      ElMessage.warning(t('templates.authoring.bindingValidationBlocking'))
    } else {
      ElMessage.success(t('templates.authoring.bindingValidationSuccess'))
    }
  } catch {
    ElMessage.error(t('templates.error.bindingValidation'))
  } finally {
    validating.value = false
  }
}

const sortVariablesByRequired = rowSortMethod<VariableSchema>((row) => row.required)
</script>

<template>
  <div class="authoring-panel">
    <div class="section-header">
      <h3>{{ t('templates.authoring.variablesTitle') }}</h3>
      <el-button type="primary" plain @click="openAddVariable">
        {{ t('templates.authoring.addVariable') }}
      </el-button>
    </div>
    <AppDataTable :data="filteredVariables" empty-text="">
      <template #empty>
        <el-empty :description="t('templates.authoring.noVariables')" />
      </template>
      <el-table-column prop="variableKey" sortable>
        <template #header>
          <TableColumnHeader
            :label="t('templates.authoring.variableKey')"
            v-model="variableColumnFilters.variableKey"
          />
        </template>
      </el-table-column>
      <el-table-column prop="variableType" sortable>
        <template #header>
          <TableColumnHeader
            :label="t('templates.authoring.variableType')"
            v-model="variableColumnFilters.variableType"
          />
        </template>
      </el-table-column>
      <el-table-column
        sortable
        :sort-method="sortVariablesByRequired"
      >
        <template #header>
          <TableColumnHeader
            :label="t('templates.authoring.required')"
            v-model="variableColumnFilters.required"
          />
        </template>
        <template #default="{ row }">
          {{ row.required ? t('common.yes') : t('common.no') }}
        </template>
      </el-table-column>
      <el-table-column :label="t('common.actions')" width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEditVariable(row)">
            {{ t('common.edit') }}
          </el-button>
          <el-button link type="danger" @click="handleDeleteVariable(row.variableKey)">
            {{ t('common.delete') }}
          </el-button>
        </template>
      </el-table-column>
    </AppDataTable>

    <div class="section-header">
      <h3>{{ t('templates.authoring.bindingsTitle') }}</h3>
      <el-button type="primary" plain @click="openAddBinding">
        {{ t('templates.authoring.addBinding') }}
      </el-button>
    </div>
    <AppDataTable :data="filteredBindings" empty-text="">
      <template #empty>
        <el-empty :description="t('templates.authoring.noBindings')" />
      </template>
      <el-table-column prop="anchorId" sortable>
        <template #header>
          <TableColumnHeader
            :label="t('templates.authoring.anchorId')"
            v-model="bindingColumnFilters.anchorId"
          />
        </template>
      </el-table-column>
      <el-table-column prop="declaredContentType" sortable>
        <template #header>
          <TableColumnHeader
            :label="t('templates.authoring.contentType')"
            v-model="bindingColumnFilters.declaredContentType"
          />
        </template>
      </el-table-column>
      <el-table-column :label="t('common.actions')" width="120">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEditBinding(row)">
            {{ t('common.edit') }}
          </el-button>
        </template>
      </el-table-column>
    </AppDataTable>

    <div class="action-row">
      <el-button
        type="primary"
        :loading="validating"
        :disabled="!hasBindings"
        @click="handleValidateBindings"
      >
        {{ t('templates.authoring.validateBindings') }}
      </el-button>
    </div>

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

    <el-dialog
      v-model="bindingDialogOpen"
      :title="editingAnchorId ? t('templates.authoring.editBinding') : t('templates.authoring.addBinding')"
      width="640px"
    >
      <el-form label-position="top">
        <el-form-item :label="t('templates.authoring.anchorId')">
          <el-input v-model="bindingForm.anchorId" :disabled="Boolean(editingAnchorId)" />
        </el-form-item>
        <el-form-item :label="t('templates.authoring.contentType')">
          <AppSearchSelect v-model="bindingForm.declaredContentType" style="width: 100%">
            <el-option v-for="type in contentTypes" :key="type" :label="type" :value="type" />
          </AppSearchSelect>
        </el-form-item>
        <el-form-item :label="t('templates.authoring.structuredContent')">
          <el-input v-model="bindingForm.structuredContentJson" type="textarea" :rows="8" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindingDialogOpen = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="templatesStore.submitting" @click="handleSaveBinding">
          {{ t('common.save') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.authoring-panel {
  .section-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    margin: 1.25rem 0 0.75rem;

    &:first-child {
      margin-top: 0;
    }

    h3 {
      margin: 0;
      font-size: 1rem;
    }
  }
}

.action-row {
  margin-top: 1rem;
}
</style>
