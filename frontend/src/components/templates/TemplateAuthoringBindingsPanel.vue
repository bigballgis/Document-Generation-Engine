<script setup lang="ts">

import { computed, onMounted, reactive, ref } from 'vue'

import { useI18n } from 'vue-i18n'

import SectionPanelHeader from '@/components/common/SectionPanelHeader.vue'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'

import TableColumnHeader from '@/components/common/TableColumnHeader.vue'

import ControlledStructuredContentEditor from '@/components/authoring/ControlledStructuredContentEditor.vue'

import { getMaster } from '@/api/masters'

import { DEFAULT_STRUCTURED_CONTENT_JSON } from '@/utils/structuredContentNodes'

import { buildMasterAnchorBindingRows, type MasterAnchorBindingRow } from '@/utils/masterAnchorBindingRows'

import { useDataTableFilters } from '@/composables/useDataTableFilters'

import { resolveApiErrorMessageKey } from '@/api/errorEnvelope'

import { useTemplatesStore } from '@/stores/templates'

import type {

  AnchorBinding,

  BindingValidationResult,

  CompositionRule,

  CompositionRuleInput,

  TemplateContentModuleReference,

  UpsertBindingPayload,

  VariableSchema,

} from '@/types/template'

import type { MasterAnchor } from '@/types/master'

import { ElMessage } from 'element-plus'



const props = defineProps<{

  templateId: string

  masterId: string

  variables: VariableSchema[]

  bindings: AnchorBinding[]

  rules: CompositionRule[] | null

  contentModuleReferences: TemplateContentModuleReference[]

}>()



const emit = defineEmits<{

  updated: []

}>()



const { t, te } = useI18n()

const templatesStore = useTemplatesStore()



type PanelMode = 'list' | 'edit'



const panelMode = ref<PanelMode>('list')

const masterAnchors = ref<MasterAnchor[]>([])

const loadingMaster = ref(false)

const validating = ref(false)

const editingAnchorId = ref<string | null>(null)

const validationResult = ref<BindingValidationResult | null>(null)

const visibilityEnabled = ref(false)

const visibilityExpression = ref('')



const contentTypes = ['TEXT', 'RICH_TEXT', 'TABLE', 'IMAGE', 'CLAUSE', 'SEAL', 'QR_CODE', 'ATTACHMENT_LIST']



const bindingForm = reactive<UpsertBindingPayload>({

  anchorId: '',

  declaredContentType: 'TEXT',

  structuredContentJson: DEFAULT_STRUCTURED_CONTENT_JSON,

})



const anchorRowsSource = computed(() =>

  buildMasterAnchorBindingRows(masterAnchors.value, props.bindings),

)



const contentModuleReferenceKeys = computed(() =>

  props.contentModuleReferences.map((reference) => reference.referenceKey),

)



const { filters: bindingColumnFilters, filteredRows: filteredAnchorRows } = useDataTableFilters(

  anchorRowsSource,

  [

    { key: 'anchorId', getValue: (row) => row.anchorId },

    { key: 'displayLabel', getValue: (row) => row.displayLabel },

    {

      key: 'declaredContentType',

      getValue: (row) => row.declaredContentType ?? '',

      matchMode: 'exact',

    },

    { key: 'validationStatus', getValue: (row) => row.validationStatus ?? '' },

  ],

)



const contentTypeFilterOptions = computed(() =>

  contentTypes.map((type) => ({ value: type, label: type })),

)



const configuredBindingCount = computed(() => props.bindings.length)



const editingRow = computed(() =>

  anchorRowsSource.value.find((row) => row.anchorId === editingAnchorId.value) ?? null,

)



function toRuleInput(rule: CompositionRule): CompositionRuleInput {

  return {

    ruleId: rule.ruleId,

    conditionExpression: rule.conditionExpression,

    targetAnchorId: rule.targetAnchorId,

    trueBranchRuleId: rule.trueBranchRuleId ?? undefined,

    falseBranchRuleId: rule.falseBranchRuleId ?? undefined,

  }

}



