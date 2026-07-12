<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import SectionPanelHeader from '@/components/common/SectionPanelHeader.vue'
import AppDataTable from '@/components/common/AppDataTable.vue'
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import type { MasterAnchorBindingRow } from '@/utils/masterAnchorBindingRows'
import type { BindingValidationResult } from '@/types/template'

defineProps<{
  loadingMaster: boolean
  validating: boolean
  configuredBindingCount: number
  filteredAnchorRows: MasterAnchorBindingRow[]
  filterAnchorId: string
  filterDisplayLabel: string
  filterDeclaredContentType: string
  filterValidationStatus: string
  contentTypeFilterOptions: Array<{ value: string; label: string }>
  validationResult: BindingValidationResult | null
  resolveValidationStatusLabel: (status: string | undefined | null) => string
  resolveConfiguredLabel: (row: MasterAnchorBindingRow) => string
  bindingHasPasteBlockers: (row: MasterAnchorBindingRow) => boolean
}>()

const emit = defineEmits<{
  validate: []
  edit: [row: MasterAnchorBindingRow]
  'update:filterAnchorId': [value: string]
  'update:filterDisplayLabel': [value: string]
  'update:filterDeclaredContentType': [value: string]
  'update:filterValidationStatus': [value: string]
}>()

const { t } = useI18n()
</script>

<template>
  <div>
    <SectionPanelHeader
      :title="t('templates.authoring.bindingsTitle')"
      :help-title="t('templates.authoring.bindingsHelpTitle')"
      :help-content="t('templates.authoring.bindingsHelpDescription')"
    >
      <template #actions>
        <el-button
          type="primary"
          :loading="validating"
          :disabled="configuredBindingCount === 0"
          @click="emit('validate')"
        >
          {{ t('templates.authoring.validateBindings') }}
        </el-button>
      </template>
    </SectionPanelHeader>

    <AppDataTable v-loading="loadingMaster" :data="filteredAnchorRows" empty-text="">
      <template #empty>
        <el-empty :description="t('templates.authoring.noMasterAnchors')" />
      </template>

      <el-table-column prop="anchorId" width="160">
        <template #header>
          <TableColumnHeader
            :label="t('templates.authoring.anchorId')"
            :model-value="filterAnchorId"
            @update:model-value="emit('update:filterAnchorId', $event)"
          />
        </template>
      </el-table-column>

      <el-table-column prop="displayLabel" min-width="200">
        <template #header>
          <TableColumnHeader
            :label="t('templates.authoring.anchorDisplayLabel')"
            :model-value="filterDisplayLabel"
            @update:model-value="emit('update:filterDisplayLabel', $event)"
          />
        </template>
      </el-table-column>

      <el-table-column prop="declaredContentType" width="140">
        <template #header>
          <TableColumnHeader
            :label="t('templates.authoring.contentType')"
            :model-value="filterDeclaredContentType"
            filter-type="select"
            :options="contentTypeFilterOptions"
            @update:model-value="emit('update:filterDeclaredContentType', $event)"
          />
        </template>
        <template #default="{ row }">
          {{ row.declaredContentType ?? '—' }}
        </template>
      </el-table-column>

      <el-table-column prop="validationStatus" width="140">
        <template #header>
          <TableColumnHeader
            :label="t('templates.authoring.validationStatus')"
            :model-value="filterValidationStatus"
            @update:model-value="emit('update:filterValidationStatus', $event)"
          />
        </template>
        <template #default="{ row }">
          {{ row.configured ? resolveValidationStatusLabel(row.validationStatus) : '—' }}
        </template>
      </el-table-column>

      <el-table-column width="120">
        <template #header>
          <span>{{ t('templates.authoring.bindingStatus') }}</span>
        </template>
        <template #default="{ row }">
          <div class="binding-status-cell">
            <el-tag :type="row.configured ? 'success' : 'info'" size="small">
              {{ resolveConfiguredLabel(row) }}
            </el-tag>
            <el-tag
              v-if="bindingHasPasteBlockers(row)"
              type="danger"
              size="small"
              data-testid="binding-paste-residue-tag"
            >
              {{ t('templates.authoring.pasteResidue.blockedTag') }}
            </el-tag>
          </div>
        </template>
      </el-table-column>

      <el-table-column width="120" fixed="right" :label="t('common.actions')">
        <template #default="{ row }">
          <el-button link type="primary" @click="emit('edit', row)">
            {{ row.configured ? t('common.edit') : t('templates.authoring.configureBinding') }}
          </el-button>
        </template>
      </el-table-column>
    </AppDataTable>

    <el-alert
      v-if="validationResult"
      class="validation-summary"
      :type="validationResult.summary.blocking ? 'warning' : 'success'"
      :closable="false"
      show-icon
      :title="
        t('templates.authoring.bindingValidationSummary', {
          valid: validationResult.summary.validCount,
          total: validationResult.summary.totalBindings,
        })
      "
    />
  </div>
</template>

<style scoped lang="scss">
.validation-summary {
  margin-top: 1rem;
}

.binding-status-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  align-items: center;
}
</style>
