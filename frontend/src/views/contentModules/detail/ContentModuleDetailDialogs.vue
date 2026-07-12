<script setup lang="ts">
import ContentModuleLifecycleImpactDialog from '@/components/contentModules/ContentModuleLifecycleImpactDialog.vue'
import ContentModuleVersionDialog from '@/components/contentModules/ContentModuleVersionDialog.vue'
import type { ContentModuleLifecycleImpactSummary, ContentModuleVersion } from '@/types/contentModule'

defineProps<{
  moduleId: string
  versionDialogMode: 'create' | 'edit'
  version: ContentModuleVersion | null
  loadingImpact: boolean
  impact: ContentModuleLifecycleImpactSummary | null
  operationLabelKey: string
}>()

const versionDialogOpen = defineModel<boolean>('versionDialogOpen', { required: true })
const impactDialogOpen = defineModel<boolean>('impactDialogOpen', { required: true })

const emit = defineEmits<{
  saved: []
  confirm: []
}>()
</script>

<template>
  <ContentModuleVersionDialog
    v-model="versionDialogOpen"
    :module-id="moduleId"
    :mode="versionDialogMode"
    :version="version"
    @saved="emit('saved')"
  />

  <ContentModuleLifecycleImpactDialog
    v-model="impactDialogOpen"
    :loading="loadingImpact"
    :impact="impact"
    :operation-label-key="operationLabelKey"
    @confirm="emit('confirm')"
  />
</template>
