<script setup lang="ts">
import { toRef } from 'vue'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import SectionPanelHeader from '@/components/common/SectionPanelHeader.vue'
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import PreviewProgressDialog from '@/components/template/PreviewProgressDialog.vue'
import { useTemplateTestDataSetPanel } from '@/components/templates/useTemplateTestDataSetPanel'
import type { TestDataSet } from '@/types/template'

const props = defineProps<{
  templateId: string
  generatingPreviewId?: string | null
  refreshToken?: number
}>()

const emit = defineEmits<{
  selected: [testDataSetId: string | null]
  'test-generate': [testDataSetId: string]
  loaded: [count: number]
}>()

const {
  t,
  formatDateTime,
  CLIENT_TABLE_PAGE_SIZE,
  saving,
  loading,
  columnFilters,
  paginatedDataSets,
  totalDataSetRows,
  dataSetsCurrentPage,
  sortByUpdatedAt,
  dialogVisible,
  editingId,
  previewDialogVisible,
  previewDialogPreviewId,
  previewDialogStreamUrl,
  previewDialogDataSetName,
  form,
  variablesJson,
  coverageTagsText,
  loadDataSets,
  dataSets,
  openCreateDialog,
  openEditDialog,
  handleSave,
  handleDerive,
  handleDelete,
  handleSelect,
  handleRunPreview,
  handlePreviewRetry,
  rowClassName,
} = useTemplateTestDataSetPanel({
  templateId: toRef(props, 'templateId'),
  refreshToken: toRef(props, 'refreshToken'),
  emitSelected: (id) => emit('selected', id),
  emitLoaded: (count) => emit('loaded', count),
})

defineExpose({ reload: loadDataSets, dataSets })
</script>

<template>
  <div class="test-data-set-panel">
    <SectionPanelHeader
      :title="t('templates.testDataSets.title')"
      :help-title="t('templates.testDataSets.helpTitle')"
      :help-content="t('templates.testDataSets.helpContent')"
    >
      <template #actions>
        <el-button type="primary" @click="openCreateDialog">
          {{ t('templates.testDataSets.create') }}
        </el-button>
      </template>
    </SectionPanelHeader>

    <AppDataTable
      v-loading="loading"
      :data="paginatedDataSets"
      highlight-current-row
      :row-class-name="rowClassName"
      :empty-text="t('templates.testDataSets.empty')"
      @row-click="(row: TestDataSet) => handleSelect(row.testDataSetId)"
    >
      <el-table-column prop="name" sortable min-width="160">
        <template #header>
          <TableColumnHeader
            :label="t('templates.testDataSets.name')"
            v-model="columnFilters.name"
          />
        </template>
        <template #default="{ row }">
          <span>{{ row.name }}</span>
          <el-tag v-if="row.required" size="small" type="warning" class="meta-tag">
            {{ t('templates.testDataSets.requiredTag') }}
          </el-tag>
          <el-tag v-if="row.locked" size="small" type="info" class="meta-tag">
            {{ t('templates.testDataSets.locked') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="scenarioName" min-width="140">
        <template #header>
          {{ t('templates.testDataSets.scenarioName') }}
        </template>
        <template #default="{ row }">
          {{ row.scenarioName || t('common.emptyValue') }}
        </template>
      </el-table-column>
      <el-table-column sortable :sort-method="sortByUpdatedAt" min-width="160">
        <template #header>
          <TableColumnHeader
            :label="t('templates.testDataSets.updatedAt')"
            v-model="columnFilters.updatedAt"
          />
        </template>
        <template #default="{ row }">
          {{ formatDateTime(row.updatedAt) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('common.actions')" min-width="280" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            :loading="generatingPreviewId === row.testDataSetId"
            @click.stop="handleRunPreview(row)"
          >
            {{ t('templates.testDataSets.runPreview') }}
          </el-button>
          <el-button link type="primary" :disabled="row.locked" @click.stop="openEditDialog(row)">
            {{ t('templates.testDataSets.edit') }}
          </el-button>
          <el-button
            v-if="row.locked"
            link
            type="primary"
            @click.stop="handleDerive(row.testDataSetId)"
          >
            {{ t('templates.testDataSets.derive') }}
          </el-button>
          <el-button
            link
            type="danger"
            :disabled="row.locked"
            @click.stop="handleDelete(row.testDataSetId)"
          >
            {{ t('templates.testDataSets.delete') }}
          </el-button>
        </template>
      </el-table-column>
    </AppDataTable>
    <AppTablePagination
      v-model:current-page="dataSetsCurrentPage"
      :page-size="CLIENT_TABLE_PAGE_SIZE"
      :total="totalDataSetRows"
    />

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? t('templates.testDataSets.editTitle') : t('templates.testDataSets.createTitle')"
      width="520px"
    >
      <el-form label-position="top">
        <el-form-item :label="t('templates.testDataSets.name')">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item :label="t('templates.testDataSets.descriptionLabel')">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item :label="t('templates.testDataSets.scenarioName')">
          <el-input v-model="form.scenarioName" />
        </el-form-item>
        <el-form-item :label="t('templates.testDataSets.coverageTags')">
          <el-input v-model="coverageTagsText" />
        </el-form-item>
        <el-form-item :label="t('templates.testDataSets.required')">
          <el-switch v-model="form.required" />
        </el-form-item>
        <el-form-item :label="t('templates.testDataSets.variablesJson')">
          <el-input v-model="variablesJson" type="textarea" :rows="8" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('templates.testDataSets.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">
          {{ t('templates.testDataSets.save') }}
        </el-button>
      </template>
    </el-dialog>
    <PreviewProgressDialog
      v-model="previewDialogVisible"
      :template-id="templateId"
      :preview-id="previewDialogPreviewId"
      :stream-url="previewDialogStreamUrl"
      :data-set-name="previewDialogDataSetName"
      @retry="handlePreviewRetry"
    />
  </div>
</template>

<style scoped lang="scss">
.meta-tag {
  margin-left: 0.35rem;
}

:deep(.is-selected-row) {
  background-color: var(--el-color-primary-light-9);
}
</style>
