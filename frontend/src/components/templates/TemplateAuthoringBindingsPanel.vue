<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import ControlledStructuredContentEditor from '@/components/authoring/ControlledStructuredContentEditor.vue'
import { DEFAULT_STRUCTURED_CONTENT_JSON } from '@/utils/structuredContentNodes'
import { useDataTableFilters } from '@/composables/useDataTableFilters'
import { useCatalogPagination } from '@/composables/useCatalogPagination'
import { CLIENT_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import { resolveApiErrorMessageKey } from '@/api/errorEnvelope'
import { useTemplatesStore } from '@/stores/templates'
import type { AnchorBinding, BindingValidationResult, UpsertBindingPayload, VariableSchema } from '@/types/template'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  templateId: string
  variables: VariableSchema[]
  bindings: AnchorBinding[]
}>()

const emit = defineEmits<{
  updated: []
}>()

const { t, te } = useI18n()
const templatesStore = useTemplatesStore()

const validating = ref(false)
const bindingDialogOpen = ref(false)
const editingAnchorId = ref<string | null>(null)
const selectedAnchorId = ref<string | null>(null)
const validationResult = ref<BindingValidationResult | null>(null)

const contentTypes = ['TEXT', 'RICH_TEXT', 'TABLE', 'IMAGE', 'CLAUSE', 'SEAL', 'QR_CODE', 'ATTACHMENT_LIST']

const bindingForm = reactive<UpsertBindingPayload>({
  anchorId: '',
  declaredContentType: 'TEXT',
  structuredContentJson: DEFAULT_STRUCTURED_CONTENT_JSON,
})

const hasBindings = computed(() => props.bindings.length > 0)

const selectedBinding = computed(() =>
  props.bindings.find((binding) => binding.anchorId === selectedAnchorId.value) ?? null,
)

const bindingsSource = computed(() => props.bindings)
const { filters: bindingColumnFilters, filteredRows: filteredBindings } = useDataTableFilters(
  bindingsSource,
  [
    { key: 'anchorId', getValue: (row) => row.anchorId },
    { key: 'declaredContentType', getValue: (row) => row.declaredContentType, matchMode: 'exact' },
    { key: 'validationStatus', getValue: (row) => row.validationStatus ?? '' },
  ],
)

const bindingsCurrentPage = ref(1)
const { paginatedRows: paginatedBindings, totalRows: totalBindingRows } = useCatalogPagination(
  filteredBindings,
  bindingsCurrentPage,
  CLIENT_TABLE_PAGE_SIZE,
)

const contentTypeFilterOptions = computed(() =>
  contentTypes.map((type) => ({ value: type, label: type })),
)

watch(
  () => props.bindings,
  (bindings) => {
    if (selectedAnchorId.value && bindings.some((binding) => binding.anchorId === selectedAnchorId.value)) {
      return
    }
    selectedAnchorId.value = bindings[0]?.anchorId ?? null
  },
  { immediate: true },
)

watch(selectedBinding, (binding) => {
  if (!binding) {
    return
  }
  bindingForm.anchorId = binding.anchorId
  bindingForm.declaredContentType = binding.declaredContentType
  bindingForm.structuredContentJson = binding.structuredContentJson ?? DEFAULT_STRUCTURED_CONTENT_JSON
  editingAnchorId.value = binding.anchorId
})

function resetBindingForm() {
  bindingForm.anchorId = ''
  bindingForm.declaredContentType = 'TEXT'
  bindingForm.structuredContentJson = DEFAULT_STRUCTURED_CONTENT_JSON
  editingAnchorId.value = null
}

function openAddBinding() {
  resetBindingForm()
  bindingDialogOpen.value = true
}

function selectBinding(binding: AnchorBinding) {
  selectedAnchorId.value = binding.anchorId
}

function resolveValidationStatusLabel(status: string | undefined): string {
  if (!status) {
    return t('templates.authoring.validationUnknown')
  }
  const key = `templates.bindingGate.status.${status}`
  return te(key) ? t(key) : status
}

async function handleSaveBinding() {
  try {
    await templatesStore.upsertBinding(props.templateId, bindingForm.anchorId, { ...bindingForm })
    if (bindingDialogOpen.value) {
      bindingDialogOpen.value = false
    }
    selectedAnchorId.value = bindingForm.anchorId
    ElMessage.success(t('templates.authoring.saveBindingSuccess'))
    emit('updated')
  } catch {
    ElMessage.error(t('templates.error.saveBinding'))
  }
}

