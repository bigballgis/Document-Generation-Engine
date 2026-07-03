<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'

defineProps<{
  templateId: string
}>()

const { t } = useI18n()
</script>

<template>
  <el-card shadow="never" class="section-card">
    <h2>{{ t('templates.policy.recentInvocations.title') }}</h2>
    <p class="panel-hint">{{ t('templates.policy.recentInvocations.description') }}</p>
    <EmptyStatePanel
      title-key="templates.policy.recentInvocations.comingSoonTitle"
      description-key="templates.policy.recentInvocations.comingSoonDescription"
    />
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
</style>
