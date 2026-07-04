<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  messageKey: string
  retryable?: boolean
}>()

const emit = defineEmits<{
  retry: []
}>()

const { t, te } = useI18n()

const title = computed(() => (te(props.messageKey) ? t(props.messageKey) : t('common.loadError')))
</script>

<template>
  <el-result icon="error" :title="title">
    <template #extra>
      <p v-if="retryable" class="retryable-hint">
        {{ t('common.retryableHint') }}
      </p>
      <el-button type="primary" @click="emit('retry')">
        {{ t('common.retry') }}
      </el-button>
    </template>
  </el-result>
</template>

<style scoped lang="scss">
.retryable-hint {
  margin: 0 0 0.75rem;
  color: var(--text-muted);
}
</style>
