<script setup lang="ts">
import { toRef } from 'vue'
import FidelityWarningList from '@/components/authoring/FidelityWarningList.vue'
import { useTemplatePreviewPanel } from '@/components/templates/useTemplatePreviewPanel'
import type { AnchorBinding, PreviewRecord } from '@/types/template'

const props = withDefaults(
  defineProps<{
    templateId: string
    bindings: AnchorBinding[]
    preview: PreviewRecord | null
    compact?: boolean
  }>(),
  {
    compact: false,
  },
)

const {
  t,
  loading,
  downloadingFormat,
  latestPreview,
  comparisonItems,
  comparisonSummary,
  refreshPreview,
  severityTagType,
  locationLabel,
  previewArtifact,
  devVersionId,
  markingViewedIndex,
  markWarningViewed,
  canDownloadDocx,
  canDownloadPdf,
  downloadArtifact,
} = useTemplatePreviewPanel({
  templateId: toRef(props, 'templateId'),
  preview: toRef(props, 'preview'),
})
</script>

<template>
  <div class="preview-panel">
    <template v-if="latestPreview">
      <dl class="preview-meta">
        <div>
          <dt>{{ t('templates.preview.previewId') }}</dt>
          <dd>{{ latestPreview.previewId }}</dd>
        </div>
        <div>
          <dt>{{ t('templates.preview.status') }}</dt>
          <dd>{{ latestPreview.status }}</dd>
        </div>
        <div>
          <dt>{{ t('templates.preview.comparisonSummary') }}</dt>
          <dd>{{ comparisonSummary ?? t('templates.preview.noComparison') }}</dd>
        </div>
        <div v-if="latestPreview.testDataSetId">
          <dt>{{ t('templates.preview.testDataSetId') }}</dt>
          <dd>{{ latestPreview.testDataSetId }}</dd>
        </div>
      </dl>

      <div v-if="canDownloadDocx || canDownloadPdf" class="preview-downloads">
        <el-button
          v-if="canDownloadDocx"
          :loading="downloadingFormat === 'docx'"
          @click="downloadArtifact('docx')"
        >
          {{ t('templates.previewHistory.downloadDocx') }}
        </el-button>
        <el-button
          v-if="canDownloadPdf"
          :loading="downloadingFormat === 'pdf'"
          @click="downloadArtifact('pdf')"
        >
          {{ t('templates.previewHistory.downloadPdf') }}
        </el-button>
      </div>

      <el-button :loading="loading" @click="refreshPreview">
        {{ t('templates.preview.refresh') }}
      </el-button>

      <h3>{{ t('templates.preview.comparisonTitle') }}</h3>
      <el-empty
        v-if="!comparisonItems.length"
        :description="t('templates.preview.noComparisonItems')"
      />
      <el-table v-else :data="comparisonItems" size="small" class="comparison-table">
        <el-table-column :label="t('templates.preview.locationType')" min-width="120">
          <template #default="{ row }">
            {{ locationLabel(row.locationType) }}
          </template>
        </el-table-column>
        <el-table-column prop="locationRef" :label="t('templates.preview.locationRef')" min-width="140" />
        <el-table-column :label="t('templates.preview.severity')" width="120">
          <template #default="{ row }">
            <el-tag :type="severityTagType(row.severity)" size="small">
              {{ t(`templates.preview.severities.${row.severity}`) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="diffCode" :label="t('templates.preview.diffCode')" min-width="140" />
        <el-table-column prop="summary" :label="t('templates.preview.diffSummary')" min-width="220" />
      </el-table>

      <h3>{{ t('templates.preview.warningsTitle') }}</h3>
      <FidelityWarningList
        v-if="latestPreview.fidelityWarnings.length"
        :warnings="latestPreview.fidelityWarnings"
        :artifact-hint="previewArtifact"
        :template-id="templateId"
        :dev-version-id="devVersionId"
        :marking-viewed-index="markingViewedIndex"
        @mark-viewed="markWarningViewed"
      />
      <el-empty
        v-else
        :description="t('templates.preview.noWarnings')"
      />
    </template>
    <el-empty v-else :description="t('templates.preview.empty')" />
  </div>
</template>

<style scoped lang="scss" src="./TemplatePreviewPanel.scss"></style>
