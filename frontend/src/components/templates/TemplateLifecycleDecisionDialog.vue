<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  TEMPLATE_DECISION_REASON_CATEGORIES,
  isLifecycleDecisionFormValid,
} from '@/utils/templateLifecycleDecisionForm'

export type LifecycleDecisionDialogMode = 'test-fail' | 'approval-reject'

const props = defineProps<{
  modelValue: boolean
  mode: LifecycleDecisionDialogMode
  loading?: boolean
  initialComment?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submit: [payload: { reasonCategory: string; impactSummary: string; commentSummary?: string }]
}>()

const { t } = useI18n()
const formRef = ref<FormInstance>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const form = reactive({
  reasonCategory: '',
  impactSummary: '',
  commentSummary: '',
})

const dialogTitle = computed(() =>
  props.mode === 'test-fail'
    ? t('templates.lifecycle.decisionForm.failTestTitle')
    : t('templates.lifecycle.decisionForm.rejectTitle'),
)

const rules = computed<FormRules>(() => ({
  reasonCategory: [
    {
      required: true,
      message: t('templates.lifecycle.decisionForm.validation.reasonCategoryRequired'),
      trigger: 'change',
    },
  ],
  impactSummary: [
    {
      required: true,
      message: t('templates.lifecycle.decisionForm.validation.impactSummaryRequired'),
      trigger: 'blur',
    },
  ],
}))

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      form.reasonCategory = ''
      form.impactSummary = ''
      form.commentSummary = props.initialComment ?? ''
      formRef.value?.clearValidate()
    }
  },
)

function closeDialog() {
  visible.value = false
}

async function submitForm() {
  if (!formRef.value) {
    return
  }
  await formRef.value.validate((valid) => {
    if (!valid || !isLifecycleDecisionFormValid(form)) {
      return
    }
    emit('submit', {
      reasonCategory: form.reasonCategory.trim(),
      impactSummary: form.impactSummary.trim(),
      commentSummary: form.commentSummary.trim() || undefined,
    })
  })
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="560px"
    destroy-on-close
    :close-on-click-modal="false"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item
        :label="t('templates.lifecycle.decisionForm.reasonCategory')"
        prop="reasonCategory"
        required
      >
        <el-select
          v-model="form.reasonCategory"
          filterable
          :placeholder="t('templates.lifecycle.decisionForm.reasonCategoryPlaceholder')"
          style="width: 100%"
        >
          <el-option
            v-for="category in TEMPLATE_DECISION_REASON_CATEGORIES"
            :key="category"
            :label="t(`templates.lifecycle.decisionForm.reasonCategories.${category}`)"
            :value="category"
          />
        </el-select>
      </el-form-item>
      <el-form-item
        :label="t('templates.lifecycle.decisionForm.impactSummary')"
        prop="impactSummary"
        required
      >
        <el-input
          v-model="form.impactSummary"
          type="textarea"
          :rows="4"
          maxlength="2048"
          show-word-limit
          :placeholder="t('templates.lifecycle.decisionForm.impactSummaryPlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="t('templates.lifecycle.decisionForm.optionalComment')">
        <el-input
          v-model="form.commentSummary"
          type="textarea"
          :rows="2"
          maxlength="2048"
          :placeholder="t('templates.lifecycle.commentPlaceholder')"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="closeDialog">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="loading" @click="submitForm">
        {{ t('templates.lifecycle.decisionForm.submit') }}
      </el-button>
    </template>
  </el-dialog>
</template>
