<script setup lang="ts">
import { reactive, toRef } from 'vue'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import SectionPanelHeader from '@/components/common/SectionPanelHeader.vue'
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import PreviewProgressDialog from '@/components/templates/PreviewProgressDialog.vue'
import TemplateTestDataSetEditDialog from '@/components/templates/TemplateTestDataSetEditDialog.vue'
import { useTemplateTestDataSetPanel } from '@/components/templates/useTemplateTestDataSetPanel'
import type { TestDataSet, VariableSchema } from '@/types/template'

const props = defineProps<{
  templateId: string
  variables?: VariableSchema[]
  generatingPreviewId?: string | null
  refreshToken?: number
  selectedTestDataSetId?: string | null
}>()

const emit = defineEmits<{
  selected: [testDataSetId: string | null]
  'test-generate': [testDataSetId: string]
  loaded: [count: number]
}>()

const api = reactive(
  useTemplateTestDataSetPanel({
    templateId: toRef(props, 'templateId'),
    refreshToken: toRef(props, 'refreshToken'),
    selectedTestDataSetId: toRef(props, 'selectedTestDataSetId'),
    variables: () => props.variables ?? [],
    emitSelected: (id) => emit('selected', id),
    emitLoaded: (count) => emit('loaded', count),
  }),
)

defineExpose({ reload: api.loadDataSets, dataSets: api.dataSets })
</script>

<template>
  <div class="test-data-set-panel">
    <SectionPanelHeader
      :title="api.t('templates.testDataSets.title')"
      :help-title="api.t('templates.testDataSets.helpTitle')"
      :help-content="api.t('templates.testDataSets.helpContent')"
    >
      <template #actions>
        <el-button type="primary" @click="api.openCreateDialog">
          {{ api.t('templates.testDataSets.create') }}
        </el-button>
      </template>
    </SectionPanelHeader>

    <AppDataTable
      v-loading="api.loading"
      :data="api.paginatedDataSets"
      highlight-current-row
      :row-class-name="api.rowClassName"
      :empty-text="api.t('templates.testDataSets.empty')"
      @row-click="(row: TestDataSet) => api.handleSelect(row.testDataSetId)"
    >
      <el-table-column prop="name" sortable min-width="160">
        <template #header>
          <TableColumnHeader :label="api.t('templates.testDataSets.name')" v-model="api.columnFilters.name" />
        </template>
        <template #default="{ row }">
          <span>{{ row.name }}</span>
          <el-tag v-if="row.required" size="small" type="warning" class="meta-tag">
            {{ api.t('templates.testDataSets.requiredTag') }}
          </el-tag>
          <el-tag v-if="row.locked" size="small" type="info" class="meta-tag">
            {{ api.t('templates.testDataSets.locked') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="scenarioName" min-width="140">
        <template #header>{{ api.t('templates.testDataSets.scenarioName') }}</template>
        <template #default="{ row }">{{ row.scenarioName || api.t('common.emptyValue') }}</template>
      </el-table-column>
      <el-table-column sortable :sort-method="api.sortByUpdatedAt" min-width="160">
        <template #header>
          <TableColumnHeader :label="api.t('templates.testDataSets.updatedAt')" v-model="api.columnFilters.updatedAt" />
        </template>
        <template #default="{ row }">{{ api.formatDateTime(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column :label="api.t('common.actions')" min-width="280" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :loading="generatingPreviewId === row.testDataSetId" @click.stop="api.handleRunPreview(row)">
            {{ api.t('templates.testDataSets.runPreview') }}
          </el-button>
          <el-button link type="primary" :disabled="row.locked" @click.stop="api.openEditDialog(row)">
            {{ api.t('templates.testDataSets.edit') }}
          </el-button>
          <el-button v-if="row.locked" link type="primary" @click.stop="api.handleDerive(row.testDataSetId)">
            {{ api.t('templates.testDataSets.derive') }}
          </el-button>
          <el-button link type="danger" :disabled="row.locked" @click.stop="api.handleDelete(row.testDataSetId)">
            {{ api.t('templates.testDataSets.delete') }}
          </el-button>
        </template>
      </el-table-column>
    </AppDataTable>
    <AppTablePagination v-model:current-page="api.dataSetsCurrentPage" :page-size="api.CLIENT_TABLE_PAGE_SIZE" :total="api.totalDataSetRows" />

    <TemplateTestDataSetEditDialog
      v-model="api.dialogVisible"
      v-model:coverage-tags-text="api.coverageTagsText"
      :editing-id="api.editingId"
      :saving="api.saving"
      :form="api.form"
      :variables="variables ?? []"
      :initial-variables="api.initialVariables"
      :server-field-errors="api.serverFieldErrors"
      @save="api.handleSave"
      @clear-server-errors="api.clearServerErrors"
    />
    <PreviewProgressDialog
      v-model="api.previewDialogVisible"
      :template-id="templateId"
      :preview-id="api.previewDialogPreviewId"
      :stream-url="api.previewDialogStreamUrl"
      :data-set-name="api.previewDialogDataSetName"
      @retry="api.handlePreviewRetry"
    />
  </div>
</template>

<style scoped lang="scss" src="./TemplateTestDataSetPanel.scss"></style>
