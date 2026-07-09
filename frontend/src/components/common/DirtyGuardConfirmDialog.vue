<script setup lang="ts">
import { useI18n } from 'vue-i18n'

defineProps<{
  modelValue: boolean
  showSave?: boolean
  saving?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  stay: []
  discard: []
  save: []
}>()

const { t } = useI18n()

function handleStay() {
  emit('stay')
  emit('update:modelValue', false)
}

function handleDiscard() {
  emit('discard')
}

function handleSave() {
  emit('save')
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="t('common.dirtyGuard.title')"
    :width="480"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    append-to-body
    class="dirty-guard-dialog"
    data-testid="dirty-guard-dialog"
    @update:model-value="emit('update:modelValue', $event)"
    @close="handleStay"
  >
    <p class="dirty-guard-dialog__message">{{ t('common.dirtyGuard.message') }}</p>

    <template #footer>
      <div class="dirty-guard-dialog__actions">
        <el-button data-testid="dirty-guard-stay" @click="handleStay">
          {{ t('common.dirtyGuard.stay') }}
        </el-button>
        <el-button data-testid="dirty-guard-discard" type="warning" @click="handleDiscard">
          {{ t('common.dirtyGuard.discard') }}
        </el-button>
        <el-button
          v-if="showSave"
          data-testid="dirty-guard-save"
          type="primary"
          :loading="saving"
          @click="handleSave"
        >
          {{ t('common.dirtyGuard.save') }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.dirty-guard-dialog {
  &__message {
    margin: 0;
    color: var(--text-secondary);
    line-height: 1.5;
  }

  &__actions {
    display: flex;
    flex-wrap: wrap;
    justify-content: flex-end;
    gap: var(--space-2);
  }
}
</style>
