<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'

const props = defineProps<{
  draftUpdatedAt: string
  serverUpdatedAt?: string | null
}>()

const emit = defineEmits<{
  restore: []
  discard: []
}>()

const { t } = useI18n()
const { formatDateTime } = useLocaleFormatters()

const draftTimestampLabel = computed(() => formatDateTime(props.draftUpdatedAt))
const serverTimestampLabel = computed(() =>
  props.serverUpdatedAt ? formatDateTime(props.serverUpdatedAt) : null,
)
</script>

<template>
  <el-alert
    class="draft-recovery-banner"
    type="warning"
    :closable="false"
    show-icon
    data-testid="structured-draft-recovery-banner"
  >
    <template #title>
      <span class="draft-recovery-banner__title">
        {{ t('templates.structuredEditor.draftRecovery.title') }}
      </span>
    </template>
    <div class="draft-recovery-banner__body">
      <p class="draft-recovery-banner__message">
        {{
          t('templates.structuredEditor.draftRecovery.message', {
            draftTimestamp: draftTimestampLabel,
          })
        }}
      </p>
      <p class="draft-recovery-banner__meta">
        <span>
          {{
            t('templates.structuredEditor.draftRecovery.draftTimestamp', {
              timestamp: draftTimestampLabel,
            })
          }}
        </span>
        <span v-if="serverTimestampLabel">
          {{
            t('templates.structuredEditor.draftRecovery.serverTimestamp', {
              timestamp: serverTimestampLabel,
            })
          }}
        </span>
      </p>
      <div class="draft-recovery-banner__actions">
        <el-button
          type="primary"
          size="small"
          data-testid="structured-draft-recovery-banner-restore"
          @click="emit('restore')"
        >
          {{ t('templates.structuredEditor.draftRecovery.restore') }}
        </el-button>
        <el-button
          size="small"
          data-testid="structured-draft-recovery-banner-discard"
          @click="emit('discard')"
        >
          {{ t('templates.structuredEditor.draftRecovery.discard') }}
        </el-button>
      </div>
    </div>
  </el-alert>
</template>

<style scoped lang="scss">
.draft-recovery-banner {
  margin-bottom: 0.75rem;

  :deep(.el-alert__content) {
    width: 100%;
  }
}

.draft-recovery-banner__title {
  font-weight: 600;
}

.draft-recovery-banner__body {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.draft-recovery-banner__message,
.draft-recovery-banner__meta {
  margin: 0;
  color: var(--text-muted);
  font-size: 0.875rem;
}

.draft-recovery-banner__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}

.draft-recovery-banner__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}
</style>
