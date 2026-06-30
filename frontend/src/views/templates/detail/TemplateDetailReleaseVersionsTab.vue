<script setup lang="ts">
import TemplateReleaseVersionHistoryPanel from '@/components/templates/TemplateReleaseVersionHistoryPanel.vue'
import type { TemplateLifecycleStatus } from '@/types/template'

defineProps<{
  templateId: string
  templateLifecycleStatus: TemplateLifecycleStatus
}>()

const emit = defineEmits<{
  changed: []
}>()
</script>

<template>
  <TemplateReleaseVersionHistoryPanel
    :template-id="templateId"
    :template-lifecycle-status="templateLifecycleStatus"
    @changed="emit('changed')"
  />
</template>
