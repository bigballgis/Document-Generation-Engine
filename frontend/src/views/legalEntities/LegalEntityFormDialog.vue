<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { listDocumentBrands } from '@/api/documentBrands'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import ScopedGroupSelect from '@/components/common/ScopedGroupSelect.vue'
import type {
  CreateLegalEntityPayload,
  DocumentBrandStatus,
  DocumentBrandView,
  LegalEntityView,
  UpdateLegalEntityPayload,
} from '@/types/documentBrand'

const props = defineProps<{
  modelValue: boolean
  mode: 'create' | 'edit'
  initial?: LegalEntityView | null
  loading?: boolean
  defaultGroupCode?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  create: [payload: CreateLegalEntityPayload]
  update: [payload: UpdateLegalEntityPayload]
}>()

const { t } = useI18n()

const dialogOpen = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const form = reactive({
  groupCode: '',
  legalEntityCode: '',
  displayName: '',
  status: 'ACTIVE' as DocumentBrandStatus,
  documentBrandCode: '',
})

const brandOptions = ref<DocumentBrandView[]>([])
const loadingBrands = ref(false)
const statusOptions: DocumentBrandStatus[] = ['ACTIVE', 'INACTIVE']

async function loadBrandOptions(group: string) {
  if (!group) {
    brandOptions.value = []
    return
  }
  loadingBrands.value = true
  try {
    const page = await listDocumentBrands(group, { status: 'ACTIVE' })
    brandOptions.value = page.content
    if (
      form.documentBrandCode &&
      !brandOptions.value.some((b) => b.documentBrandCode === form.documentBrandCode)
    ) {
      // Keep current binding visible when editing an entity bound to INACTIVE brand.
      if (props.mode === 'edit' && props.initial?.documentBrandCode === form.documentBrandCode) {
        brandOptions.value = [
          {
            groupCode: group,
            documentBrandCode: form.documentBrandCode,
            displayName: form.documentBrandCode,
            status: 'INACTIVE',
            logoObjectRef: '',
          },
          ...brandOptions.value,
        ]
      }
    }
  } catch {
    brandOptions.value = []
  } finally {
    loadingBrands.value = false
  }
}

watch(
  () => props.modelValue,
  async (open) => {
    if (!open) {
      return
    }
    if (props.mode === 'edit' && props.initial) {
      form.groupCode = props.initial.groupCode
      form.legalEntityCode = props.initial.legalEntityCode
      form.displayName = props.initial.displayName
      form.status = props.initial.status
      form.documentBrandCode = props.initial.documentBrandCode
    } else {
      form.groupCode = props.defaultGroupCode ?? ''
      form.legalEntityCode = ''
      form.displayName = ''
      form.status = 'ACTIVE'
      form.documentBrandCode = ''
    }
    await loadBrandOptions(form.groupCode)
  },
)

watch(
  () => form.groupCode,
  async (group) => {
    if (!props.modelValue) {
      return
    }
    await loadBrandOptions(group)
    if (
      form.documentBrandCode &&
      !brandOptions.value.some((b) => b.documentBrandCode === form.documentBrandCode)
    ) {
      form.documentBrandCode = ''
    }
  },
)

const canSubmit = computed(() => {
  if (!form.groupCode.trim() || !form.displayName.trim() || !form.documentBrandCode.trim()) {
    return false
  }
  if (props.mode === 'create' && !form.legalEntityCode.trim()) {
    return false
  }
  return true
})

function closeDialog() {
  dialogOpen.value = false
}

function submitForm() {
  if (!canSubmit.value) {
    return
  }
  if (props.mode === 'create') {
    emit('create', {
      groupCode: form.groupCode.trim(),
      legalEntityCode: form.legalEntityCode.trim(),
      displayName: form.displayName.trim(),
      status: form.status,
      documentBrandCode: form.documentBrandCode.trim(),
    })
    return
  }
  emit('update', {
    groupCode: form.groupCode.trim(),
    displayName: form.displayName.trim(),
    status: form.status,
    documentBrandCode: form.documentBrandCode.trim(),
  })
}

</script>

<template>
  <el-dialog
    v-model="dialogOpen"
    :title="
      mode === 'create'
        ? t('legalEntities.form.createTitle')
        : t('legalEntities.form.editTitle')
    "
    width="560px"
    destroy-on-close
  >
    <el-form label-position="top" data-testid="legal-entity-form">
      <el-form-item :label="t('legalEntities.form.groupCode')" required>
        <ScopedGroupSelect
          v-model="form.groupCode"
          :disabled="mode === 'edit'"
          data-testid="legal-entity-group"
        />
      </el-form-item>
      <el-form-item :label="t('legalEntities.form.legalEntityCode')" required>
        <el-input
          v-model="form.legalEntityCode"
          maxlength="64"
          :disabled="mode === 'edit'"
          data-testid="legal-entity-code"
        />
      </el-form-item>
      <el-form-item :label="t('legalEntities.form.displayName')" required>
        <el-input
          v-model="form.displayName"
          maxlength="256"
          data-testid="legal-entity-display-name"
        />
      </el-form-item>
      <el-form-item :label="t('legalEntities.form.status')" required>
        <el-select v-model="form.status" data-testid="legal-entity-status">
          <el-option
            v-for="status in statusOptions"
            :key="status"
            :label="t(`legalEntities.status.${status}`)"
            :value="status"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('legalEntities.form.documentBrandCode')" required>
        <AppSearchSelect
          v-model="form.documentBrandCode"
          :loading="loadingBrands"
          :placeholder="t('legalEntities.form.documentBrandPlaceholder')"
          data-testid="legal-entity-document-brand"
        >
          <el-option
            v-for="brand in brandOptions"
            :key="brand.documentBrandCode"
            :label="`${brand.displayName} (${brand.documentBrandCode})`"
            :value="brand.documentBrandCode"
          />
        </AppSearchSelect>
        <p class="legal-entity-form__hint">{{ t('legalEntities.form.documentBrandHint') }}</p>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="closeDialog">{{ t('common.cancel') }}</el-button>
      <el-button
        type="primary"
        :loading="loading"
        :disabled="!canSubmit || loading"
        data-testid="legal-entity-submit"
        @click="submitForm"
      >
        {{ t('legalEntities.form.save') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.legal-entity-form__hint {
  margin: 0.35rem 0 0;
  font-size: 0.85rem;
  color: var(--text-muted);
  line-height: 1.4;
}
</style>
