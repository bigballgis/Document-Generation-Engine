<script setup lang="ts">
import TemplateStatusBadge from '@/components/templates/TemplateStatusBadge.vue'
import type { TemplateLifecycleStatus } from '@/types/template'

withDefaults(
  defineProps<{
    templateName: string
    groupLabel?: string
    status?: TemplateLifecycleStatus
    approvalSubState?: 'PENDING_SUBMIT' | 'PENDING_DECISION' | null
    backLabel: string
  }>(),
  {
    groupLabel: undefined,
    status: undefined,
    approvalSubState: null,
  },
)

const emit = defineEmits<{
  back: []
}>()
</script>

<template>
  <header class="workspace-header">
    <div class="workspace-header__left">
      <el-button
        link
        type="primary"
        class="workspace-header__back"
        @click="emit('back')"
      >
        {{ backLabel }}
      </el-button>
      <span class="workspace-header__title" :title="templateName">{{ templateName }}</span>
      <el-tag v-if="groupLabel" size="small" class="workspace-header__group">{{ groupLabel }}</el-tag>
      <TemplateStatusBadge v-if="status" :status="status" :approval-sub-state="approvalSubState ?? undefined" />
    </div>
    <div v-if="$slots.actions" class="workspace-header__actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<style scoped lang="scss">
.workspace-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem 1rem;
  padding: 0.75rem 0 1rem;
  margin-bottom: 0.5rem;
  border-bottom: 1px solid var(--border-color);

  &__left {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 0.5rem;
    flex: 1 1 auto;
    min-width: 0;
  }

  &__back {
    flex-shrink: 0;
  }

  &__title {
    flex: 1 1 auto;
    min-width: 0;
    font-size: 1.125rem;
    font-weight: 600;
    color: var(--text-primary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 40ch;
  }

  &__group {
    flex-shrink: 0;
  }

  &__actions {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 0.5rem;
    flex-shrink: 0;
  }
}
</style>
