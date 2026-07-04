<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import FidelityWarningList from '@/components/authoring/FidelityWarningList.vue'
import { useTemplatePanelDataStore } from '@/stores/templatePanelData'
import { downloadBlobExport } from '@/utils/downloadExport'
import type { AnchorBinding, PreviewComparisonItem, PreviewRecord } from '@/types/template'

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

const { t, te } = useI18n()
const panelDataStore = useTemplatePanelDataStore()
const loading = computed(() => {
  const previewId = latestPreview.value?.previewId
  if (!previewId) {
    return false
  }
  return panelDataStore.getEntry(props.templateId).loadingPreviewById[previewId] ?? false
})
const downloadingFormat = ref<'docx' | 'pdf' | null>(null)
const latestPreview = ref<PreviewRecord | null>(props.preview)

watch(
  () => props.preview,
  (value) => {
    latestPreview.value = value
  },
)

const comparisonItems = computed<PreviewComparisonItem[]>(
  () => latestPreview.value?.previewComparison?.items ?? [],
)

const comparisonSummary = computed(() => {
  const comparison = latestPreview.value?.previewComparison
  if (!comparison) {
    return null
  }
  return t('templates.preview.comparisonCounts', {
    total: comparison.totalDiffCount,
    blockers: comparison.blockerCount,
    warnings: comparison.warningCount,
  })
})

async function refreshPreview() {
  if (!latestPreview.value?.previewId) {
    return
  }
  try {
    latestPreview.value = await panelDataStore.fetchPreview(
      props.templateId,
      latestPreview.value.previewId,
    )
  } catch {
    ElMessage.error(t('templates.previewHistory.error.load'))
  }
}

function severityTagType(severity: string): 'danger' | 'warning' {
  return severity === 'BLOCKER' ? 'danger' : 'warning'
}

function locationLabel(locationType: string): string {
  const key = `templates.preview.locationTypes.${locationType}`
  return te(key) ? t(key) : locationType
}

const previewArtifact = computed(() => latestPreview.value?.artifactStorageKey ?? null)

const canDownloadDocx = computed(
  () => latestPreview.value?.status === 'SUCCEEDED' && Boolean(latestPreview.value?.artifactStorageKey),
)
const canDownloadPdf = computed(
  () =>
    latestPreview.value?.status === 'SUCCEEDED'
    && (Boolean(latestPreview.value?.pdfArtifactStorageKey) || Boolean(latestPreview.value?.artifactStorageKey)),
)

async function downloadArtifact(format: 'docx' | 'pdf') {
  if (!latestPreview.value?.previewId) {
    return
  }
  downloadingFormat.value = format
  try {
    const { blob, filename } = await panelDataStore.downloadPreviewArtifact(
      props.templateId,
      latestPreview.value.previewId,
      format,
    )
    downloadBlobExport(filename, blob)
  } catch {
    ElMessage.error(t('templates.previewHistory.error.download'))
  } finally {
    downloadingFormat.value = null
  }
}
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
      />
      <el-empty
        v-else
        :description="t('templates.preview.noWarnings')"
      />
    </template>
    <el-empty v-else :description="t('templates.preview.empty')" />
  </div>
</template>

<style scoped lang="scss">
.preview-meta {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1rem;
  margin: 0 0 1rem;

  dt {
    margin: 0;
    font-size: 0.85rem;
    color: var(--text-muted);
  }

  dd {
    margin: 0.25rem 0 0;
    font-weight: 500;
  }
}

h3 {
  margin: 1.25rem 0 0.75rem;
  font-size: 1rem;
}

.comparison-table {
  width: 100%;
}

.preview-downloads {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
}
</style>
