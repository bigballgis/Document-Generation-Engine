<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import TeamSettingsReminderTimingDialog from '@/components/collaboration/TeamSettingsReminderTimingDialog.vue'
import GroupManagementPanel from '@/views/identity/GroupManagementPanel.vue'
import { canMaintainCollaborationTimeoutConfig, MANAGEMENT_ROLES } from '@/auth/roles'
import { useCapabilities } from '@/composables/useCapabilities'
import { useSessionStore } from '@/stores/session'

const { t } = useI18n()
const sessionStore = useSessionStore()
const { context } = useCapabilities()

const teamSettingsOpen = ref(false)

const showTeamSettings = computed(() => {
  const roles = sessionStore.session?.roles ?? []
  return (
    roles.includes(MANAGEMENT_ROLES.GROUP_ADMIN) &&
    canMaintainCollaborationTimeoutConfig(context.value)
  )
})
</script>

<template>
  <AppPageLayout layout-variant="fluid">
    <PageHeader
      :title="t('identity.groupsPageTitle')"
      :description="t('identity.groupsPageDescription')"
    >
      <template v-if="showTeamSettings" #actions>
        <el-button data-testid="team-settings-button" @click="teamSettingsOpen = true">
          {{ t('identity.groups.teamSettings') }}
        </el-button>
      </template>
    </PageHeader>

    <GroupManagementPanel />

    <TeamSettingsReminderTimingDialog v-model="teamSettingsOpen" />
  </AppPageLayout>
</template>
