<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import { listLifecycleEvents } from '@/api/audit'
import { isGroupScopedAuditRole, resolveAuditActorRole } from '@/auth/roles'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { useSessionStore } from '@/stores/session'
import { resolveAuditActorDisplay } from '@/utils/auditEntityDisplay'
import type { AuditQueryFilters, LifecycleAuditEvent } from '@/types/audit'

const props = defineProps<{
  templateId: string
}>()

const { t } = useI18n()
const { formatDateTime } = useLocaleFormatters()
const sessionStore = useSessionStore()

const loading = ref(false)
const loadFailed = ref(false)
const events = ref<LifecycleAuditEvent[]>([])

onMounted(() => {
  void loadEvents()
})

function buildFilters(): AuditQueryFilters | null {
  const actorRole = resolveAuditActorRole(sessionStore.session?.roles ?? [])
  if (!actorRole) {
    return null
  }

  const filters: AuditQueryFilters = {
    actorRole,
    templateId: props.templateId,
    page: 0,
    size: 50,
  }

  if (isGroupScopedAuditRole(actorRole)) {
    const groupScope = sessionStore.session?.authorizedGroupCodes?.find((code) => code !== '*')
    if (groupScope) {
      filters.groupScope = groupScope
    }
  }

  return filters
}

async function loadEvents() {
  loading.value = true
  loadFailed.value = false
  try {
    const filters = buildFilters()
    if (!filters) {
      loadFailed.value = true
      events.value = []
      return
    }
    const page = await listLifecycleEvents(filters)
    events.value = page.events
  } catch {
    loadFailed.value = true
    events.value = []
  } finally {
    loading.value = false
  }
}

function eventLabel(event: LifecycleAuditEvent): string {
  const key = `audit.lifecycle.eventType.${event.eventType}`
  return t(key, event.eventType)
}

function eventKey(event: LifecycleAuditEvent, index: number): string {
  return `${event.eventAt}-${event.eventType}-${index}`
}
</script>

<template>
  <el-card shadow="never" class="timeline-card">
    <h3>{{ t('templates.releaseDetail.approval.timelineTitle') }}</h3>
    <p class="read-only-hint">{{ t('templates.releaseDetail.approval.timelineHint') }}</p>

    <LoadErrorPanel
      v-if="loadFailed"
      message-key="templates.releaseDetail.approval.timelineLoadError"
      @retry="loadEvents"
    />

    <el-skeleton v-else-if="loading" :rows="4" animated />

    <EmptyStatePanel
      v-else-if="events.length === 0"
      title-key="templates.releaseDetail.approval.timelineEmptyTitle"
      description-key="templates.releaseDetail.approval.timelineEmptyDescription"
    />

    <el-timeline v-else>
      <el-timeline-item
        v-for="(event, index) in events"
        :key="eventKey(event, index)"
        :timestamp="formatDateTime(event.eventAt)"
        placement="top"
      >
        <p class="event-type">{{ eventLabel(event) }}</p>
        <p class="event-actor">{{ resolveAuditActorDisplay(event) }}</p>
        <p v-if="event.summary" class="event-comment">{{ event.summary }}</p>
      </el-timeline-item>
    </el-timeline>
  </el-card>
</template>

<style scoped lang="scss">
.timeline-card {
  h3 {
    margin: 0 0 0.5rem;
    font-size: 1.0625rem;
  }
}

.read-only-hint {
  margin: 0 0 1rem;
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

.event-type {
  margin: 0;
  font-weight: 600;
}

.event-actor {
  margin: 0.25rem 0 0;
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}

.event-comment {
  margin: 0.35rem 0 0;
  color: var(--text-primary);
  font-size: var(--font-size-sm);
}
</style>
