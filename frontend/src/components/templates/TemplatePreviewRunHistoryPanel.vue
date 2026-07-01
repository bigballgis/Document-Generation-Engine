<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import AppDataTable from '@/components/common/AppDataTable.vue'
import SectionPanelHeader from '@/components/common/SectionPanelHeader.vue'
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { rowSortMethod, useDataTableFilters } from '@/composables/useDataTableFilters'
import * as templatesApi from '@/api/templates'
import { downloadBlobExport } from '@/utils/downloadExport'
import type { PreviewRunSummary } from '@/types/template'

const props = defineProps<{
  templateId: string
  refreshToken?: number
  selectedPreviewId: string | null
}>()

const emit = defineEmits<{
  selected: [previewId: string | null]
}>()

const { t } = useI18n()
const { formatDateTime } = useLocaleFormatters()
const loading = ref(false)
const downloadingKey = ref<string | null>(null)
const runs = ref<PreviewRunSummary[]>([])
const runsSource = computed(() => runs.value)

const { filteredRows: filteredRuns } = useDataTableFilters(runsSource, [
  { key: 'previewId', getValue: (row) => row.previewId },
  { key: 'testDataSetId', getValue: (row) => row.testDataSetId ?? '' },
  { key: 'createdAt', getValue: (row) => formatDateTime(row.createdAt) },
])

const sortByCreatedAt = rowSortMethod<PreviewRunSummary>((row) => row.createdAt)

async function loadRuns() {
  loading.value = true
  try {
    runs.value = await templatesApi.listPreviewRuns(props.templateId)
    if (props.selectedPreviewId) {
      const stillExists = runs.value.some((row) => row.previewId === props.selectedPreviewId)
      if (!stillExists) {
        emit('selected', null)
      }
    }
  } catch {
    ElMessage.error(t('templates.previewHistory.error.load'))
  } finally {
    loading.value = false
  }
}

async function downloadArtifact(row: PreviewRunSummary, format: 'docx' | 'pdf') {
  const key = `${row.previewId}-${format}`
  downloadingKey.value = key
  try {
    const { blob, filename } = await templatesApi.downloadPreviewArtifact(
      props.templateId,
      row.previewId,
      format,
    )
    downloadBlobExport(filename, blob)
  } catch {
    ElMessage.error(t('templates.previewHistory.error.download'))
  } finally {
    downloadingKey.value = null
  }
}

function selectRow(row: PreviewRunSummary) {
  emit('selected', row.previewId)
}

function statusTagType(status: PreviewRunSummary['status']): 'success' | 'warning' | 'danger' | 'info' {
  if (status === 'SUCCEEDED') {
    return 'success'
  }
  if (status === 'FAILED') {
    return 'danger'
  }
  if (status === 'PROCESSING') {
    return 'warning'
  }
  return 'info'
}

function isSelected(row: PreviewRunSummary): boolean {
  return props.selectedPreviewId === row.previewId
}

function rowClassName({ row }: { row: PreviewRunSummary }) {
  return isSelected(row) ? 'preview-run-row is-selected' : 'preview-run-row'
}

onMounted(() => {
  void loadRuns()
})

watch(
  () => props.refreshToken,
  () => {
    void loadRuns()
  },
)

defineExpose({ reload: loadRuns })
</script>

<template>
  <section v-loading="loading" class="preview-run-history">
    <SectionPanelHeader
      :title="t('templates.previewHistory.title')"
      :help-title="t('templates.previewHistory.helpTitle')"
      :help-content="t('templates.previewHistory.helpContent')"
    >
      <template #actions>
        <el-button link type="primary" @click="loadRuns">
          {{ t('templates.previewHistory.refresh') }}
        </el-button>
      </template>
    </SectionPanelHeader>

    <el-empty v-if="!filteredRuns.length" :description="t('templates.previewHistory.empty')" />

    <AppDataTable
      v-else
      :data="filteredRuns"
      :row-class-name="rowClassName"
      highlight-current-row
      class="preview-run-history__table"
      @row-click="selectRow"
    >
      <el-table-column
        prop="createdAt"
        :label="t('templates.previewHistory.columns.runAt')"
        min-width="160"
        sortable
        :sort-method="sortByCreatedAt"
      >
        <template #header>
          <TableColumnHeader :label="t('templates.previewHistory.columns.runAt')" column-key="createdAt" />
        </template>
        <template #default="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>
      </el-table-column>

      <el-table-column :label="t('templates.previewHistory.columns.dataSet')" min-width="140">
        <template #header>
          <TableColumnHeader
            :label="t('templates.previewHistory.columns.dataSet')"
            column-key="testDataSetId"
          />
        </template>
        <template #default="{ row }">
          {{ row.testDataSetId ?? t('templates.previewHistory.adhocRun') }}
        </template>
      </el-table-column>

      <el-table-column :label="t('templates.previewHistory.columns.status')" width="120">
        <template #default="{ row }">
          <el-tag size="small" :type="statusTagType(row.status)">
            {{ t(`templates.previewHistory.status.${row.status}`) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column :label="t('templates.previewHistory.columns.warnings')" width="110">
        <template #default="{ row }">
          {{ row.fidelityWarningCount }}
        </template>
      </el-table-column>

      <el-table-column :label="t('templates.previewHistory.columns.actions')" min-width="220" fixed="right">
        <template #default="{ row }">
          <el-button-group>
            <el-button
              size="small"
              :disabled="!row.docxAvailable"
              :loading="downloadingKey === `${row.previewId}-docx`"
              @click.stop="downloadArtifact(row, 'docx')"
            >
              {{ t('templates.previewHistory.downloadDocx') }}
            </el-button>
            <el-button
              size="small"
              :disabled="!row.pdfAvailable"
              :loading="downloadingKey === `${row.previewId}-pdf`"
              @click.stop="downloadArtifact(row, 'pdf')"
            >
              {{ t('templates.previewHistory.downloadPdf') }}
            </el-button>
            <el-button size="small" type="primary" plain @click.stop="selectRow(row)">
              {{ t('templates.previewHistory.viewDetails') }}
            </el-button>
          </el-button-group>
        </template>
      </el-table-column>
    </AppDataTable>
  </section>
</template>

<style scoped lang="scss">
.preview-run-history {
  margin-bottom: 1rem;

  &__table {
    width: 100%;
  }

  :deep(.preview-run-row.is-selected > td) {
    background: var(--brand-accent-soft, #eef6ff);
  }
}
</style>
