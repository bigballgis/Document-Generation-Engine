<script setup lang="ts">
/* eslint-disable vue/no-mutating-props -- reactive form bag owned by parent */
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessageBox } from 'element-plus'
import type { VariableSchema } from '@/types/template'
import type { ApiFieldError } from '@/types/session'
import { resolveVariableDisplayName } from '@/utils/variableDisplayName'
import {
  buildSchemaSkeleton,
  enterableVariables,
  fieldErrorsToMap,
  isNonEmptyVariablesPayload,
  mapApiFieldErrors,
  parseEnumValues,
  parseVariablesJson,
  shouldExpandAdvancedJson,
  stringifyVariablesJson,
  stripComputeKeys,
  validateVariablesAgainstSchema,
  type SchemaFieldError,
} from '@/utils/testDataSetSchemaForm'
import {
  isPiiMarkedField,
  payloadTouchesPiiFields,
  type TestDataSetPiiHandling,
  type TestDataSetSavePayload,
} from '@/utils/testDataSetPii'

const props = defineProps<{
  modelValue: boolean
  editingId: string | null
  saving: boolean
  form: {
    name: string
    description: string
    scenarioName: string
    required: boolean
  }
  coverageTagsText: string
  variables: VariableSchema[]
  initialVariables: Record<string, unknown>
  serverFieldErrors?: ApiFieldError[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'update:coverageTagsText': [value: string]
  save: [payload: TestDataSetSavePayload]
  'clear-server-errors': []
}>()

const { t } = useI18n()

const variablesModel = reactive<Record<string, unknown>>({})
const variablesJson = ref('{}')
const jsonSyncErrorKey = ref<string | null>(null)
const fieldErrorKeys = ref<Record<string, string>>({})
const summaryErrors = ref<SchemaFieldError[]>([])
const advancedActiveNames = ref<string[]>([])
const applyingJson = ref(false)
const applyingForm = ref(false)
const complexFieldText = reactive<Record<string, string>>({})

/** CE-G03 — recommended default when PII gate is active. */
const piiHandling = ref<TestDataSetPiiHandling | null>('SYNTHETIC')
const explicitConfirmVisible = ref(false)
const piiConfirmReason = ref('')
const secondaryConfirmed = ref(false)
const explicitConfirmErrorKey = ref<string | null>(null)
const pendingSaveVariables = ref<Record<string, unknown> | null>(null)

const enterable = computed(() => enterableVariables(props.variables))
const schemaEmpty = computed(() => enterable.value.length === 0)

const touchesPii = computed(() =>
  payloadTouchesPiiFields(props.variables, { ...variablesModel }),
)

watch(touchesPii, (active) => {
  if (active && piiHandling.value == null) {
    piiHandling.value = 'SYNTHETIC'
  }
  if (!active) {
    piiHandling.value = 'SYNTHETIC'
    explicitConfirmVisible.value = false
    piiConfirmReason.value = ''
    secondaryConfirmed.value = false
    explicitConfirmErrorKey.value = null
  }
})

function clearClientErrors() {
  fieldErrorKeys.value = {}
  summaryErrors.value = []
  jsonSyncErrorKey.value = null
}

function applyErrors(errors: SchemaFieldError[]) {
  summaryErrors.value = errors
  fieldErrorKeys.value = fieldErrorsToMap(errors)
}

function syncComplexFieldText() {
  for (const key of Object.keys(complexFieldText)) {
    delete complexFieldText[key]
  }
  for (const variable of enterable.value) {
    if (variable.variableType === 'LIST' || variable.variableType === 'OBJECT') {
      const current = variablesModel[variable.variableKey]
      complexFieldText[variable.variableKey] = stringifyVariablesJson(
        current ?? (variable.variableType === 'LIST' ? [] : {}),
      )
    }
  }
}

function replaceModel(source: Record<string, unknown>) {
  for (const key of Object.keys(variablesModel)) {
    delete variablesModel[key]
  }
  Object.assign(variablesModel, source)
  applyingForm.value = true
  variablesJson.value = stringifyVariablesJson(variablesModel)
  syncComplexFieldText()
  void nextTick(() => {
    applyingForm.value = false
  })
}

function resetPiiControls() {
  piiHandling.value = 'SYNTHETIC'
  explicitConfirmVisible.value = false
  piiConfirmReason.value = ''
  secondaryConfirmed.value = false
  explicitConfirmErrorKey.value = null
  pendingSaveVariables.value = null
}

function resetModelFromInitial() {
  const source =
    props.editingId != null || Object.keys(props.initialVariables).length > 0
      ? { ...props.initialVariables }
      : buildSchemaSkeleton(props.variables)
  replaceModel(source)
  advancedActiveNames.value = shouldExpandAdvancedJson(props.variables, variablesJson.value)
    ? ['advanced']
    : []
  clearClientErrors()
  resetPiiControls()
}

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      resetModelFromInitial()
    }
  },
  { immediate: true },
)

