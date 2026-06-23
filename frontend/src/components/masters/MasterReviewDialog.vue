<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { MasterReviewDecision } from '@/types/master'

const props = defineProps<{
  modelValue: boolean
  mode: MasterReviewDecision
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submit: [payload: { decision: MasterReviewDecision; commentSummary: string }]
}>()

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const form = reactive({
  commentSummary: '',
})

watch(
  () => props.modelValue,
  (open) => {
    if (!open) {
      form.commentSummary = ''
    }
  },
)

const titleKey = computed(() =>
  props.mode === 'APPROVED' ? 'masters.review.approveTitle' : 'masters.review.rejectTitle',
)

function closeDialog() {
  visible.value = false
}

function submitForm() {
  emit('submit', {
    decision: props.mode,
    commentSummary: form.commentSummary.trim(),
  })
}
</script>

<template>
  <el-dialog v-model="visible" :title="t(titleKey)" width="520px" destroy-on-close>
    <el-form label-position="top">
      <el-form-item :label="t('masters.review.commentSummary')">
        <el-input
          v-model="form.commentSummary"
          type="textarea"
          maxlength="2048"
          :rows="4"
          :placeholder="t('masters.review.commentSummaryPlaceholder')"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="closeDialog">{{ t('masters.actions.cancel') }}</el-button>
      <el-button
        :type="mode === 'APPROVED' ? 'success' : 'danger'"
        @click="submitForm"
      >
        {{ t(mode === 'APPROVED' ? 'masters.review.approve' : 'masters.review.reject') }}
      </el-button>
    </template>
  </el-dialog>
</template>
