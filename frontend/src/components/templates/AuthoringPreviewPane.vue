<script setup lang="ts">
import { computed, toRef } from 'vue'
import { useI18n } from 'vue-i18n'
import InlinePdfPreviewViewer from '@/components/templates/InlinePdfPreviewViewer.vue'
import TemplatePreviewPanel from '@/components/templates/TemplatePreviewPanel.vue'
import { useInlinePdfPreview } from '@/components/templates/useInlinePdfPreview'
import type { AnchorBinding, PreviewRecord } from '@/types/template'

const props = defineProps<{
  templateId: string
  bindings: AnchorBinding[]
  preview: PreviewRecord | null
  stale?: boolean
  refreshing?: boolean
}>()

const emit = defineEmits<{
  refresh: []
}>()

const { t } = useI18n()

const hasPreview = computed(() => props.preview !== null)
const refreshDisabled = computed(() => props.refreshing === true)

const {
  loading: inlinePdfLoading,
  errorMessage: inlinePdfError,
  pdfBlob,
  canShowInlinePdf,
} = useInlinePdfPreview({
  templateId: toRef(props, 'templateId'),
  preview: toRef(props, 'preview'),
})

function handleRefresh() {
  if (refreshDisabled.value) {
    return
  }
  emit('refresh')
}
</script>

<template>
  <section class="authoring-preview-pane" data-testid="authoring-preview-pane">
    <header class="authoring-preview-pane__header">
      <h3 class="authoring-preview-pane__title">{{ t('templates.authoring.previewPaneTitle') }}</h3>
      <el-tag
        v-if="stale && hasPreview"
        type="warning"
        size="small"
        data-testid="authoring-preview-stale-badge"
      >
        {{ t('templates.authoring.previewStale') }}
      </el-tag>
    </header>

    <p class="authoring-preview-pane__boundary" data-testid="authoring-preview-boundary">
      {{ t('templates.authoring.previewBoundary') }}
    </p>

    <div class="authoring-preview-pane__actions">
      <el-button
        :loading="refreshing"
        :disabled="refreshDisabled"
        data-testid="authoring-preview-refresh"
        @click="handleRefresh"
      >
        {{ t('templates.authoring.previewRefreshNow') }}
      </el-button>
    </div>

    <section
      v-if="hasPreview && canShowInlinePdf"
      class="authoring-preview-pane__inline-pdf"
      data-testid="authoring-inline-pdf-section"
    >
      <h4 class="authoring-preview-pane__inline-pdf-title">
        {{ t('templates.authoring.inlinePdfTitle') }}
      </h4>
      <InlinePdfPreviewViewer
        :blob="pdfBlob"
        :loading="inlinePdfLoading || refreshing"
        :error-message="inlinePdfError"
      />
    </section>

    <TemplatePreviewPanel
      v-if="hasPreview"
      compact
      :template-id="templateId"
      :bindings="bindings"
      :preview="preview"
    />

    <el-empty
      v-else
      data-testid="authoring-preview-empty"
      :description="t('templates.authoring.previewEmptyTitle')"
    >
      <p class="authoring-preview-pane__empty-hint">
        {{ t('templates.authoring.previewEmptyDescription') }}
      </p>
    </el-empty>
  </section>
</template>

<style scoped lang="scss">
.authoring-preview-pane {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);

  &__header {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--space-2);
  }

  &__title {
    margin: 0;
    font-size: var(--font-size-md);
    font-weight: 650;
  }

  &__boundary {
    margin: 0;
    padding: var(--space-3);
    border-radius: var(--radius-sm);
    background: var(--status-warning-bg);
    color: var(--status-warning-text-strong);
    font-size: var(--font-size-sm);
    line-height: 1.5;
  }

  &__actions {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
  }

  &__inline-pdf {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  &__inline-pdf-title {
    margin: 0;
    font-size: var(--font-size-sm);
    font-weight: 650;
    color: var(--text-secondary);
  }

  &__empty-hint {
    margin: 0;
    color: var(--text-muted);
    font-size: var(--font-size-sm);
    text-align: center;
    max-width: 28rem;
  }
}
</style>
