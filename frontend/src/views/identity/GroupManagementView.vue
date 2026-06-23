<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import GroupManagementPanel from '@/views/identity/GroupManagementPanel.vue'

const { t } = useI18n()
</script>

<template>
  <div class="identity-page">
    <header class="page-header">
      <h1>{{ t('identity.groupsPageTitle') }}</h1>
      <p>{{ t('identity.groupsPageDescription') }}</p>
    </header>

    <div class="panel-shell">
      <GroupManagementPanel />
    </div>
  </div>
</template>

<style scoped lang="scss">
.identity-page {
  padding: 2rem;
}

.page-header {
  margin-bottom: 1.5rem;

  h1 {
    margin: 0.25rem 0;
    font-size: 1.75rem;
  }

  p {
    margin: 0;
    color: var(--text-muted);
  }
}

.panel-shell {
  background: #fff;
  padding: 1rem;
  border: 1px solid var(--border-color);
  border-radius: 6px;
}
</style>