watch(
  () => props.serverFieldErrors,
  (errors) => {
    if (errors?.length) {
      applyErrors(mapApiFieldErrors(errors))
    }
  },
)

watch(
  variablesModel,
  () => {
    if (applyingJson.value) {
      return
    }
    applyingForm.value = true
    variablesJson.value = stringifyVariablesJson(variablesModel)
    void nextTick(() => {
      applyingForm.value = false
    })
    emit('clear-server-errors')
  },
  { deep: true },
)

function onJsonInput(value: string) {
  variablesJson.value = value
  if (applyingForm.value) {
    return
  }
  const parsed = parseVariablesJson(value)
  if (!parsed.ok) {
    jsonSyncErrorKey.value = parsed.error.messageKey
    return
  }
  jsonSyncErrorKey.value = null
  applyingJson.value = true
  for (const key of Object.keys(variablesModel)) {
    delete variablesModel[key]
  }
  Object.assign(variablesModel, parsed.value)
  syncComplexFieldText()
  void nextTick(() => {
    applyingJson.value = false
  })
  emit('clear-server-errors')
}

async function handleGenerateSkeleton() {
  const skeleton = buildSchemaSkeleton(props.variables)
  if (
    isNonEmptyVariablesPayload(variablesModel) &&
    JSON.stringify(variablesModel) !== JSON.stringify(skeleton)
  ) {
    try {
      await ElMessageBox.confirm(
        t('templates.testDataSets.skeletonOverwriteMessage'),
        t('templates.testDataSets.skeletonOverwriteTitle'),
        {
          type: 'warning',
          confirmButtonText: t('common.confirm'),
          cancelButtonText: t('common.cancel'),
        },
      )
    } catch {
      return
    }
  }
  replaceModel(skeleton)
  clearClientErrors()
}

function setFieldValue(key: string, value: unknown) {
  variablesModel[key] = value
}

function onComplexFieldInput(variable: VariableSchema, raw: string) {
  complexFieldText[variable.variableKey] = raw
  try {
    const parsed: unknown = JSON.parse(raw || (variable.variableType === 'LIST' ? '[]' : '{}'))
    setFieldValue(variable.variableKey, parsed)
  } catch {
    // keep typing until JSON is valid; Save will catch via overall JSON sync
  }
}

function validateFieldBlur(variableKey: string) {
  const errs = validateVariablesAgainstSchema(props.variables, { ...variablesModel })
  const match = errs.find((item) => item.field === variableKey)
  const next = { ...fieldErrorKeys.value }
  if (match) {
    next[variableKey] = match.messageKey
  } else {
    delete next[variableKey]
  }
  fieldErrorKeys.value = next
}

function fieldLabel(variable: VariableSchema): string {
  return resolveVariableDisplayName(variable)
}

function enumOptions(variable: VariableSchema): string[] {
  return parseEnumValues(variable.enumValues)
}

function numberModelValue(key: string): number | undefined {
  const value = variablesModel[key]
  return typeof value === 'number' ? value : undefined
}

function stringModelValue(key: string): string {
  const value = variablesModel[key]
  return value == null ? '' : String(value)
}

function emitSave(variables: Record<string, unknown>, pii?: Partial<TestDataSetSavePayload>) {
  clearClientErrors()
  const payload: TestDataSetSavePayload = { variables }
  if (pii?.piiHandling) {
    payload.piiHandling = pii.piiHandling
  }
  if (pii?.piiConfirmReason) {
    payload.piiConfirmReason = pii.piiConfirmReason
  }
  if (pii?.secondaryConfirmed != null) {
    payload.secondaryConfirmed = pii.secondaryConfirmed
  }
  emit('save', payload)
}

