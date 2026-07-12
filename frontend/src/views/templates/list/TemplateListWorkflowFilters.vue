<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { WorkflowFilterKey } from '@/views/templates/useTemplateListCatalog'

defineProps<{
  activeWorkflowFilter: WorkflowFilterKey | null
  workflowFilterChips: Array<{ key: WorkflowFilterKey; labelKey: string }>
}>()

const emit = defineEmits<{
  clear: []
  change: [key: WorkflowFilterKey, checked: boolean]
}>()

const { t } = useI18n()
</script>

<template>
  <div v-if="workflowFilterChips.length > 0" class="workflow-filters">
    <el-check-tag
      :checked="activeWorkflowFilter === null"
      @change="(checked: boolean) => checked && emit('clear')"
    >
      {{ t('templates.list.workflowFilters.all') }}
    </el-check-tag>
    <el-check-tag
      v-for="chip in workflowFilterChips"
      :key="chip.key"
      :checked="activeWorkflowFilter === chip.key"
      @change="(checked: boolean) => emit('change', chip.key, checked)"
    >
      {{ t(chip.labelKey) }}
    </el-check-tag>
  </div>
</template>

<style scoped lang="scss">
.workflow-filters {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}
</style>
