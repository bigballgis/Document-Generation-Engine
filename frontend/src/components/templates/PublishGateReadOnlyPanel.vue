<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import { fetchReleasePublishGate } from '@/api/templates'
import { resolveApiError, resolveApiErrorMessageKey } from '@/api/errorEnvelope'
import { resolvePublishGateLoadErrorKey } from '@/utils/templateBindingGateDisplay'
import {
  mapPublishGateChecklistItems,
  type PublishGateDisplayItem,
} from '@/utils/templateLifecycleDecisionForm'
import type { PublishGateChecklist } from '@/types/template'

const props = defineProps<{
  templateId: string
  releaseVersion: string
}>()

const { t, te } = useI18n()

const loading = ref(false)
const loadErrorKey = ref<string | null>(null)
const loadErrorRetryable = ref(true)
const checklist = ref<PublishGateChecklist | null>(null)

function resolveItemLabel(item: {
  checkCode: string
  messageKey: string
  summary: string
}): string {
  if (te(item.messageKey)) {
    return t(item.messageKey)
  }
  const codeKey = `templates.publishGate.checkCodes.${item.checkCode}`
  if (te(codeKey)) {
    return t(codeKey)
  }
  return item.summary
}

const displayItems = computed<PublishGateDisplayItem[]>(() => {
  if (!checklist.value) {
    return []
  }
  return mapPublishGateChecklistItems(checklist.value.items, resolveItemLabel)
})

async function loadGate() {
  if (!props.templateId || !props.releaseVersion) {
    return
  }
  loading.value = true
  loadErrorKey.value = null
  try {
    checklist.value = await fetchReleasePublishGate(props.templateId, props.releaseVersion)
  } catch (error) {
    checklist.value = null
    loadErrorKey.value = resolvePublishGateLoadErrorKey(
      resolveApiErrorMessageKey(error, 'templates.error.loadPublishGate'),
    )
    loadErrorRetryable.value = resolveApiError(error)?.error.retryable ?? true
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadGate()
})

watch(
  () => [props.templateId, props.releaseVersion] as const,
  () => {
    void loadGate()
  },
)
</script>

<template>
  <el-card shadow="never" class="publish-gate-readonly">
    <h3>{{ t('templates.releaseDetail.approval.publishGateTitle') }}</h3>
    <p class="publish-gate-readonly__subtitle">
      {{ t('templates.releaseDetail.approval.publishGateSubtitle') }}
    </p>

    <LoadErrorPanel
      v-if="loadErrorKey"
      :message-key="loadErrorKey"
      :retryable="loadErrorRetryable"
      @retry="loadGate"
    />

    <el-skeleton v-else-if="loading" :rows="3" animated />

    <EmptyStatePanel
      v-else-if="displayItems.length === 0"
      title-key="templates.releaseDetail.approval.publishGateEmptyTitle"
      description-key="templates.releaseDetail.approval.publishGateEmptyDescription"
    />

    <ul v-else class="gate-list">
      <li v-for="item in displayItems" :key="item.key">
        <span>{{ item.label }}</span>
        <el-tag v-if="item.informational" type="info" size="small">
          {{ t('templates.publishGate.informational') }}
        </el-tag>
        <el-tag v-else :type="item.ready ? 'success' : 'warning'" size="small">
          {{ item.ready ? t('templates.publishGate.ready') : t('templates.publishGate.pending') }}
        </el-tag>
      </li>
    </ul>
  </el-card>
</template>

<style scoped lang="scss">
.publish-gate-readonly {
  margin-bottom: 1rem;
}

.publish-gate-readonly__subtitle {
  margin: 0 0 1rem;
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

.gate-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;

  li {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
  }
}
</style>
