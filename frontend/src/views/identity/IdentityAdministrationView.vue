<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import GroupManagementPanel from '@/views/identity/GroupManagementPanel.vue'
import UserManagementPanel from '@/views/identity/UserManagementPanel.vue'

const { t } = useI18n()
const activeTab = ref('users')
</script>

<template>
  <div class="identity-page">
    <header class="page-header">
      <h1>{{ t('identity.title') }}</h1>
      <p>{{ t('identity.description') }}</p>
    </header>

    <el-tabs v-model="activeTab" class="identity-tabs">
      <el-tab-pane :label="t('identity.tabs.users')" name="users">
        <UserManagementPanel />
      </el-tab-pane>
      <el-tab-pane :label="t('identity.tabs.groups')" name="groups">
        <GroupManagementPanel />
      </el-tab-pane>
    </el-tabs>
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

.identity-tabs {
  background: #fff;
  padding: 0 1rem 1rem;
  border: 1px solid var(--border-color);
  border-radius: 6px;
}
</style>