function mergeAnchorVisibilityRule(

  existingRules: CompositionRule[],

  anchorId: string,

  enabled: boolean,

  expression: string,

): CompositionRuleInput[] {

  const others = existingRules.filter((rule) => rule.targetAnchorId !== anchorId)

  if (!enabled || !expression.trim()) {

    return others.map(toRuleInput)

  }

  const existing = existingRules.find((rule) => rule.targetAnchorId === anchorId)

  return [

    ...others.map(toRuleInput),

    {

      ruleId: existing?.ruleId ?? `visibility-${anchorId}`,

      conditionExpression: expression.trim(),

      targetAnchorId: anchorId,

    },

  ]

}



onMounted(async () => {

  loadingMaster.value = true

  try {

    const master = await getMaster(props.masterId)

    masterAnchors.value = master.anchors

  } catch {

    ElMessage.error(t('templates.authoring.masterAnchorsLoadFailed'))

  } finally {

    loadingMaster.value = false

  }

})



function resolveValidationStatusLabel(status: string | undefined | null): string {

  if (!status) {

    return t('templates.authoring.validationUnknown')

  }

  const key = `templates.bindingGate.status.${status}`

  return te(key) ? t(key) : status

}



function resolveConfiguredLabel(row: MasterAnchorBindingRow): string {

  return row.configured

    ? t('templates.authoring.bindingConfigured')

    : t('templates.authoring.bindingNotConfigured')

}



function loadVisibilityRuleForAnchor(anchorId: string) {

  const rule = (props.rules ?? []).find((item) => item.targetAnchorId === anchorId)

  if (rule) {

    visibilityEnabled.value = true

    visibilityExpression.value = rule.conditionExpression

  } else {

    visibilityEnabled.value = false

    visibilityExpression.value = ''

  }

}



function openEditPanel(row: MasterAnchorBindingRow) {

  editingAnchorId.value = row.anchorId

  bindingForm.anchorId = row.anchorId

  if (row.binding) {

    bindingForm.declaredContentType = row.binding.declaredContentType

    bindingForm.structuredContentJson = row.binding.structuredContentJson ?? DEFAULT_STRUCTURED_CONTENT_JSON

  } else {

    bindingForm.declaredContentType = 'TEXT'

    bindingForm.structuredContentJson = DEFAULT_STRUCTURED_CONTENT_JSON

  }

  loadVisibilityRuleForAnchor(row.anchorId)

  panelMode.value = 'edit'

}



function backToList() {

  panelMode.value = 'list'

  editingAnchorId.value = null

}



