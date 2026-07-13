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

<style scoped lang="scss" src="./NotificationDropdownPanel.scss"></style>
