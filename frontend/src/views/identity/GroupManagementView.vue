<script setup lang="ts">

import { useI18n } from 'vue-i18n'

import AppPageLayout from '@/components/layout/AppPageLayout.vue'

import PageHeader from '@/components/layout/PageHeader.vue'

import GroupManagementPanel from '@/views/identity/GroupManagementPanel.vue'



const { t } = useI18n()

</script>



<template>

  <AppPageLayout layout-variant="fluid">

    <PageHeader

      :title="t('identity.groupsPageTitle')"

      :description="t('identity.groupsPageDescription')"

    />

    <GroupManagementPanel />

  </AppPageLayout>

</template>
