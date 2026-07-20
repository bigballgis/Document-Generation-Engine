<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import ScopedGroupSelect from '@/components/common/ScopedGroupSelect.vue'
import type {
  CreateDocumentBrandPayload,
  DocumentBrandStatus,
  DocumentBrandView,
  UpdateDocumentBrandPayload,
} from '@/types/documentBrand'

const props = defineProps<{
  modelValue: boolean
  mode: 'create' | 'edit'
  initial?: DocumentBrandView | null
  loading?: boolean
  defaultGroupCode?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  create: [payload: CreateDocumentBrandPayload]
  update: [payload: UpdateDocumentBrandPayload]
}>()

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const form = reactive({
  groupCode: '',
  documentBrandCode: '',
  displayName: '',
  status: 'ACTIVE' as DocumentBrandStatus,
  logoObjectRef: '',
  defaultSealObjectRef: '',
  letterheadLegalName: '',
})

const statusOptions: DocumentBrandStatus[] = ['ACTIVE', 'INACTIVE']

watch(
  () => props.modelValue,
  (open) => {
    if (!open) {
      return
    }
    if (props.mode === 'edit' && props.initial) {
      form.groupCode = props.initial.groupCode
      form.documentBrandCode = props.initial.documentBrandCode
      form.displayName = props.initial.displayName
      form.status = props.initial.status
      form.logoObjectRef = props.initial.logoObjectRef
      form.defaultSealObjectRef = props.initial.defaultSealObjectRef ?? ''
      form.letterheadLegalName = props.initial.letterheadLegalName ?? ''
    } else {
      form.groupCode = props.defaultGroupCode ?? ''
      form.documentBrandCode = ''
      form.displayName = ''
      form.status = 'ACTIVE'
      form.logoObjectRef = ''
      form.defaultSealObjectRef = ''
      form.letterheadLegalName = ''
    }
  },
)

const canSubmit = computed(() => {
  if (!form.groupCode.trim() || !form.displayName.trim() || !form.logoObjectRef.trim()) {
    return false
  }
  if (props.mode === 'create' && !form.documentBrandCode.trim()) {
    return false
  }
  return true
})

function closeDialog() {
  visible.value = false
}

function submitForm() {
  if (!canSubmit.value) {
    return
  }
  if (props.mode === 'create') {
    emit('create', {
      groupCode: form.groupCode.trim(),
      documentBrandCode: form.documentBrandCode.trim(),
      displayName: form.displayName.trim(),
      status: form.status,
      logoObjectRef: form.logoObjectRef.trim(),
      defaultSealObjectRef: form.defaultSealObjectRef.trim() || null,
      letterheadLegalName: form.letterheadLegalName.trim() || null,
    })
    return
  }
  emit('update', {
    groupCode: form.groupCode.trim(),
    displayName: form.displayName.trim(),
    status: form.status,
    logoObjectRef: form.logoObjectRef.trim(),
    defaultSealObjectRef: form.defaultSealObjectRef.trim() || null,
    letterheadLegalName: form.letterheadLegalName.trim() || null,
  })
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="
      mode === 'create'
        ? t('documentBrands.form.createTitle')
        : t('documentBrands.form.editTitle')
    "
    width="560px"
    destroy-on-close
  >
    <el-form label-position="top" data-testid="document-brand-form">
      <el-form-item :label="t('documentBrands.form.groupCode')" required>
        <ScopedGroupSelect
          v-model="form.groupCode"
          :disabled="mode === 'edit'"
          data-testid="document-brand-group"
        />
      </el-form-item>
      <el-form-item :label="t('documentBrands.form.documentBrandCode')" required>
        <el-input
          v-model="form.documentBrandCode"
          maxlength="64"
          :disabled="mode === 'edit'"
          data-testid="document-brand-code"
        />
      </el-form-item>
      <el-form-item :label="t('documentBrands.form.displayName')" required>
        <el-input
          v-model="form.displayName"
          maxlength="256"
          data-testid="document-brand-display-name"
        />
      </el-form-item>
      <el-form-item :label="t('documentBrands.form.status')" required>
        <el-select v-model="form.status" data-testid="document-brand-status">
          <el-option
            v-for="status in statusOptions"
            :key="status"
            :label="t(`documentBrands.status.${status}`)"
            :value="status"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('documentBrands.form.logoObjectRef')" required>
        <el-input
          v-model="form.logoObjectRef"
          maxlength="256"
          data-testid="document-brand-logo-ref"
        />
      </el-form-item>
      <el-form-item :label="t('documentBrands.form.defaultSealObjectRef')">
        <el-input
          v-model="form.defaultSealObjectRef"
          maxlength="256"
          data-testid="document-brand-seal-ref"
        />
      </el-form-item>
      <el-form-item :label="t('documentBrands.form.letterheadLegalName')">
        <el-input
          v-model="form.letterheadLegalName"
          maxlength="256"
          data-testid="document-brand-letterhead-name"
        />
      </el-form-item>
      <p class="document-brand-form__hint">{{ t('documentBrands.form.uiChromeHint') }}</p>
    </el-form>
    <template #footer>
      <el-button @click="closeDialog">{{ t('common.cancel') }}</el-button>
      <el-button
        type="primary"
        :loading="loading"
        :disabled="!canSubmit || loading"
        data-testid="document-brand-submit"
        @click="submitForm"
      >
        {{ t('documentBrands.form.save') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.document-brand-form__hint {
  margin: 0;
  font-size: 0.85rem;
  color: var(--text-muted);
  line-height: 1.4;
}
</style>
