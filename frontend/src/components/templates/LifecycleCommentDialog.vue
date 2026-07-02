<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  modelValue: boolean
  loading?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: [comment: string]
}>()

const { t } = useI18n()

const comment = ref('')

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      comment.value = ''
    }
  },
)

function handleConfirm() {
  emit('confirm', comment.value.trim())
  emit('update:modelValue', false)
}

function handleCancel() {
  emit('update:modelValue', false)
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="t('templates.lifecycle.commentDialogTitle')"
    width="480px"
    :close-on-click-modal="false"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-input
      v-model="comment"
      type="textarea"
      :rows="3"
      :placeholder="t('templates.lifecycle.commentPlaceholder')"
    />
    <template #footer>
      <el-button @click="handleCancel">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">
        {{ t('common.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>
