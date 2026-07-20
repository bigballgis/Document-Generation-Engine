<script setup lang="ts">
import { ref } from 'vue'
import type { ComponentPublicInstance } from 'vue'
import { useI18n } from 'vue-i18n'
import TemplateVersionLinesPanel from '@/components/templates/TemplateVersionLinesPanel.vue'

defineProps<{
  templateId: string
  canClone: boolean
  canManageVersions: boolean
}>()

const dependenciesGuidanceVisible = defineModel<boolean>('dependenciesGuidanceVisible', {
  required: true,
})

const emit = defineEmits<{
  cloned: []
  changed: []
}>()

const { t } = useI18n()

const versionLinesPanelRef = ref<ComponentPublicInstance<{ reload: () => Promise<void> }> | null>(
  null,
)

defineExpose({
  reloadVersionLines: () => versionLinesPanelRef.value?.reload(),
})
</script>

<template>
  <el-alert
    v-if="dependenciesGuidanceVisible"
    class="dependencies-guidance"
    type="info"
    show-icon
    closable
    data-testid="hub-dependencies-guidance"
    :title="t('templates.packageHub.dependenciesGuidance')"
    @close="dependenciesGuidanceVisible = false"
  />

  <TemplateVersionLinesPanel
    ref="versionLinesPanelRef"
    :template-id="templateId"
    :can-clone="canClone"
    :can-manage-versions="canManageVersions"
    @cloned="emit('cloned')"
    @changed="emit('changed')"
  />
</template>

<style scoped lang="scss">
.dependencies-guidance {
  margin-bottom: var(--space-4);
}
</style>
