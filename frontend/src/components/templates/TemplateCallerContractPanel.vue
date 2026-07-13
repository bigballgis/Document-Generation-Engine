<script setup lang="ts">
import { toRef } from 'vue'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import { useTemplateCallerContractPanel } from '@/components/templates/useTemplateCallerContractPanel'
import type { RuntimeEnvironment } from '@/config/environments'

const props = defineProps<{
  templateId: string
  environment?: RuntimeEnvironment
}>()
const emit = defineEmits<{
  'update:environment': [environment: RuntimeEnvironment]
}>()

const {
  t,
  CLIENT_TABLE_PAGE_SIZE,
  loading,
  errorMessageKey,
  contract,
  selectedEnvironment,
  environmentOptions,
  versionColumnFilters,
  paginatedVersionComparison,
  versionComparisonCurrentPage,
  totalVersionComparisonRows,
  yesNoFilterOptions,
  errorColumnFilters,
  paginatedErrorCodes,
  errorCodesCurrentPage,
  totalErrorCodeRows,
  errorMessage,
} = useTemplateCallerContractPanel({
  templateId: toRef(props, 'templateId'),
  environment: toRef(props, 'environment'),
  emitEnvironment: (environment) => emit('update:environment', environment),
})
</script>

<template>
  <div class="caller-contract-panel">
    <el-skeleton v-if="loading" :rows="6" animated />

    <el-alert
      v-else-if="errorMessageKey"
      type="error"
      :title="errorMessage(errorMessageKey)"
      show-icon
      :closable="false"
    />

    <template v-else-if="contract">
      <div class="toolbar">
        <el-form-item :label="t('templates.contract.environment')">
          <AppSearchSelect v-model="selectedEnvironment" class="environment-select">
            <el-option
              v-for="option in environmentOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </AppSearchSelect>
        </el-form-item>
      </div>

      <p class="panel-description">{{ t('templates.contract.description') }}</p>

      <h3>{{ t('templates.contract.sections.paths') }}</h3>
      <ul class="path-list">
        <li v-for="path in contract.paths" :key="path">
          <code>{{ path }}</code>
        </li>
      </ul>

      <h3>{{ t('templates.contract.sections.versions') }}</h3>
      <AppDataTable :data="paginatedVersionComparison">
        <el-table-column prop="releaseVersion" sortable>
          <template #header>
            <TableColumnHeader
              :label="t('templates.contract.columns.releaseVersion')"
              v-model="versionColumnFilters.releaseVersion"
            />
          </template>
        </el-table-column>
        <el-table-column prop="explicitVersionUrl" sortable min-width="280">
          <template #header>
            <TableColumnHeader
              :label="t('templates.contract.columns.generateUrl')"
              v-model="versionColumnFilters.explicitVersionUrl"
            />
          </template>
          <template #default="{ row }">
            <code>{{ row.explicitVersionUrl }}</code>
          </template>
        </el-table-column>
        <el-table-column width="160">
          <template #header>
            <TableColumnHeader
              :label="t('templates.contract.columns.defaultRouteTarget')"
              v-model="versionColumnFilters.defaultRoute"
              filter-type="select"
              :options="yesNoFilterOptions"
            />
          </template>
          <template #default="{ row }">
            <el-tag v-if="row.isDefaultRouteTarget" type="success" size="small">
              {{ t('templates.contract.defaultRouteTargetYes') }}
            </el-tag>
            <span v-else>{{ t('templates.contract.defaultRouteTargetNo') }}</span>
          </template>
        </el-table-column>
      </AppDataTable>
      <AppTablePagination
        v-model:current-page="versionComparisonCurrentPage"
        :page-size="CLIENT_TABLE_PAGE_SIZE"
        :total="totalVersionComparisonRows"
      />

      <h3>{{ t('templates.contract.sections.policy') }}</h3>
      <dl class="summary-grid">
        <div>
          <dt>{{ t('templates.contract.fields.policyVersion') }}</dt>
          <dd>{{ contract.apiPolicy.policyVersion }}</dd>
        </div>
        <div>
          <dt>{{ t('templates.contract.fields.outputFormats') }}</dt>
          <dd>{{ contract.apiPolicy.allowedOutputFormats.join(', ') }}</dd>
        </div>
        <div>
          <dt>{{ t('templates.contract.fields.outputModes') }}</dt>
          <dd>{{ contract.apiPolicy.allowedOutputModes.join(', ') }}</dd>
        </div>
        <div>
          <dt>{{ t('templates.contract.fields.batchLimits') }}</dt>
          <dd>{{ contract.apiPolicy.batchLimits.syncMaxItems }}</dd>
        </div>
      </dl>

      <h3>{{ t('templates.contract.sections.errorCodes') }}</h3>
      <AppDataTable :data="paginatedErrorCodes">
        <el-table-column prop="code" sortable width="240">
          <template #header>
            <TableColumnHeader
              :label="t('templates.contract.columns.errorCode')"
              v-model="errorColumnFilters.code"
            />
          </template>
        </el-table-column>
        <el-table-column prop="category" sortable width="140">
          <template #header>
            <TableColumnHeader
              :label="t('templates.contract.columns.category')"
              v-model="errorColumnFilters.category"
            />
          </template>
        </el-table-column>
        <el-table-column prop="message" sortable min-width="240">
          <template #header>
            <TableColumnHeader
              :label="t('templates.contract.columns.message')"
              v-model="errorColumnFilters.message"
            />
          </template>
        </el-table-column>
        <el-table-column width="120">
          <template #header>
            <TableColumnHeader
              :label="t('templates.contract.columns.retryable')"
              v-model="errorColumnFilters.retryable"
              filter-type="select"
              :options="yesNoFilterOptions"
            />
          </template>
          <template #default="{ row }">
            {{ row.retryable ? t('common.yes') : t('common.no') }}
          </template>
        </el-table-column>
      </AppDataTable>
      <AppTablePagination
        v-model:current-page="errorCodesCurrentPage"
        :page-size="CLIENT_TABLE_PAGE_SIZE"
        :total="totalErrorCodeRows"
      />

      <h3>{{ t('templates.contract.sections.examples') }}</h3>
      <ul class="example-list">
        <li v-for="example in contract.examples" :key="example">
          <code>{{ example }}</code>
        </li>
      </ul>
    </template>
  </div>
</template>

<style scoped lang="scss" src="./TemplateCallerContractPanel.scss"></style>
