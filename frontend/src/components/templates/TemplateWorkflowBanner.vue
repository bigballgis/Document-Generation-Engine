<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useCapabilities } from '@/composables/useCapabilities'
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
  publishTemplates,
} = useCapabilities()

const banner = computed(() => {
  const status = props.template.lifecycleStatus
  if (status === 'TESTING' && decideTests.value) {
    return {
      titleKey: 'dashboard.tasks.templateTest.title',
      descriptionKey: 'dashboard.tasks.templateTest.description',
    }
  }
  if (status === 'APPROVAL' && decideApprovals.value) {
    return {
      titleKey: 'dashboard.tasks.templateApproval.title',
      descriptionKey: 'dashboard.tasks.templateApproval.description',
    }
  }
  if (status === 'PENDING_RELEASE' && publishTemplates.value) {
    return {
      titleKey: 'dashboard.tasks.templatePublish.title',
      descriptionKey: 'dashboard.tasks.templatePublish.description',
    }
  }
  if (status === 'DRAFT' && authorTemplates.value) {
    return {
      titleKey: 'dashboard.tasks.templateDraft.title',
      descriptionKey: 'dashboard.tasks.templateDraft.description',
    }
  }
  return null
})
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
