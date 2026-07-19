<script setup lang="ts">
import AppDataTable from '@/components/common/AppDataTable.vue'
import SectionPanelHeader from '@/components/common/SectionPanelHeader.vue'
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import RenderedCompareDialog from '@/components/templates/RenderedCompareDialog.vue'
import { useTemplatePreviewRunHistoryPanelFromProps } from '@/components/templates/useTemplatePreviewRunHistoryPanel'

const props = defineProps<{
  templateId: string
  refreshToken?: number
  selectedPreviewId: string | null
}>()

const emit = defineEmits<{
  selected: [previewId: string | null]
}>()

const {
  t,
  formatDateTime,
  downloadingKey,
  loading,
  filteredRuns,
  sortByCreatedAt,
  loadRuns,
  downloadArtifact,
  selectRow,
  onSelectionChange,
  canCompareRendered,
  compareHintKey,
  compareRunA,
  compareRunB,
  compareDialogVisible,
  openRenderedCompare,
  statusTagType,
  rowClassName,
} = useTemplatePreviewRunHistoryPanelFromProps(props, (id) => emit('selected', id))

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

    <div class="preview-run-history__toolbar">
      <el-button
        type="primary"
        size="small"
        data-testid="compare-rendered-outputs"
        :disabled="!canCompareRendered"
        @click="openRenderedCompare"
      >
        {{ t('templates.previewHistory.renderedCompare.action') }}
      </el-button>
      <span class="preview-run-history__compare-hint" data-testid="compare-rendered-outputs-hint">
        {{ t(compareHintKey) }}
      </span>
    </div>

    <el-empty v-if="!filteredRuns.length" :description="t('templates.previewHistory.empty')" />

    <AppDataTable
      v-else
      :data="filteredRuns"
      :row-class-name="rowClassName"
      highlight-current-row
      class="preview-run-history__table"
      @row-click="selectRow"
      @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="48" />

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

    <RenderedCompareDialog
      v-model="compareDialogVisible"
      :template-id="templateId"
      :run-a="compareRunA"
      :run-b="compareRunB"
    />
  </section>
</template>

<style scoped lang="scss" src="./TemplatePreviewRunHistoryPanel.scss"></style>