function handleSave() {
  if (!props.form.name.trim()) {
    applyErrors([
      {
        field: 'name',
        reason: 'REQUIRED',
        messageKey: 'templates.testDataSets.validation.nameRequired',
      },
    ])
    return
  }

  const parsed = parseVariablesJson(variablesJson.value)
  if (!parsed.ok) {
    jsonSyncErrorKey.value = parsed.error.messageKey
    applyErrors([parsed.error])
    return
  }

  const stripped = stripComputeKeys(props.variables, parsed.value)
  const errors = validateVariablesAgainstSchema(props.variables, stripped)
  if (errors.length > 0) {
    applyErrors(errors)
    return
  }

  const needsPiiGate = payloadTouchesPiiFields(props.variables, stripped)
  if (needsPiiGate) {
    if (piiHandling.value !== 'SYNTHETIC' && piiHandling.value !== 'EXPLICIT_SENSITIVE') {
      applyErrors([
        {
          field: 'piiHandling',
          reason: 'REQUIRED',
          messageKey: 'templates.testDataSets.pii.handlingRequired',
        },
      ])
      return
    }
    if (piiHandling.value === 'EXPLICIT_SENSITIVE') {
      pendingSaveVariables.value = stripped
      piiConfirmReason.value = ''
      secondaryConfirmed.value = false
      explicitConfirmErrorKey.value = null
      explicitConfirmVisible.value = true
      return
    }
    emitSave(stripped, { piiHandling: 'SYNTHETIC' })
    return
  }

  emitSave(stripped)
}

function submitExplicitConfirm() {
  const reason = piiConfirmReason.value.trim()
  if (!reason) {
    explicitConfirmErrorKey.value = 'templates.testDataSets.pii.reasonRequired'
    return
  }
  if (!secondaryConfirmed.value) {
    explicitConfirmErrorKey.value = 'templates.testDataSets.pii.secondaryConfirmRequired'
    return
  }
  const variables = pendingSaveVariables.value
  if (!variables) {
    return
  }
  explicitConfirmVisible.value = false
  emitSave(variables, {
    piiHandling: 'EXPLICIT_SENSITIVE',
    piiConfirmReason: reason,
    secondaryConfirmed: true,
  })
}

function cancelExplicitConfirm() {
  explicitConfirmVisible.value = false
  pendingSaveVariables.value = null
  explicitConfirmErrorKey.value = null
}

function onExplicitDialogClosed() {
  pendingSaveVariables.value = null
  explicitConfirmErrorKey.value = null
}

/** Test / fail-closed helper — clears handling so client gate can block (BDD-013). */
function clearPiiHandling() {
  piiHandling.value = null
}

function setPiiHandlingForTest(value: TestDataSetPiiHandling | null) {
  piiHandling.value = value
}

function setExplicitConfirmFieldsForTest(reason: string, confirmed: boolean) {
  piiConfirmReason.value = reason
  secondaryConfirmed.value = confirmed
}

