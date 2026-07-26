<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { type FormInstance, type FormRules } from 'element-plus'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import { useAuditTemplateFilterOptions } from '@/composables/useAuditTemplateFilterOptions'
import { useLegalHoldsStore } from '@/stores/legalHolds'
import type { CreateLegalHoldPayload, LegalHoldScopeType } from '@/types/legalHold'
import { parseLegalHoldInvocationIds } from '@/utils/legalHoldInvocationIds'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  created: []
}>()

const { t, te } = useI18n()
const legalHoldsStore = useLegalHoldsStore()
const { templateOptions, loadingTemplates, searchTemplates } = useAuditTemplateFilterOptions()

const formRef = ref<FormInstance>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const form = reactive({
  scopeType: 'TEMPLATE_WINDOW' as LegalHoldScopeType,
  reason: '',
  templateId: '',
  effectiveFrom: '',
  effectiveTo: '',
  invocationExternalIdsText: '',
})

const formRules = computed<FormRules>(() => {
  const rules: FormRules = {
    scopeType: [
      {
        required: true,
        message: t('legalHold.create.validation.scopeTypeRequired'),
        trigger: 'change',
      },
    ],
  }
  if (form.scopeType === 'TEMPLATE_WINDOW') {
    rules.templateId = [
      {
        required: true,
        message: t('legalHold.create.validation.templateRequired'),
        trigger: 'change',
      },
    ]
    rules.effectiveFrom = [
      {
        required: true,
        message: t('legalHold.create.validation.effectiveFromRequired'),
        trigger: 'change',
      },
    ]
  } else {
    rules.invocationExternalIdsText = [
      {
        required: true,
        message: t('legalHold.create.validation.invocationIdsRequired'),
        trigger: 'blur',
      },
    ]
  }
  return rules
})

const apiErrorMessage = computed(() => {
  const key = legalHoldsStore.lastMutationErrorMessageKey
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('legalHold.error.create')
})

function resetForm() {
  form.scopeType = 'TEMPLATE_WINDOW'
  form.reason = ''
  form.templateId = ''
  form.effectiveFrom = ''
  form.effectiveTo = ''
  form.invocationExternalIdsText = ''
  legalHoldsStore.lastMutationErrorMessageKey = null
  formRef.value?.clearValidate()
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      resetForm()
      void searchTemplates('')
    }
  },
)

async function performCreate() {
  const payload: CreateLegalHoldPayload = {
    scopeType: form.scopeType,
    reason: form.reason.trim() || null,
  }

  if (form.scopeType === 'TEMPLATE_WINDOW') {
    payload.templateId = form.templateId
    payload.effectiveFrom = form.effectiveFrom
    payload.effectiveTo = form.effectiveTo || null
  } else {
    payload.invocationExternalIds = parseLegalHoldInvocationIds(form.invocationExternalIdsText)
  }

  await legalHoldsStore.createHold(payload)
  emit('created')
  visible.value = false
}

async function submit() {
  if (formRef.value) {
    const valid = await formRef.value.validate().catch(() => false)
    if (!valid) {
      return
    }
  }

  try {
    await performCreate()
  } catch {
    // Error key is already stored for display.
  }
}

function handleClose() {
  visible.value = false
}

defineExpose({
  form,
  submit,
  performCreate,
})
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="t('legalHold.create.title')"
    width="560px"
    destroy-on-close
    :close-on-click-modal="false"
    data-testid="legal-hold-create-dialog"
    @closed="resetForm"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="formRules"
      label-position="top"
      data-testid="legal-hold-create-form"
    >
      <el-form-item :label="t('legalHold.create.fields.scopeType')" prop="scopeType">
        <el-select
          v-model="form.scopeType"
          data-testid="legal-hold-scope-type"
          :aria-label="t('legalHold.create.fields.scopeType')"
        >
          <el-option
            :label="t('legalHold.scope.TEMPLATE_WINDOW')"
            value="TEMPLATE_WINDOW"
          />
          <el-option
            :label="t('legalHold.scope.INVOCATION_SET')"
            value="INVOCATION_SET"
          />
        </el-select>
      </el-form-item>

      <el-form-item :label="t('legalHold.create.fields.reason')" prop="reason">
        <el-input
          v-model="form.reason"
          maxlength="512"
          show-word-limit
          type="textarea"
          :rows="2"
          data-testid="legal-hold-reason"
          :placeholder="t('legalHold.create.placeholders.reason')"
        />
      </el-form-item>

      <template v-if="form.scopeType === 'TEMPLATE_WINDOW'">
        <el-form-item :label="t('legalHold.create.fields.template')" prop="templateId">
          <AppSearchSelect
            v-model="form.templateId"
            data-testid="legal-hold-template"
            filterable
            remote
            clearable
            :remote-method="searchTemplates"
            :loading="loadingTemplates"
            :placeholder="t('legalHold.create.placeholders.template')"
          >
            <el-option
              v-for="option in templateOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </AppSearchSelect>
        </el-form-item>
        <el-form-item :label="t('legalHold.create.fields.effectiveFrom')" prop="effectiveFrom">
          <el-date-picker
            v-model="form.effectiveFrom"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss[Z]"
            data-testid="legal-hold-effective-from"
            :placeholder="t('legalHold.create.placeholders.effectiveFrom')"
          />
        </el-form-item>
        <el-form-item :label="t('legalHold.create.fields.effectiveTo')" prop="effectiveTo">
          <el-date-picker
            v-model="form.effectiveTo"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss[Z]"
            clearable
            data-testid="legal-hold-effective-to"
            :placeholder="t('legalHold.create.placeholders.effectiveTo')"
          />
        </el-form-item>
      </template>

      <el-form-item
        v-else
        :label="t('legalHold.create.fields.invocationIds')"
        prop="invocationExternalIdsText"
      >
        <el-input
          v-model="form.invocationExternalIdsText"
          type="textarea"
          :rows="5"
          data-testid="legal-hold-invocation-ids"
          :placeholder="t('legalHold.create.placeholders.invocationIds')"
        />
      </el-form-item>

      <el-alert
        v-if="apiErrorMessage"
        class="legal-hold-create-error"
        type="error"
        :title="apiErrorMessage"
        show-icon
        :closable="false"
        data-testid="legal-hold-create-error"
      />
    </el-form>

    <template #footer>
      <el-button data-testid="legal-hold-create-cancel" @click="handleClose">
        {{ t('common.cancel') }}
      </el-button>
      <el-button
        type="primary"
        :loading="legalHoldsStore.submitting"
        data-testid="legal-hold-create-submit"
        @click="submit"
      >
        {{ t('legalHold.create.submit') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.legal-hold-create-error {
  margin-top: var(--space-3);
}

:deep(.el-select),
:deep(.el-date-editor) {
  width: 100%;
}
</style>
