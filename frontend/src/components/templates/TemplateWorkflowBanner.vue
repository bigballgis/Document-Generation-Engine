<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useCapabilities } from '@/composables/useCapabilities'
import { resolveTemplateWorkflowBannerContext } from '@/utils/templateWorkflowBannerContext'
import type { TemplateDetail } from '@/types/template'

const props = defineProps<{
  template: TemplateDetail
}>()

const emit = defineEmits<{
  openLifecycle: []
}>()

const { t } = useI18n()
const { authorTemplates, decideTests, decideApprovals, publishTemplates } = useCapabilities()

const banner = computed(() =>
  resolveTemplateWorkflowBannerContext(
    props.template.lifecycleStatus,
    {
      authorTemplates: authorTemplates.value,
      decideTests: decideTests.value,
      decideApprovals: decideApprovals.value,
      publishTemplates: publishTemplates.value,
    },
    props.template.approvalSubState ?? null,
  ),
)
</script>

<template>
  <el-alert
    v-if="banner"
    class="workflow-banner"
    type="warning"
    :closable="false"
    show-icon
  >
    <template #title>
      {{ t('templates.workflow.actionRequired') }}
    </template>
    <p class="workflow-banner__title">{{ t(banner.titleKey) }}</p>
    <p class="workflow-banner__description">{{ t(banner.descriptionKey) }}</p>
    <el-button type="primary" link class="workflow-banner__cta" @click="emit('openLifecycle')">
      {{ t('templates.workflow.openLifecyclePanel') }}
    </el-button>
  </el-alert>
</template>

<style scoped lang="scss">
.workflow-banner {
  margin-bottom: 1rem;
}

.workflow-banner__title {
  margin: 0.25rem 0 0;
  font-weight: 600;
}

.workflow-banner__description,
.workflow-banner__cta {
  margin: 0.35rem 0 0;
}

.workflow-banner__description {
  color: var(--text-muted);
}
</style>
