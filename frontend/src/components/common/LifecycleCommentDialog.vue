<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  modelValue: boolean
  title: string
  message?: string
  confirmLabel: string
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

function close() {
  emit('update:modelValue', false)
}

function handleConfirm() {
  emit('confirm', comment.value.trim())
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    width="480px"
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
    @close="close"
  >
    <p v-if="message" class="lifecycle-comment-dialog__message">{{ message }}</p>
    <el-input
      v-model="comment"
      type="textarea"
      :rows="3"
      :placeholder="t('templates.lifecycle.commentPlaceholder')"
    />
    <template #footer>
      <el-button @click="close">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">
        {{ confirmLabel }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.lifecycle-comment-dialog__message {
  margin: 0 0 0.75rem;
  font-size: 0.875rem;
  color: var(--text-muted);
  line-height: 1.45;
}
</style>
