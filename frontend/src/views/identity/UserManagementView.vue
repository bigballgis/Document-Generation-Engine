<script setup lang="ts">

import { useI18n } from 'vue-i18n'

import AppPageLayout from '@/components/layout/AppPageLayout.vue'

import PageHeader from '@/components/layout/PageHeader.vue'

import UserManagementPanel from '@/views/identity/UserManagementPanel.vue'



const { t } = useI18n()

</script>



<template>

  <AppPageLayout>

    <PageHeader

      :title="t('identity.usersPageTitle')"

      :description="t('identity.usersPageDescription')"

    />

    <UserManagementPanel />

  </AppPageLayout>

</template>
