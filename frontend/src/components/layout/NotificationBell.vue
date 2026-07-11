<script setup lang="ts">
import { Bell } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { canViewCollaborationWorkItems, sessionContext } from '@/auth/roles'
import { useNotificationPolling } from '@/composables/useNotificationPolling'
import { useCollaborationNotificationsStore } from '@/stores/collaborationNotifications'
import { useSessionStore } from '@/stores/session'
import type { CollaborationNotificationItem } from '@/types/collaboration'
import { formatCollaborationAgeSeconds } from '@/utils/collaborationWorkItems'
import {
  buildNotificationTaskHubLocation,
  formatUnreadBadgeLabel,
} from '@/utils/notificationCenter'

const { t, te } = useI18n()
const router = useRouter()
const sessionStore = useSessionStore()
const notificationsStore = useCollaborationNotificationsStore()

const dropdownOpen = ref(false)

const canView = computed(() =>
  canViewCollaborationWorkItems(sessionContext(sessionStore.session)),
)

useNotificationPolling({
  enabled: () => canView.value,
})

const badgeLabel = computed(() => formatUnreadBadgeLabel(notificationsStore.unreadCount))

const bellAriaLabel = computed(() => {
  if (notificationsStore.unreadCount > 0) {
    return t('collaboration.notifications.bellAriaLabelWithCount', {
      count: notificationsStore.unreadCount,
    })
  }
  return t('collaboration.notifications.bellAriaLabel')
})

const showMarkAll = computed(() => notificationsStore.unreadCount > 0)

watch(dropdownOpen, async (visible) => {
  if (!visible) {
    return
  }
  // Opening alone must not mark read (C7-C10 / BDD-LRP-C7-003).
  try {
    await notificationsStore.fetchList()
  } catch {
    // listErrorMessageKey already set on store
  }
})

function queueLabel(queue: CollaborationNotificationItem['queue']): string {
  const key = `collaboration.workItem.queue.${queue}.label`
  return te(key) ? t(key) : queue
}

function itemPrimaryText(item: CollaborationNotificationItem): string {
  return item.summaryText?.trim() || queueLabel(item.queue)
}

async function onItemClick(item: CollaborationNotificationItem) {
  try {
    await notificationsStore.markRead(item.workItemId)
  } catch {
    ElMessage.error(
      t(notificationsStore.actionErrorMessageKey ?? 'collaboration.notifications.error.markRead'),
    )
    return
  }
  dropdownOpen.value = false
  await router.push(buildNotificationTaskHubLocation(item.queue))
}

async function onMarkAll() {
  try {
    await notificationsStore.markAllRead()
    await notificationsStore.fetchList()
  } catch {
    ElMessage.error(
      t(notificationsStore.actionErrorMessageKey ?? 'collaboration.notifications.error.markAll'),
    )
  }
}

async function onRetryList() {
  try {
    await notificationsStore.fetchList()
  } catch {
    // keep error state
  }
}
</script>

<template>
  <div v-if="canView" class="notification-bell-root">
    <el-popover
      v-model:visible="dropdownOpen"
      placement="bottom-end"
      :width="360"
      trigger="click"
      popper-class="notification-dropdown-popper"
    >
      <template #reference>
        <button
          type="button"
          class="notification-bell"
          data-testid="notification-bell"
          :aria-label="bellAriaLabel"
          :aria-expanded="dropdownOpen"
          aria-haspopup="true"
        >
          <el-icon class="notification-bell__icon" :size="18">
            <Bell />
          </el-icon>
          <span
            v-if="badgeLabel"
            class="notification-bell__badge"
            data-testid="notification-badge"
            aria-hidden="true"
          >
            {{ badgeLabel }}
          </span>
        </button>
      </template>

      <div
        class="notification-dropdown"
        data-testid="notification-dropdown"
        role="region"
        :aria-label="t('collaboration.notifications.title')"
      >
        <div class="notification-dropdown__header">
          <h2 class="notification-dropdown__title">
            {{ t('collaboration.notifications.title') }}
          </h2>
          <button
            v-if="showMarkAll"
            type="button"
            class="notification-dropdown__mark-all"
            data-testid="notification-mark-all"
            :disabled="notificationsStore.marking"
            @click="onMarkAll"
          >
            {{ t('collaboration.notifications.markAll') }}
          </button>
        </div>

        <div
          v-if="notificationsStore.listErrorMessageKey"
          class="notification-dropdown__error"
          data-testid="notification-list-error"
        >
          <p>{{ t(notificationsStore.listErrorMessageKey) }}</p>
          <el-button size="small" type="primary" @click="onRetryList">
            {{ t('common.retry') }}
          </el-button>
        </div>

        <div
          v-else-if="notificationsStore.loadingList && notificationsStore.items.length === 0"
          class="notification-dropdown__loading"
        >
          {{ t('collaboration.notifications.loading') }}
        </div>

        <div
          v-else-if="notificationsStore.items.length === 0"
          class="notification-dropdown__empty"
          data-testid="notification-empty"
        >
          {{ t('collaboration.notifications.empty') }}
        </div>

        <ul v-else class="notification-dropdown__list">
          <li
            v-for="item in notificationsStore.items"
            :key="item.workItemId"
            class="notification-item"
            :class="{ 'notification-item--read': item.read }"
          >
            <button
              type="button"
              class="notification-item__button"
              data-testid="notification-item"
              :disabled="notificationsStore.marking"
              @click="onItemClick(item)"
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
    </el-popover>
  </div>
</template>

<style scoped lang="scss">
.notification-bell-root {
  display: inline-flex;
  align-items: center;
}

.notification-bell {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  padding: 0;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  transition:
    background-color var(--transition-base),
    color var(--transition-base),
    border-color var(--transition-base);

  &:hover {
    background: color-mix(in srgb, var(--brand-primary) 6%, var(--surface-card));
    color: var(--brand-primary);
  }

  &:focus-visible {
    outline: var(--focus-ring-width) solid var(--focus-ring-color);
    outline-offset: var(--focus-ring-offset);
  }

  &[aria-expanded='true'] {
    background: color-mix(in srgb, var(--brand-primary) 10%, var(--surface-card));
    color: var(--brand-primary);
  }
}

.notification-bell__icon {
  display: inline-flex;
}

.notification-bell__badge {
  position: absolute;
  top: 0.05rem;
  right: 0.05rem;
  min-width: 1.1rem;
  height: 1.1rem;
  padding: 0 0.25rem;
  border-radius: var(--radius-lg);
  background: var(--brand-primary);
  color: var(--on-primary);
  font-size: 0.625rem;
  font-weight: 700;
  line-height: 1.1rem;
  text-align: center;
}

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
