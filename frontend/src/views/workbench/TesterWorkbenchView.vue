<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import CollaborationWorkItemPanel from '@/components/collaboration/CollaborationWorkItemPanel.vue'

const { t } = useI18n()
</script>

<template>
  <AppPageLayout class="tester-workbench-page" max-width="1200px">
    <header class="page-header">
      <h1>{{ t('workbench.tester.title') }}</h1>
      <p>{{ t('workbench.tester.description') }}</p>
    </header>

    <CollaborationWorkItemPanel
      queue="TEST"
      empty-message-key="workbench.tester.empty"
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
