<script setup lang="ts">
import { Bell } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { canViewCollaborationWorkItems, sessionContext } from '@/auth/roles'
import NotificationDropdownPanel from '@/components/layout/NotificationDropdownPanel.vue'
import { useNotificationPolling } from '@/composables/useNotificationPolling'
import { useCollaborationNotificationsStore } from '@/stores/collaborationNotifications'
import { useSessionStore } from '@/stores/session'
import type { CollaborationNotificationItem } from '@/types/collaboration'
import {
  buildNotificationTaskHubLocation,
  formatUnreadBadgeLabel,
} from '@/utils/notificationCenter'

const { t } = useI18n()
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

      <NotificationDropdownPanel
        :title="t('collaboration.notifications.title')"
        :show-mark-all="showMarkAll"
        :marking="notificationsStore.marking"
        :list-error-message-key="notificationsStore.listErrorMessageKey"
        :loading-list="notificationsStore.loadingList"
        :items="notificationsStore.items"
        :loading-label="t('collaboration.notifications.loading')"
        :empty-label="t('collaboration.notifications.empty')"
        :mark-all-label="t('collaboration.notifications.markAll')"
        :retry-label="t('common.retry')"
        @mark-all="onMarkAll"
        @retry="onRetryList"
        @item-click="onItemClick"
      />
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
</style>
