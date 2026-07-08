<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { getInvocationDetail } from '@/api/apiPolicy'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { ROUTE_KEYS, ROUTE_PATH_BY_KEY } from '@/routing/routeKeys'
import { useSessionStore } from '@/stores/session'
import type { ManagementInvocationDetail } from '@/types/template'

const visible = defineModel<boolean>('visible', { required: true })

const props = defineProps<{
  templateId: string
  invocationId: string | null
}>()

const { t } = useI18n()
const { formatDateTime } = useLocaleFormatters()
const sessionStore = useSessionStore()

const loading = ref(false)
const loadFailed = ref(false)
const detail = ref<ManagementInvocationDetail | null>(null)

const canLinkAudit = computed(() => sessionStore.canAccessRoute(ROUTE_KEYS.auditConsole))

const auditLinkTarget = computed(() => {
  const requestId = detail.value?.auditLinkHint?.requestId ?? detail.value?.requestId
  if (!requestId?.trim()) {
    return null
  }
  return {
    path: ROUTE_PATH_BY_KEY[ROUTE_KEYS.auditConsole],
    query: { requestId: requestId.trim() },
  }
})

function formatRouteSummary(value: ManagementInvocationDetail | null): string {
  if (!value) {
    return '—'
  }
  const parts = [value.routeType, value.resolvedReleaseVersion].filter(
    (part) => typeof part === 'string' && part.trim().length > 0,
  )
  return parts.length > 0 ? parts.join(' · ') : '—'
}

function formatOptionalText(value: string | null | undefined): string {
  return value?.trim() ? value : '—'
}

function formatDuration(value: number | null | undefined): string {
  if (value == null) {
    return '—'
  }
  return String(value)
}

function formatDocumentPresent(value: boolean | undefined): string {
  return value ? t('common.yes') : t('common.no')
}

async function loadDetail() {
  if (!props.invocationId) {
    detail.value = null
    return
  }
  loading.value = true
  loadFailed.value = false
  try {
    detail.value = await getInvocationDetail(props.templateId, props.invocationId)
  } catch {
    detail.value = null
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

watch(
  () => [visible.value, props.invocationId, props.templateId] as const,
  ([isVisible, invocationId]) => {
    if (isVisible && invocationId) {
      void loadDetail()
      return
    }
    detail.value = null
    loadFailed.value = false
  },
  { immediate: true },
)
</script>

<template>
  <el-drawer
    v-model="visible"
    :title="t('templates.policy.invocations.drawer.title')"
    size="480px"
    destroy-on-close
    data-testid="invocation-summary-drawer"
  >
    <el-skeleton v-if="loading" :rows="8" animated />
    <LoadErrorPanel
      v-else-if="loadFailed"
      message-key="templates.policy.invocations.drawer.loadFailed"
      @retry="loadDetail"
    />
    <template v-else-if="detail">
      <dl class="summary-list">
        <div class="summary-row">
          <dt>{{ t('templates.policy.invocations.drawer.fields.invocationId') }}</dt>
          <dd>{{ detail.invocationId }}</dd>
        </div>
        <div class="summary-row">
          <dt>{{ t('templates.policy.invocations.drawer.fields.requestId') }}</dt>
          <dd>{{ detail.requestId }}</dd>
        </div>
        <div class="summary-row">
          <dt>{{ t('templates.policy.invocations.drawer.fields.routeSummary') }}</dt>
          <dd>{{ formatRouteSummary(detail) }}</dd>
        </div>
        <div class="summary-row">
          <dt>{{ t('templates.policy.invocations.drawer.fields.outcome') }}</dt>
          <dd>{{ formatOptionalText(detail.outcome) }}</dd>
        </div>
        <div class="summary-row">
          <dt>{{ t('templates.policy.invocations.drawer.fields.durationMs') }}</dt>
          <dd>{{ formatDuration(detail.durationMs) }}</dd>
        </div>
        <div class="summary-row">
          <dt>{{ t('templates.policy.invocations.drawer.fields.accessAccount') }}</dt>
          <dd>{{ detail.accessAccountSummary }}</dd>
        </div>
        <div v-if="detail.batchId" class="summary-row">
          <dt>{{ t('templates.policy.invocations.drawer.fields.batchId') }}</dt>
          <dd>{{ detail.batchId }}</dd>
        </div>
        <div v-if="detail.parentInvocationId" class="summary-row">
          <dt>{{ t('templates.policy.invocations.drawer.fields.parentInvocationId') }}</dt>
          <dd>{{ detail.parentInvocationId }}</dd>
        </div>
        <div class="summary-row">
          <dt>{{ t('templates.policy.invocations.drawer.fields.createdAt') }}</dt>
          <dd>{{ formatDateTime(detail.createdAt) }}</dd>
        </div>
        <div class="summary-row">
          <dt>{{ t('templates.policy.invocations.drawer.fields.documentPresent') }}</dt>
          <dd>{{ formatDocumentPresent(detail.documentPresent) }}</dd>
        </div>
      </dl>

      <div v-if="canLinkAudit && auditLinkTarget" class="drawer-actions">
        <RouterLink :to="auditLinkTarget" class="audit-link">
          {{ t('templates.policy.invocations.drawer.auditLink') }}
        </RouterLink>
      </div>
    </template>
  </el-drawer>
</template>

<style scoped lang="scss">
.summary-list {
  margin: 0;
}

.summary-row {
  display: grid;
  grid-template-columns: minmax(8rem, 35%) 1fr;
  gap: var(--space-3);
  padding: var(--space-3) 0;
  border-bottom: 1px solid var(--border-subtle);

  &:last-child {
    border-bottom: none;
  }

  dt {
    margin: 0;
    color: var(--text-muted);
    font-size: var(--font-size-sm);
    font-weight: 500;
  }

  dd {
    margin: 0;
    color: var(--text-primary);
    font-size: var(--font-size-sm);
    word-break: break-word;
  }
}

.drawer-actions {
  margin-top: var(--space-6);
  padding-top: var(--space-4);
  border-top: 1px solid var(--border-subtle);
}

.audit-link {
  color: var(--color-primary);
  font-size: var(--font-size-sm);
  font-weight: 500;
  text-decoration: none;

  &:hover {
    text-decoration: underline;
  }
}
</style>
