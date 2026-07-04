<script setup lang="ts">
import { useI18n } from 'vue-i18n'

// Non-blocking banner shown when the session nears its absolute limit
// (LR-B6 SCEN-UX-02). It never signs the user out by itself; the action
// button hands control to the sign-in-again flow owned by the shell.
const { t } = useI18n()

const emit = defineEmits<{ action: [] }>()
</script>

<template>
  <el-alert
    class="session-limit-reminder"
    type="warning"
    show-icon
    :closable="false"
  >
    <template #title>{{ t('session.absoluteLimitReminder.title') }}</template>
    <div class="session-limit-reminder__body">
      <p class="session-limit-reminder__message">
        {{ t('session.absoluteLimitReminder.message') }}
      </p>
      <el-button
        type="primary"
        plain
        size="small"
        class="session-limit-reminder__action"
        @click="emit('action')"
      >
        {{ t('session.absoluteLimitReminder.action') }}
      </el-button>
    </div>
  </el-alert>
</template>

<style scoped lang="scss">
.session-limit-reminder {
  border-radius: 0;
  border-bottom: 1px solid var(--border-default);
  padding: var(--space-2) var(--space-6);
  background: var(--status-warning-bg);

  :deep(.el-alert__title) {
    color: var(--status-warning-text);
    font-weight: 650;
    font-size: var(--font-size-base);
  }

  :deep(.el-alert__icon) {
    color: var(--status-warning-text);
  }

  :deep(.el-alert__description) {
    margin: var(--space-1) 0 0;
  }
}

.session-limit-reminder__body {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-2) var(--space-4);
}

.session-limit-reminder__message {
  margin: 0;
  color: var(--status-warning-text);
  font-size: var(--font-size-sm);
  line-height: 1.5;
}

.session-limit-reminder__action {
  flex-shrink: 0;
}

// Warning-semantic action states. The reminder is a warning surface, so the
// action stays brand-neutral (identical under REDBC and GREENBC) instead of
// inheriting Element Plus factory blue or the global plain-primary brand
// hover. All values are design tokens and keep WCAG AA contrast on the warm
// banner: rest #b45309 on #fffbeb = 4.84:1, hover/active #92400e on #ffffff
// = 7.09:1. The extra .el-button class outranks the library and global
// plain-primary variable rules without !important.
.el-button.session-limit-reminder__action {
  --el-button-text-color: var(--status-warning-text);
  --el-button-border-color: var(--status-warning-text);
  --el-button-bg-color: transparent;
  --el-button-hover-text-color: var(--status-warning-text-strong);
  --el-button-hover-border-color: var(--status-warning-text-strong);
  --el-button-hover-bg-color: var(--surface-card);
  --el-button-active-text-color: var(--status-warning-text-strong);
  --el-button-active-border-color: var(--status-warning-text-strong);
  --el-button-active-bg-color: var(--surface-card);
}
</style>