defineExpose({
  clearPiiHandling,
  setPiiHandlingForTest,
  setExplicitConfirmFieldsForTest,
  piiHandling,
  secondaryConfirmed,
  piiConfirmReason,
  explicitConfirmVisible,
})
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="editingId ? t('templates.testDataSets.editTitle') : t('templates.testDataSets.createTitle')"
    width="640px"
    class="test-data-set-edit-dialog"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form label-position="top" data-testid="test-data-set-edit-form">
      <el-form-item
        :label="t('templates.testDataSets.name')"
        :error="fieldErrorKeys.name ? t(fieldErrorKeys.name) : undefined"
      >
        <el-input v-model="form.name" data-testid="test-data-set-name" />
      </el-form-item>
      <el-form-item :label="t('templates.testDataSets.descriptionLabel')">
        <el-input v-model="form.description" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item :label="t('templates.testDataSets.scenarioName')">
        <el-input v-model="form.scenarioName" />
      </el-form-item>
      <el-form-item :label="t('templates.testDataSets.coverageTags')">
        <el-input
          :model-value="coverageTagsText"
          @update:model-value="emit('update:coverageTagsText', $event)"
        />
      </el-form-item>
      <el-form-item :label="t('templates.testDataSets.required')">
        <el-switch v-model="form.required" />
      </el-form-item>

      <div class="schema-form-section">
        <div class="schema-form-section__header">
          <h3>{{ t('templates.testDataSets.schemaFormTitle') }}</h3>
          <el-button data-testid="generate-schema-skeleton" @click="handleGenerateSkeleton">
            {{ t('templates.testDataSets.generateSkeleton') }}
          </el-button>
        </div>

        <el-alert
          v-if="schemaEmpty"
          type="info"
          :closable="false"
          show-icon
          class="schema-form-empty"
          :title="t('templates.testDataSets.schemaEmpty')"
        />

        <template v-else>
          <el-form-item
            v-for="variable in enterable"
            :key="variable.variableKey"
            :required="variable.required"
            :error="
              fieldErrorKeys[variable.variableKey]
                ? t(fieldErrorKeys[variable.variableKey])
                : undefined
            "
            :data-testid="`schema-field-${variable.variableKey}`"
          >
            <template #label>
              <span class="schema-field-label">
                <span>{{ fieldLabel(variable) }}</span>
                <el-tag
                  v-if="isPiiMarkedField(variable)"
                  size="small"
                  type="warning"
                  effect="plain"
                  class="pii-field-badge"
                  :data-testid="`pii-badge-${variable.variableKey}`"
                >
                  {{ t('templates.testDataSets.pii.badge') }}
                </el-tag>
              </span>
            </template>
            <el-input
              v-if="variable.variableType === 'TEXT' || variable.variableType === 'DATE'"
              :model-value="stringModelValue(variable.variableKey)"
              :type="variable.variableType === 'DATE' ? 'date' : 'text'"
              :data-testid="`schema-input-${variable.variableKey}`"
              @update:model-value="setFieldValue(variable.variableKey, $event)"
              @blur="validateFieldBlur(variable.variableKey)"
            />
            <el-input-number
              v-else-if="variable.variableType === 'NUMBER' || variable.variableType === 'AMOUNT'"
              :model-value="numberModelValue(variable.variableKey)"
              :controls="false"
              class="schema-number-input"
              :data-testid="`schema-input-${variable.variableKey}`"
              @update:model-value="setFieldValue(variable.variableKey, $event ?? null)"
              @blur="validateFieldBlur(variable.variableKey)"
            />
            <el-switch
              v-else-if="variable.variableType === 'BOOLEAN'"
              :model-value="Boolean(variablesModel[variable.variableKey])"
              :data-testid="`schema-input-${variable.variableKey}`"
              @update:model-value="setFieldValue(variable.variableKey, $event)"
            />
            <el-select
              v-else-if="variable.variableType === 'ENUM'"
              :model-value="
                typeof variablesModel[variable.variableKey] === 'string'
                  ? (variablesModel[variable.variableKey] as string)
                  : undefined
              "
              clearable
              class="schema-enum-select"
              :data-testid="`schema-input-${variable.variableKey}`"
              @update:model-value="setFieldValue(variable.variableKey, $event ?? '')"
            >
              <el-option
                v-for="option in enumOptions(variable)"
                :key="option"
                :label="option"
                :value="option"
              />
            </el-select>
            <el-input
              v-else
              :model-value="complexFieldText[variable.variableKey] ?? ''"
              type="textarea"
              :rows="4"
              :data-testid="`schema-input-${variable.variableKey}`"
              @update:model-value="onComplexFieldInput(variable, $event)"
            />
          </el-form-item>
        </template>
      </div>

      <div
        v-if="touchesPii"
        class="pii-handling-section"
        data-testid="pii-handling-group"
      >
        <h3 class="pii-handling-section__title">
          {{ t('templates.testDataSets.pii.handlingTitle') }}
        </h3>
        <p class="pii-handling-section__hint">
          {{ t('templates.testDataSets.pii.handlingHint') }}
        </p>
        <el-radio-group v-model="piiHandling" class="pii-handling-radios">
          <el-radio
            value="SYNTHETIC"
            data-testid="pii-handling-synthetic"
          >
            {{ t('templates.testDataSets.pii.synthetic') }}
          </el-radio>
          <el-radio
            value="EXPLICIT_SENSITIVE"
            data-testid="pii-handling-explicit"
          >
            {{ t('templates.testDataSets.pii.explicitSensitive') }}
          </el-radio>
        </el-radio-group>
        <p
          v-if="fieldErrorKeys.piiHandling"
          class="pii-handling-error"
          data-testid="pii-handling-error"
        >
          {{ t(fieldErrorKeys.piiHandling) }}
        </p>
      </div>

      <el-collapse v-model="advancedActiveNames" class="advanced-json-collapse">
        <el-collapse-item
          :title="t('templates.testDataSets.advancedJson')"
          name="advanced"
          data-testid="advanced-json-collapse"
        >
          <el-form-item
            :label="t('templates.testDataSets.variablesJson')"
            :error="jsonSyncErrorKey ? t(jsonSyncErrorKey) : undefined"
          >
            <el-input
              :model-value="variablesJson"
              type="textarea"
              :rows="10"
              data-testid="advanced-json-editor"
              @update:model-value="onJsonInput"
            />
          </el-form-item>
        </el-collapse-item>
      </el-collapse>

      <ul
        v-if="summaryErrors.length"
        class="field-error-summary"
        data-testid="field-error-summary"
      >
        <li v-for="error in summaryErrors" :key="`${error.field}-${error.reason}`">
          <strong>{{ error.field }}</strong>: {{ t(error.messageKey) }}
        </li>
      </ul>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">
        {{ t('templates.testDataSets.cancel') }}
      </el-button>
      <el-button
        type="primary"
        :loading="saving"
        data-testid="test-data-set-save"
        @click="handleSave"
      >
        {{ t('templates.testDataSets.save') }}
      </el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="explicitConfirmVisible"
    :title="t('templates.testDataSets.pii.explicitConfirmTitle')"
    width="560px"
    :teleported="false"
    data-testid="pii-explicit-confirm-dialog"
    @closed="onExplicitDialogClosed"
  >
    <p class="pii-explicit-intro">
      {{ t('templates.testDataSets.pii.explicitConfirmIntro') }}
    </p>
    <el-form label-position="top">
      <el-form-item
        :label="t('templates.testDataSets.pii.confirmReason')"
        :error="explicitConfirmErrorKey ? t(explicitConfirmErrorKey) : undefined"
        required
      >
        <el-input
          v-model="piiConfirmReason"
          type="textarea"
          :rows="3"
          maxlength="2048"
          show-word-limit
          data-testid="pii-confirm-reason"
        />
      </el-form-item>
      <el-form-item>
        <el-checkbox
          v-model="secondaryConfirmed"
          class="pii-secondary-confirm"
          data-testid="pii-secondary-confirm"
        >
          {{ t('templates.testDataSets.pii.secondaryConfirm') }}
        </el-checkbox>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button data-testid="pii-explicit-confirm-cancel" @click="cancelExplicitConfirm">
        {{ t('common.cancel') }}
      </el-button>
      <el-button
        type="primary"
        data-testid="pii-explicit-confirm-submit"
        @click="submitExplicitConfirm"
      >
        {{ t('templates.testDataSets.pii.confirmSave') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.schema-form-section {
  margin-block: var(--space-4, 16px);
}

.schema-form-section__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3, 12px);
  margin-bottom: var(--space-3, 12px);

  h3 {
    margin: 0;
    font-size: var(--font-size-md, 14px);
    font-weight: 600;
    color: var(--color-text-primary, #1a1a1a);
  }
}

.schema-form-empty {
  margin-bottom: var(--space-3, 12px);
}

.schema-field-label {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2, 8px);
  flex-wrap: wrap;
}

