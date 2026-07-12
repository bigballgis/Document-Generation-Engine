<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { CollaborationNotificationItem } from '@/types/collaboration'
import { formatCollaborationAgeSeconds } from '@/utils/collaborationWorkItems'

defineProps<{
  title: string
  showMarkAll: boolean
  marking: boolean
  listErrorMessageKey: string | null
  loadingList: boolean
  items: CollaborationNotificationItem[]
  loadingLabel: string
  emptyLabel: string
  markAllLabel: string
  retryLabel: string
}>()

const emit = defineEmits<{
  'mark-all': []
  retry: []
  'item-click': [item: CollaborationNotificationItem]
}>()

const { t, te } = useI18n()

function queueLabel(queue: CollaborationNotificationItem['queue']): string {
  const key = `collaboration.workItem.queue.${queue}.label`
  return te(key) ? t(key) : queue
}

function itemPrimaryText(item: CollaborationNotificationItem): string {
  return item.summaryText?.trim() || queueLabel(item.queue)
}
</script>

<template>
  <div
    class="notification-dropdown"
    data-testid="notification-dropdown"
    role="region"
    :aria-label="title"
  >
    <div class="notification-dropdown__header">
      <h2 class="notification-dropdown__title">
        {{ title }}
      </h2>
      <button
        v-if="showMarkAll"
        type="button"
        class="notification-dropdown__mark-all"
        data-testid="notification-mark-all"
        :disabled="marking"
        @click="emit('mark-all')"
      >
        {{ markAllLabel }}
      </button>
    </div>

    <div
      v-if="listErrorMessageKey"
      class="notification-dropdown__error"
      data-testid="notification-list-error"
    >
      <p>{{ t(listErrorMessageKey) }}</p>
      <el-button size="small" type="primary" @click="emit('retry')">
        {{ retryLabel }}
      </el-button>
    </div>

    <div
      v-else-if="loadingList && items.length === 0"
      class="notification-dropdown__loading"
    >
      {{ loadingLabel }}
    </div>

    <div
      v-else-if="items.length === 0"
      class="notification-dropdown__empty"
      data-testid="notification-empty"
    >
      {{ emptyLabel }}
    </div>

    <ul v-else class="notification-dropdown__list">
      <li
        v-for="item in items"
        :key="item.workItemId"
        class="notification-item"
        :class="{ 'notification-item--read': item.read }"
      >
        <button
          type="button"
          class="notification-item__button"
          data-testid="notification-item"
          :disabled="marking"
          @click="emit('item-click', item)"
        >
          <span class="notification-item__primary">{{ itemPrimaryText(item) }}</span>
          <span class="notification-item__meta">
            <span class="notification-item__template">{{ item.templateName }}</span>
            <span class="notification-item__sep" aria-hidden="true">·</span>
            <span class="notification-item__queue">{{ queueLabel(item.queue) }}</span>
            <span class="notification-item__sep" aria-hidden="true">·</span>
            <span class="notification-item__age">{{
              formatCollaborationAgeSeconds(item.ageSeconds)
            }}</span>
          </span>
        </button>
      </li>
    </ul>
  </div>
</template>

<style scoped lang="scss">
.notification-dropdown__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  margin-bottom: var(--space-3);
  padding-bottom: var(--space-2);
  border-bottom: 1px solid var(--border-default);
}

.notification-dropdown__title {
  margin: 0;
  font-size: var(--font-size-sm);
  font-weight: 650;
  color: var(--text-primary);
}

.notification-dropdown__mark-all {
  border: none;
  background: transparent;
  padding: 0.2rem 0.35rem;
  border-radius: var(--radius-sm);
  font: inherit;
  font-size: var(--font-size-xs);
  font-weight: 600;
  color: var(--brand-primary);
  cursor: pointer;

  &:hover:not(:disabled) {
    background: color-mix(in srgb, var(--brand-primary) 8%, transparent);
  }

  &:disabled {
    opacity: 0.55;
    cursor: not-allowed;
  }

  &:focus-visible {
    outline: var(--focus-ring-width) solid var(--focus-ring-color);
    outline-offset: var(--focus-ring-offset);
  }
}

.notification-dropdown__error,
.notification-dropdown__loading,
.notification-dropdown__empty {
  padding: var(--space-4) var(--space-2);
  text-align: center;
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}

.notification-dropdown__error {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
  color: var(--text-primary);

  p {
    margin: 0;
  }
}

.notification-dropdown__list {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 22rem;
  overflow-y: auto;
}

.notification-item__button {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.25rem;
  width: 100%;
  padding: 0.65rem 0.5rem;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  text-align: left;
  font: inherit;
  cursor: pointer;
  transition: background-color var(--transition-base);

  &:hover:not(:disabled) {
    background: color-mix(in srgb, var(--brand-primary) 6%, var(--surface-card));
  }

  &:focus-visible {
    outline: var(--focus-ring-width) solid var(--focus-ring-color);
    outline-offset: var(--focus-ring-offset);
  }

  &:disabled {
    cursor: wait;
  }
}

.notification-item--read .notification-item__button {
  opacity: 0.72;
}

.notification-item__primary {
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.35;
}

.notification-item--read .notification-item__primary {
  font-weight: 500;
}

.notification-item__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.25rem;
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
}

.notification-item__sep {
  color: var(--text-tertiary);
}
</style>
