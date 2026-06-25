<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import AppDataTable from '@/components/common/AppDataTable.vue'
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import { rowSortMethod, useDataTableFilters } from '@/composables/useDataTableFilters'
import * as templatesApi from '@/api/templates'
import { useConfirmAction } from '@/composables/useConfirmAction'
import type { TestDataSet } from '@/types/template'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  templateId: string
}>()

const emit = defineEmits<{
  selected: [testDataSetId: string | null]
}>()

const { t } = useI18n()
const { confirmAction } = useConfirmAction()
const loading = ref(false)
const saving = ref(false)
const dataSets = ref<TestDataSet[]>([])
const dataSetsSource = computed(() => dataSets.value)
const { filters: columnFilters, filteredRows: filteredDataSets } = useDataTableFilters(
  dataSetsSource,
  [
    { key: 'name', getValue: (row) => row.name },
    { key: 'testDataSetId', getValue: (row) => row.testDataSetId },
    { key: 'updatedAt', getValue: (row) => new Date(row.updatedAt).toLocaleString() },
  ],
)
const sortByUpdatedAt = rowSortMethod<TestDataSet>((row) => row.updatedAt)
const selectedId = ref<string | null>(null)
const dialogVisible = ref(false)
const editingId = ref<string | null>(null)

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
  loading.value = true
  try {
    dataSets.value = await templatesApi.listTestDataSets(props.templateId)
  } catch {
    ElMessage.error(t('templates.testDataSets.error.load'))
  } finally {
    loading.value = false
  }
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
    if (editingId.value) {
      await templatesApi.updateTestDataSet(props.templateId, editingId.value, payload)
      ElMessage.success(t('templates.testDataSets.updateSuccess'))
    } else {
      const created = await templatesApi.createTestDataSet(props.templateId, payload)
      selectedId.value = created.testDataSetId
      emit('selected', created.testDataSetId)
      ElMessage.success(t('templates.testDataSets.createSuccess'))
    }
    dialogVisible.value = false
    await loadDataSets()
  } catch {
    ElMessage.error(t('templates.testDataSets.error.save'))
  } finally {
    saving.value = false
  }
}

async function handleDerive(testDataSetId: string) {
  try {
    const derived = await templatesApi.deriveTestDataSet(props.templateId, testDataSetId)
    selectedId.value = derived.testDataSetId
    emit('selected', derived.testDataSetId)
    ElMessage.success(t('templates.testDataSets.deriveSuccess'))
    await loadDataSets()
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
    await templatesApi.deleteTestDataSet(props.templateId, testDataSetId)
    if (selectedId.value === testDataSetId) {
      selectedId.value = null
      emit('selected', null)
    }
    ElMessage.success(t('templates.testDataSets.deleteSuccess'))
    await loadDataSets()
  } catch {
    ElMessage.error(t('templates.testDataSets.error.delete'))
  }
}

function handleSelect(testDataSetId: string) {
  selectedId.value = testDataSetId
  emit('selected', testDataSetId)
}

onMounted(() => {
  void loadDataSets()
})
</script>

<template>
  <div class="test-data-set-panel">
    <p>{{ t('templates.testDataSets.description') }}</p>
    <div class="action-row">
      <el-button type="primary" @click="openCreateDialog">
        {{ t('templates.testDataSets.create') }}
      </el-button>
    </div>
    <AppDataTable
      v-loading="loading"
      :data="filteredDataSets"
      highlight-current-row
      :empty-text="t('templates.testDataSets.empty')"
      @row-click="(row: TestDataSet) => handleSelect(row.testDataSetId)"
    >
      <el-table-column prop="name" sortable min-width="160">
        <template #header>
          <TableColumnHeader
            :label="t('templates.testDataSets.name')"
            v-model="columnFilters.name"
          />
        </template>
        <template #default="{ row }">
          <span>{{ row.name }}</span>
          <el-tag v-if="row.locked" size="small" type="info" class="locked-tag">
            {{ t('templates.testDataSets.locked') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="testDataSetId" sortable min-width="140">
        <template #header>
          <TableColumnHeader
            :label="t('templates.testDataSets.id')"
            v-model="columnFilters.testDataSetId"
          />
        </template>
      </el-table-column>
      <el-table-column prop="datasetVersion" width="90">
        <template #header>
          {{ t('templates.testDataSets.datasetVersion') }}
        </template>
      </el-table-column>
      <el-table-column sortable :sort-method="sortByUpdatedAt" min-width="180">
        <template #header>
          <TableColumnHeader
            :label="t('templates.testDataSets.updatedAt')"
            v-model="columnFilters.updatedAt"
          />
        </template>
        <template #default="{ row }">
          {{ new Date(row.updatedAt).toLocaleString() }}
        </template>
      </el-table-column>
      <el-table-column :label="t('templates.testDataSets.actions')" min-width="220">
        <template #default="{ row }">
          <el-button link type="primary" :disabled="row.locked" @click.stop="openEditDialog(row)">
            {{ t('templates.testDataSets.edit') }}
          </el-button>
          <el-button
            v-if="row.locked"
            link
            type="primary"
            @click.stop="handleDerive(row.testDataSetId)"
          >
            {{ t('templates.testDataSets.derive') }}
          </el-button>
          <el-button
            link
            type="danger"
            :disabled="row.locked"
            @click.stop="handleDelete(row.testDataSetId)"
          >
            {{ t('templates.testDataSets.delete') }}
          </el-button>
        </template>
      </el-table-column>
    </AppDataTable>
    <p v-if="selectedId" class="selection-hint">
      {{ t('templates.testDataSets.selectedHint', { testDataSetId: selectedId }) }}
    </p>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? t('templates.testDataSets.editTitle') : t('templates.testDataSets.createTitle')"
      width="520px"
    >
      <el-form label-position="top">
        <el-form-item :label="t('templates.testDataSets.name')">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item :label="t('templates.testDataSets.descriptionLabel')">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item :label="t('templates.testDataSets.scenarioName')">
          <el-input v-model="form.scenarioName" />
        </el-form-item>
        <el-form-item :label="t('templates.testDataSets.coverageTags')">
          <el-input v-model="coverageTagsText" />
        </el-form-item>
        <el-form-item :label="t('templates.testDataSets.required')">
          <el-switch v-model="form.required" />
        </el-form-item>
        <el-form-item :label="t('templates.testDataSets.variablesJson')">
          <el-input v-model="variablesJson" type="textarea" :rows="8" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('templates.testDataSets.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">
          {{ t('templates.testDataSets.save') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.action-row {
  margin-bottom: 1rem;
}

.locked-tag {
  margin-left: 0.5rem;
}

.selection-hint {
  margin: 0.75rem 0 0;
  color: var(--text-muted);
}
</style>