.pii-field-badge {
  font-weight: 600;
}

.schema-number-input,
.schema-enum-select {
  width: 100%;
}

.pii-handling-section {
  margin-block: var(--space-4, 16px);
  padding: var(--space-3, 12px);
  border: 1px solid var(--color-border, #e4e7eb);
  border-radius: var(--radius-sm, 4px);
  background: var(--color-surface-muted, #f7f8fa);
}

.pii-handling-section__title {
  margin: 0 0 var(--space-2, 8px);
  font-size: var(--font-size-md, 14px);
  font-weight: 600;
  color: var(--color-text-primary, #1a1a1a);
}

.pii-handling-section__hint {
  margin: 0 0 var(--space-3, 12px);
  font-size: var(--font-size-sm, 12px);
  color: var(--color-text-secondary, #5c6670);
  line-height: 1.5;
}

.pii-handling-radios {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: var(--space-2, 8px);
}

.pii-handling-error {
  margin: var(--space-2, 8px) 0 0;
  color: var(--el-color-danger);
  font-size: var(--font-size-sm, 12px);
}

.pii-explicit-intro {
  margin: 0 0 var(--space-3, 12px);
  font-size: var(--font-size-sm, 13px);
  color: var(--color-text-secondary, #5c6670);
  line-height: 1.5;
}

.pii-secondary-confirm {
  align-items: flex-start;
  height: auto;
  white-space: normal;

  :deep(.el-checkbox__label) {
    white-space: normal;
    line-height: 1.45;
    color: var(--color-text-primary, #1a1a1a);
  }

  :deep(.el-checkbox__input) {
    margin-top: 2px;
  }
}


.advanced-json-collapse {
  margin-top: var(--space-3, 12px);
}

.field-error-summary {
  margin: var(--space-3, 12px) 0 0;
  padding-left: var(--space-4, 16px);
  color: var(--el-color-danger);
  font-size: var(--font-size-sm, 12px);
}
</style>