async function handleSaveBinding() {

  try {

    await templatesStore.upsertBinding(props.templateId, bindingForm.anchorId, { ...bindingForm })

    const mergedRules = mergeAnchorVisibilityRule(

      props.rules ?? [],

      bindingForm.anchorId,

      visibilityEnabled.value,

      visibilityExpression.value,

    )

    await templatesStore.saveRules(props.templateId, mergedRules)

    ElMessage.success(t('templates.authoring.saveBindingSuccess'))

    emit('updated')

    backToList()

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
    <template v-if="panelMode === 'list'">
      <SectionPanelHeader
        :title="t('templates.authoring.bindingsTitle')"
        :help-title="t('templates.authoring.bindingsHelpTitle')"
        :help-content="t('templates.authoring.bindingsHelpDescription')"
      >
        <template #actions>
          <el-button
            type="primary"
            :loading="validating"
            :disabled="configuredBindingCount === 0"
            @click="handleValidateBindings"
          >
            {{ t('templates.authoring.validateBindings') }}
          </el-button>
        </template>
      </SectionPanelHeader>



      <AppDataTable v-loading="loadingMaster" :data="filteredAnchorRows" empty-text="">

        <template #empty>

          <el-empty :description="t('templates.authoring.noMasterAnchors')" />

        </template>

        <el-table-column prop="anchorId" width="160">

          <template #header>

            <TableColumnHeader

              :label="t('templates.authoring.anchorId')"

              v-model="bindingColumnFilters.anchorId"

            />

          </template>

        </el-table-column>

        <el-table-column prop="displayLabel" min-width="200">

          <template #header>

            <TableColumnHeader

              :label="t('templates.authoring.anchorDisplayLabel')"

              v-model="bindingColumnFilters.displayLabel"

            />

          </template>

        </el-table-column>

        <el-table-column prop="declaredContentType" width="140">

          <template #header>

            <TableColumnHeader

              :label="t('templates.authoring.contentType')"

              v-model="bindingColumnFilters.declaredContentType"

              filter-type="select"

              :options="contentTypeFilterOptions"

            />

          </template>

          <template #default="{ row }">

            {{ row.declaredContentType ?? '—' }}

          </template>

        </el-table-column>

        <el-table-column prop="validationStatus" width="140">

          <template #header>

            <TableColumnHeader

              :label="t('templates.authoring.validationStatus')"

              v-model="bindingColumnFilters.validationStatus"

            />

          </template>

          <template #default="{ row }">

            {{ row.configured ? resolveValidationStatusLabel(row.validationStatus) : '—' }}

          </template>

        </el-table-column>

        <el-table-column width="120">

          <template #header>

            <span>{{ t('templates.authoring.bindingStatus') }}</span>

          </template>

          <template #default="{ row }">

            <el-tag :type="row.configured ? 'success' : 'info'" size="small">

              {{ resolveConfiguredLabel(row) }}

            </el-tag>

          </template>

        </el-table-column>

        <el-table-column width="120" fixed="right" :label="t('common.actions')">

          <template #default="{ row }">

            <el-button link type="primary" @click="openEditPanel(row)">

              {{ row.configured ? t('common.edit') : t('templates.authoring.configureBinding') }}

            </el-button>

          </template>

        </el-table-column>

      </AppDataTable>



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

    </template>



    <template v-else>

      <div class="binding-editor">

        <div class="binding-editor__toolbar">

          <el-button @click="backToList">{{ t('common.back') }}</el-button>

          <div class="binding-editor__title">

            <strong>{{ editingRow?.anchorId }}</strong>

            <span v-if="editingRow?.displayLabel" class="binding-editor__subtitle">

              {{ editingRow.displayLabel }}

            </span>

          </div>

          <el-button type="primary" :loading="templatesStore.submitting" @click="handleSaveBinding">

            {{ t('common.save') }}

          </el-button>

        </div>



        <p class="binding-editor__hint">{{ t('templates.authoring.bindingEditorSubtitle') }}</p>



        <el-form label-position="top" class="binding-form">

          <el-form-item :label="t('templates.authoring.contentType')">

            <AppSearchSelect v-model="bindingForm.declaredContentType" style="width: 100%">

              <el-option v-for="type in contentTypes" :key="type" :label="type" :value="type" />

            </AppSearchSelect>

          </el-form-item>



          <div class="visibility-section">

            <h4>{{ t('templates.authoring.visibilityCondition.title') }}</h4>

            <p class="visibility-section__hint">{{ t('templates.authoring.visibilityCondition.description') }}</p>

            <el-form-item>

              <el-checkbox v-model="visibilityEnabled">

                {{ t('templates.authoring.visibilityCondition.enable') }}

              </el-checkbox>

            </el-form-item>

            <el-form-item

              v-if="visibilityEnabled"

              :label="t('templates.authoring.visibilityCondition.expression')"

            >

              <el-input

                v-model="visibilityExpression"

                :placeholder="t('templates.authoring.visibilityCondition.expressionPlaceholder')"

              />

            </el-form-item>

          </div>



          <el-form-item :label="t('templates.authoring.structuredContentEditor')">

            <ControlledStructuredContentEditor

              v-model="bindingForm.structuredContentJson"

              :template-id="templateId"

              :variables="variables"

              :content-module-reference-keys="contentModuleReferenceKeys"

            />

          </el-form-item>

        </el-form>

      </div>

    </template>

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



.validation-summary {

  margin-top: 1rem;

}



.binding-editor {

  &__toolbar {

    display: flex;

    align-items: center;

    gap: 1rem;

    margin-bottom: 0.75rem;

  }



  &__title {

    flex: 1;

    min-width: 0;

  }



  &__subtitle {

    display: block;

    margin-top: 0.25rem;

    color: var(--text-muted);

    font-size: 0.875rem;

    font-weight: normal;

  }



  &__hint {

    margin: 0 0 1rem;

    color: var(--text-muted);

  }

}



.visibility-section {

  margin-bottom: 1rem;

  padding: 0.75rem;

  border: 1px solid var(--border-color);

  border-radius: var(--radius-md);

  background: var(--surface-muted);



  h4 {

    margin: 0 0 0.25rem;

    font-size: 0.9375rem;

    font-weight: 650;

  }



  &__hint {

    margin: 0 0 0.75rem;

    color: var(--text-muted);

    font-size: 0.875rem;

  }

}

</style>

