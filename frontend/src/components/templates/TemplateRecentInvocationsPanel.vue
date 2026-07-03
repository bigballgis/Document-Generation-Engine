<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { listRecentInvocations } from '@/api/apiPolicy'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import type { ManagementInvocationSummary } from '@/types/template'

const props = defineProps<{
  templateId: string
}>()

const { t } = useI18n()

const loading = ref(true)
const loadFailed = ref(false)
const rows = ref<ManagementInvocationSummary[]>([])

async function loadRecentInvocations() {
  loading.value = true
  loadFailed.value = false
  try {
    rows.value = await listRecentInvocations(props.templateId, 10)
  } catch {
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadRecentInvocations()
})
</script>

<template>
  <el-card shadow="never" class="section-card">
    <h2>{{ t('templates.policy.recentInvocations.title') }}</h2>
    <p class="panel-hint">{{ t('templates.policy.recentInvocations.description') }}</p>
    <el-skeleton v-if="loading" :rows="3" animated />
    <LoadErrorPanel
      v-else-if="loadFailed"
      message-key="templates.policy.recentInvocations.loadFailed"
      @retry="loadRecentInvocations"
    />
    <EmptyStatePanel
      v-else-if="rows.length === 0"
      title-key="templates.policy.recentInvocations.emptyTitle"
      description-key="templates.policy.recentInvocations.emptyDescription"
    />
    <el-table v-else :data="rows" stripe class="invocation-table">
      <el-table-column prop="createdAt" :label="t('templates.policy.recentInvocations.columns.createdAt')" min-width="160" />
      <el-table-column prop="invocationId" :label="t('templates.policy.recentInvocations.columns.invocationId')" min-width="140" />
      <el-table-column prop="invocationKind" :label="t('templates.policy.recentInvocations.columns.kind')" min-width="120" />
      <el-table-column prop="status" :label="t('templates.policy.recentInvocations.columns.status')" min-width="120" />
      <el-table-column prop="requestId" :label="t('templates.policy.recentInvocations.columns.requestId')" min-width="140" />
      <el-table-column
        prop="accessAccountSummary"
        :label="t('templates.policy.recentInvocations.columns.accessAccount')"
        min-width="120"
      />
    </el-table>
  </el-card>
</template>

<style scoped lang="scss">
.section-card {
  margin-bottom: var(--space-6);

  h2 {
    margin: 0 0 var(--space-3);
    font-size: var(--font-size-lg);
  }
}

.panel-hint {
  margin: 0 0 var(--space-4);
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

.invocation-table {
  width: 100%;
}
</style>
