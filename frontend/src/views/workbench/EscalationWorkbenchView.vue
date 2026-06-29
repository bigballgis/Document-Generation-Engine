<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import CollaborationWorkItemPanel from '@/components/collaboration/CollaborationWorkItemPanel.vue'

const { t } = useI18n()
</script>

<template>
  <AppPageLayout class="escalation-workbench-page" max-width="1200px">
    <header class="page-header">
      <h1>{{ t('workbench.escalation.title') }}</h1>
      <p>{{ t('workbench.escalation.description') }}</p>
    </header>

    <CollaborationWorkItemPanel
      queue="ESCALATION"
      empty-message-key="workbench.escalation.empty"
    />
  </AppPageLayout>
</template>

<style scoped lang="scss">
.page-header {
  margin-bottom: 1.5rem;

  h1 {
    margin: 0 0 0.25rem;
    font-size: 1.75rem;
  }

  p {
    margin: 0;
    color: var(--text-muted);
  }
}
</style>
