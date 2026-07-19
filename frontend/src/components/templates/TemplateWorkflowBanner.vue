<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useCapabilities } from '@/composables/useCapabilities'
import ContextHelpTrigger from '@/components/common/ContextHelpTrigger.vue'
import { resolveTemplateWorkflowBannerContext } from '@/utils/templateWorkflowBannerContext'
import type { TemplateDetail } from '@/types/template'

const props = defineProps<{
  template: TemplateDetail
}>()

const emit = defineEmits<{
  openLifecycle: []
}>()

const { t } = useI18n()
const {
  authorTemplates,
  decideTests,
  decideApprovals,
  decideLegalApprovals,
  publishTemplates,
} = useCapabilities()

const banner = computed(() =>
  resolveTemplateWorkflowBannerContext(
    props.template.lifecycleStatus,
    {
      authorTemplates: authorTemplates.value,
      decideTests: decideTests.value,
      decideApprovals: decideApprovals.value,
      decideLegalApprovals: decideLegalApprovals.value,
      publishTemplates: publishTemplates.value,
    },
    props.template.approvalSubState ?? null,
  ),
)
</script>

<template>
  <el-alert
    v-if="banner"
    class="workflow-banner workflow-banner--compact"
    type="warning"
    :closable="false"
    show-icon
  >
    <template #title>
      <span class="workflow-banner__title-row">
        <span>{{ t(banner.titleKey) }}</span>
        <ContextHelpTrigger
          :title="t(banner.titleKey)"
          :content="t(banner.descriptionKey)"
        />
      </span>
    </template>
    <el-button type="primary" link class="workflow-banner__cta" @click="emit('openLifecycle')">
      {{ t('templates.workflow.openLifecyclePanel') }}
    </el-button>
  </el-alert>
</template>

<style scoped lang="scss">
.workflow-banner {
  margin-bottom: 0.75rem;

  &--compact {
    :deep(.el-alert__content) {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: space-between;
      gap: 0.5rem;
    }
  }
}

.workflow-banner__title-row {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
}

.workflow-banner__cta {
  margin: 0;
  padding: 0;
}
</style>