async function handleValidateBindings() {
  validating.value = true
  validationResult.value = null
  try {
    const result = await templatesStore.validateBindings(props.templateId)
    validationResult.value = result
    emit('updated')
    if (result.summary.blocking) {
      ElMessage.warning(t('templates.authoring.bindingValidationBlocking'))
    } else {
      ElMessage.success(t('templates.authoring.bindingValidationSuccess'))
    }
  } catch (error) {
    const messageKey = resolveApiErrorMessageKey(error, 'templates.error.bindingValidation')
    ElMessage.error(te(messageKey) ? t(messageKey) : t('templates.error.bindingValidation'))
  } finally {
    validating.value = false
  }
}
</script>

<template>
  <div class="bindings-panel authoring-panel">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="bindings-help"
      :title="t('templates.authoring.bindingsHelpTitle')"
      :description="t('templates.authoring.bindingsHelpDescription')"
    />

    <div class="section-header">
      <p class="section-description">{{ t('templates.authoring.bindingsDescription') }}</p>
      <el-button type="primary" plain @click="openAddBinding">
        {{ t('templates.authoring.addBinding') }}
      </el-button>
    </div>

    <AppDataTable
      activatable
      :data="paginatedBindings"
      empty-text=""
      highlight-current-row
      @row-click="selectBinding"
    >
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
      <el-table-column prop="declaredContentType" sortable width="140">
        <template #header>
          <TableColumnHeader
            :label="t('templates.authoring.contentType')"
            v-model="bindingColumnFilters.declaredContentType"
            filter-type="select"
            :options="contentTypeFilterOptions"
          />
        </template>
      </el-table-column>
      <el-table-column prop="validationStatus" width="160">
        <template #header>
          <TableColumnHeader
            :label="t('templates.authoring.validationStatus')"
            v-model="bindingColumnFilters.validationStatus"
          />
        </template>
        <template #default="{ row }">
          {{ resolveValidationStatusLabel(row.validationStatus) }}
        </template>
      </el-table-column>
    </AppDataTable>
    <AppTablePagination
      v-model:current-page="bindingsCurrentPage"
      :page-size="CLIENT_TABLE_PAGE_SIZE"
      :total="totalBindingRows"
    />

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

    <el-alert
      v-if="validationResult"
      class="validation-summary"
      :type="validationResult.summary.blocking ? 'warning' : 'success'"
      :closable="false"
      show-icon
      :title="
        t('templates.authoring.bindingValidationSummary', {
          valid: validationResult.summary.validCount,
          total: validationResult.summary.totalBindings,
        })
      "
    />

    <el-card v-if="selectedBinding" shadow="never" class="binding-editor-card">
      <template #header>
        <div class="binding-editor-card__header">
          <div>
            <strong>{{ selectedBinding.anchorId }}</strong>
            <span class="binding-editor-card__subtitle">
              {{ t('templates.authoring.bindingEditorSubtitle') }}
            </span>
          </div>
          <el-button type="primary" :loading="templatesStore.submitting" @click="handleSaveBinding">
            {{ t('common.save') }}
          </el-button>
        </div>
      </template>

      <el-form label-position="top" class="binding-form">
        <el-form-item :label="t('templates.authoring.contentType')">
          <AppSearchSelect v-model="bindingForm.declaredContentType" style="width: 100%">
            <el-option v-for="type in contentTypes" :key="type" :label="type" :value="type" />
          </AppSearchSelect>
        </el-form-item>
        <el-form-item :label="t('templates.authoring.structuredContentEditor')">
          <ControlledStructuredContentEditor
            v-model="bindingForm.structuredContentJson"
            :template-id="templateId"
            :variables="variables"
          />
        </el-form-item>
      </el-form>
    </el-card>

    <el-dialog
      v-model="bindingDialogOpen"
      :title="t('templates.authoring.addBinding')"
      width="760px"
    >
      <el-form label-position="top">
        <el-form-item :label="t('templates.authoring.anchorId')">
          <el-input v-model="bindingForm.anchorId" />
        </el-form-item>
        <el-form-item :label="t('templates.authoring.contentType')">
          <AppSearchSelect v-model="bindingForm.declaredContentType" style="width: 100%">
            <el-option v-for="type in contentTypes" :key="type" :label="type" :value="type" />
          </AppSearchSelect>
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
.bindings-panel {
  .section-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 1rem;
    margin-bottom: 0.75rem;
  }

  .section-description {
    margin: 0;
    color: var(--text-muted);
    max-width: 40rem;
  }
}

.bindings-help {
  margin-bottom: 1rem;
}

.action-row {
  margin-top: 1rem;
}

.validation-summary {
  margin-top: 1rem;
}

.binding-editor-card {
  margin-top: 1.25rem;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
  }

  &__subtitle {
    display: block;
    margin-top: 0.25rem;
    color: var(--text-muted);
    font-size: 0.875rem;
    font-weight: normal;
  }
}
</style>
