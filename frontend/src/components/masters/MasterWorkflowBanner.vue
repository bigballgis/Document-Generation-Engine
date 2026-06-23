<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useCapabilities } from '@/composables/useCapabilities'
import type { MasterDocumentDetail } from '@/types/master'

const props = defineProps<{
  master: MasterDocumentDetail
}>()

const { t } = useI18n()
const { manageMasters, reviewMasters } = useCapabilities()

const banner = computed(() => {
  const status = props.master.status
  if ((status === 'DRAFT' || status === 'REJECTED') && manageMasters.value) {
    return {
      titleKey: 'masters.workflow.submitReview.title',
      descriptionKey: 'masters.workflow.submitReview.description',
    }
  }
  if (status === 'PENDING_REVIEW' && reviewMasters.value) {
    return {
      titleKey: 'masters.workflow.reviewDecision.title',
      descriptionKey: 'masters.workflow.reviewDecision.description',
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
      {{ t('masters.workflow.actionRequired') }}
    </template>
    <p class="workflow-banner__title">{{ t(banner.titleKey) }}</p>
    <p class="workflow-banner__description">{{ t(banner.descriptionKey) }}</p>
    <p class="workflow-banner__hint">{{ t('masters.workflow.useReviewPanel') }}</p>
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
.workflow-banner__hint {
  margin: 0.35rem 0 0;
  color: var(--text-muted);
}
</style>
