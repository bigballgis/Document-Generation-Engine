<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { TemplateJourneyWorkspaceQuery } from '@/utils/templateJourneyWorkspaceLink'

defineProps<{
  workspaceQuery: TemplateJourneyWorkspaceQuery | null
}>()

const emit = defineEmits<{
  navigate: [query: TemplateJourneyWorkspaceQuery]
}>()

const { t } = useI18n()
</script>

<template>
  <el-button
    v-if="workspaceQuery"
    link
    type="primary"
    data-journey-workspace-link
    @click="emit('navigate', workspaceQuery)"
  >
    {{ t('templates.packageHub.openDevWorkflow') }}
  </el-button>
</template>
