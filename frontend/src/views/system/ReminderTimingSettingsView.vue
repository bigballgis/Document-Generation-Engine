<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import CollaborationTimeoutConfigPanel from '@/components/collaboration/CollaborationTimeoutConfigPanel.vue'

const { t } = useI18n()
</script>

<template>
  <AppPageLayout layout-variant="fluid">
    <PageHeader
      :title="t('collaboration.timeoutConfig.title')"
      :description="t('collaboration.timeoutConfig.description')"
    />
    <CollaborationTimeoutConfigPanel mode="global" />
  </AppPageLayout>
</template>
