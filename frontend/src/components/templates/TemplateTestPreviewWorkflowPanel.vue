<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import SectionPanelHeader from '@/components/common/SectionPanelHeader.vue'
import type { TemplateLifecycleStatus } from '@/types/template'

const props = defineProps<{
  lifecycleStatus: TemplateLifecycleStatus
}>()

const { t } = useI18n()

const statusLabelKey = computed(() => `templates.status.${props.lifecycleStatus}`)
</script>

<template>
  <section class="test-preview-workflow">
    <SectionPanelHeader
      :title="t('templates.testPreview.workflow.title')"
      :help-title="t('templates.testPreview.workflow.helpTitle')"
      :help-content="t('templates.testPreview.workflow.helpContent')"
    />

    <div class="test-preview-workflow__status-row">
      <span class="test-preview-workflow__status-label">{{ t('templates.testPreview.workflow.status') }}</span>
      <el-tag size="small" type="info">{{ t(statusLabelKey) }}</el-tag>
    </div>
  </section>
</template>

<style scoped lang="scss">
.test-preview-workflow {
  margin-bottom: 1rem;
  padding: 0.875rem 1rem;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--surface-muted);

  &__status-row {
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }

  &__status-label {
    font-size: 0.875rem;
    color: var(--text-muted);
  }
}
</style>
