<script setup lang="ts">
import { computed, ref, toRef } from 'vue'
import { useI18n } from 'vue-i18n'
import InlinePdfPreviewViewer from '@/components/templates/InlinePdfPreviewViewer.vue'
import { usePreviewRunPdfBlob } from '@/components/templates/usePreviewRunPdfBlob'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { downloadBlobExport } from '@/utils/downloadExport'
import { useTemplatePanelDataStore } from '@/stores/templatePanelData'
import { isComparablePreviewRun } from '@/components/templates/renderedCompareSelection'
import type { PreviewRunSummary } from '@/types/template'

const props = defineProps<{
  templateId: string
  run: PreviewRunSummary
  paneTestId: string
}>()

const { t } = useI18n()
const { formatDateTime } = useLocaleFormatters()
const panelDataStore = useTemplatePanelDataStore()
const downloadingFormat = ref<'docx' | 'pdf' | null>(null)

const enabled = computed(() => isComparablePreviewRun(props.run))

const { loading, errorMessage, pdfBlob } = usePreviewRunPdfBlob({
  templateId: toRef(props, 'templateId'),
  previewId: computed(() => props.run.previewId),
  enabled,
})

async function downloadArtifact(format: 'docx' | 'pdf') {
  downloadingFormat.value = format
  try {
    const { blob, filename } = await panelDataStore.downloadPreviewArtifact(
      props.templateId,
      props.run.previewId,
      format,
    )
    downloadBlobExport(filename, blob)
  } catch {
    // Pane already surfaces PDF load errors; download failures stay non-blocking for compare.
  } finally {
    downloadingFormat.value = null
  }
}
</script>

<template>
  <article class="rendered-compare-pane" :data-testid="paneTestId">
    <header class="rendered-compare-pane__meta">
      <dl>
        <div>
          <dt>{{ t('templates.preview.previewId') }}</dt>
          <dd data-testid="rendered-compare-preview-id">{{ run.previewId }}</dd>
        </div>
        <div>
          <dt>{{ t('templates.previewHistory.columns.runAt') }}</dt>
          <dd>{{ formatDateTime(run.createdAt) }}</dd>
        </div>
        <div>
          <dt>{{ t('templates.previewHistory.columns.dataSet') }}</dt>
          <dd>{{ run.testDataSetId ?? t('templates.previewHistory.adhocRun') }}</dd>
        </div>
      </dl>
      <div class="rendered-compare-pane__downloads">
        <el-button
          size="small"
          :disabled="!run.docxAvailable"
          :loading="downloadingFormat === 'docx'"
          data-testid="rendered-compare-download-docx"
          @click="downloadArtifact('docx')"
        >
          {{ t('templates.previewHistory.downloadDocx') }}
        </el-button>
        <el-button
          size="small"
          :disabled="!run.pdfAvailable"
          :loading="downloadingFormat === 'pdf'"
          data-testid="rendered-compare-download-pdf"
          @click="downloadArtifact('pdf')"
        >
          {{ t('templates.previewHistory.downloadPdf') }}
        </el-button>
      </div>
    </header>

    <InlinePdfPreviewViewer
      :blob="pdfBlob"
      :loading="loading"
      :error-message="errorMessage"
    />
  </article>
</template>

<style scoped lang="scss">
.rendered-compare-pane {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  min-width: 0;
  padding: var(--space-3);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--surface-card);

  &__meta {
    display: flex;
    flex-direction: column;
    gap: var(--space-2);
  }

  dl {
    display: grid;
    gap: var(--space-2);
    margin: 0;
  }

  dt {
    margin: 0;
    font-size: var(--font-size-sm);
    color: var(--text-muted);
  }

  dd {
    margin: 0;
    font-size: var(--font-size-sm);
    color: var(--text-primary);
    word-break: break-all;
  }

  &__downloads {
    display: flex;
    flex-wrap: wrap;
    gap: var(--space-2);
  }
}
</style>
