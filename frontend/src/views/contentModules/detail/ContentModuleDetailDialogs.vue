<script setup lang="ts">
import ContentModuleLifecycleImpactDialog from '@/components/contentModules/ContentModuleLifecycleImpactDialog.vue'
import ContentModuleSharedGroupsSettingsDialog from '@/components/contentModules/ContentModuleSharedGroupsSettingsDialog.vue'
import ContentModuleVersionDialog from '@/components/contentModules/ContentModuleVersionDialog.vue'
import type { ContentModuleLifecycleImpactSummary, ContentModuleVersion } from '@/types/contentModule'

defineProps<{
  moduleId: string
  versionDialogMode: 'create' | 'edit'
  version: ContentModuleVersion | null
  loadingImpact: boolean
  impact: ContentModuleLifecycleImpactSummary | null
  operationLabelKey: string
  ownerGroupCode: string
  sharedGroupCodes: string[]
  canConfigureSharedGroups: boolean
}>()

const versionDialogOpen = defineModel<boolean>('versionDialogOpen', { required: true })
const impactDialogOpen = defineModel<boolean>('impactDialogOpen', { required: true })
const settingsDialogOpen = defineModel<boolean>('settingsDialogOpen', { required: true })

const emit = defineEmits<{
  saved: []
  confirm: []
  settingsSaved: []
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

  <ContentModuleSharedGroupsSettingsDialog
    v-model="settingsDialogOpen"
    :module-id="moduleId"
    :owner-group-code="ownerGroupCode"
    :shared-group-codes="sharedGroupCodes"
    :can-configure="canConfigureSharedGroups"
    @saved="emit('settingsSaved')"
  />
</template>
