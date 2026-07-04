<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import TaskHubPartitionSection from '@/components/dashboard/TaskHubPartitionSection.vue'
import type { TaskPartition } from '@/composables/useWorkflowTasks'
import type { CollaborationTimeoutConfig } from '@/types/collaboration'

defineProps<{
  loading: boolean
  showCollaborationLoading: boolean
  collaborationFetchFailed: boolean
  collaborationLoadErrorKey: string
  visiblePartitions: TaskPartition[]
  globalTimeoutConfig: CollaborationTimeoutConfig | null
  groupTimeoutConfigs: Record<string, CollaborationTimeoutConfig | null>
}>()

const emit = defineEmits<{
  retryCollaboration: []
  openTask: [path: string]
}>()

const { t } = useI18n()
</script>

<template>
  <section id="tasks-section" class="tasks-section">
    <el-skeleton v-if="loading || showCollaborationLoading" :rows="5" animated />

    <LoadErrorPanel
      v-if="collaborationFetchFailed"
      :message-key="collaborationLoadErrorKey"
      @retry="emit('retryCollaboration')"
    />

    <template v-if="!loading && !showCollaborationLoading">
      <el-empty
        v-if="visiblePartitions.length === 0 && !collaborationFetchFailed"
        :description="t('dashboard.tasks.empty')"
      />

      <TaskHubPartitionSection
        v-for="partition in visiblePartitions"
        :key="partition.id"
        :partition="partition"
        :global-timeout-config="globalTimeoutConfig"
        :group-timeout-configs="groupTimeoutConfigs"
        @open="(path: string) => emit('openTask', path)"
      />
    </template>
  </section>
</template>

<style scoped lang="scss">
.tasks-section {
  margin-bottom: var(--space-6);
}
</style>
