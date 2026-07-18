<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import SectionPanelHeader from '@/components/common/SectionPanelHeader.vue'
import AppDataTable from '@/components/common/AppDataTable.vue'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { useTemplatePanelDataStore } from '@/stores/templatePanelData'
import { formatUserDisplayLabel } from '@/utils/userDisplay'
import {
  matchTestDataSetBySampleId,
  normalizeSampleResults,
  type NormalizedBatchTestSampleResult,
} from '@/utils/batchTestSampleResults'
import * as templatesApi from '@/api/templates'
import type { BatchTestRunSummary } from '@/types/template'

const props = defineProps<{
  templateId: string
  refreshToken?: number
}>()

const emit = defineEmits<{
  'open-data-set': [
    payload: { dataSetExternalId: string; testDataSetId: string | null; matched: boolean },
  ]
  'open-preview': [payload: { previewId: string }]
}>()

const { t } = useI18n()
const { formatDateTime } = useLocaleFormatters()
const panelDataStore = useTemplatePanelDataStore()
const entry = computed(() => panelDataStore.getEntry(props.templateId))
const loading = computed(() => entry.value.loadingBatchTestHistory)
const history = computed(() => entry.value.batchTestHistory)

const ERROR_DETAIL_PREVIEW_LEN = 160

async function loadHistory() {
  try {
    await panelDataStore.fetchBatchTestHistory(props.templateId)
  } catch {
    ElMessage.error(t('templates.batchTestHistory.error.load'))
  }
}

onMounted(() => {
  void loadHistory()
})

watch(
  () => props.refreshToken,
  () => {
    void loadHistory()
  },
)

function effectiveStatus(row: BatchTestRunSummary): string {
  if (row.invalidatedAt) return 'INVALIDATED'
  return row.status
}

function statusTagType(row: BatchTestRunSummary): 'success' | 'warning' | 'danger' | 'info' {
  if (row.invalidatedAt) return 'info'
  if (row.status === 'COMPLETED' && row.gatePassed) return 'success'
  if (row.status === 'COMPLETED' && !row.gatePassed) return 'warning'
  if (row.status === 'FAILED') return 'danger'
  return 'info'
}

function gateTagType(row: BatchTestRunSummary): 'success' | 'warning' | 'info' {
  if (row.gatePassed === true) return 'success'
  if (row.gatePassed === false) return 'warning'
  return 'info'
}

function samplesForRow(row: BatchTestRunSummary): NormalizedBatchTestSampleResult[] {
  return normalizeSampleResults(row.sampleResults ?? [])
}

function sampleEmptyMessage(row: BatchTestRunSummary): string {
  if (row.status === 'RUNNING' && !row.invalidatedAt) {
    return t('templates.batchTestHistory.sampleResults.inProgress')
  }
  return t('templates.batchTestHistory.sampleResults.empty')
}

function truncatedError(detail: string | null): string {
  if (!detail) {
    return ''
  }
  if (detail.length <= ERROR_DETAIL_PREVIEW_LEN) {
    return detail
  }
  return `${detail.slice(0, ERROR_DETAIL_PREVIEW_LEN)}…`
}

async function handleOpenDataSet(sample: NormalizedBatchTestSampleResult) {
  let dataSets = entry.value.testDataSets
  if (dataSets.length === 0) {
    try {
      dataSets = await templatesApi.listTestDataSets(props.templateId)
    } catch {
      dataSets = []
    }
  }
  const match = matchTestDataSetBySampleId(dataSets, sample.dataSetExternalId)
  emit('open-data-set', {
    dataSetExternalId: sample.dataSetExternalId,
    testDataSetId: match?.testDataSetId ?? null,
    matched: match != null,
  })
}

function handleOpenPreview(sample: NormalizedBatchTestSampleResult) {
  if (!sample.previewId) {
    return
  }
  emit('open-preview', { previewId: sample.previewId })
}

defineExpose({ reload: loadHistory })
</script>

<template>
  <div v-loading="loading" class="batch-test-history">
    <SectionPanelHeader
      :title="t('templates.batchTestHistory.title')"
      :help-title="t('templates.batchTestHistory.helpTitle')"
      :help-content="t('templates.batchTestHistory.helpContent')"
    >
      <template #actions>
        <el-button link type="primary" @click="loadHistory">
          {{ t('templates.batchTestHistory.refresh') }}
        </el-button>
      </template>
    </SectionPanelHeader>

    <el-empty
      v-if="!loading && history.length === 0"
      :description="t('templates.batchTestHistory.empty')"
    />

    <AppDataTable v-else :data="history" row-key="runId" class="batch-test-history__table">
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="batch-test-history__samples" data-testid="batch-history-sample-results">
            <h3 class="batch-test-history__samples-title">
              {{ t('templates.batchTestHistory.sampleResults.title') }}
            </h3>

            <el-empty
              v-if="samplesForRow(row).length === 0"
              :description="sampleEmptyMessage(row)"
              :image-size="64"
            />

            <div v-else class="batch-test-history__samples-scroll">
              <table class="batch-test-history__samples-table">
                <thead>
                  <tr>
                    <th>{{ t('templates.batchTestHistory.sampleResults.dataSet') }}</th>
                    <th>{{ t('templates.batchTestHistory.sampleResults.status') }}</th>
                    <th>{{ t('templates.batchTestHistory.sampleResults.error') }}</th>
                    <th>{{ t('templates.batchTestHistory.sampleResults.actions') }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="sample in samplesForRow(row)" :key="sample.dataSetExternalId + (sample.previewId ?? '')">
                    <td>{{ sample.dataSetExternalId }}</td>
                    <td>
                      <el-tag :type="sample.success ? 'success' : 'danger'" size="small">
                        {{
                          sample.success
                            ? t('templates.batchTestHistory.sampleResults.succeeded')
                            : t('templates.batchTestHistory.sampleResults.failed')
                        }}
                      </el-tag>
                    </td>
                    <td>
                      <span
                        v-if="sample.errorDetail"
                        class="batch-test-history__error"
                        :title="sample.errorDetail"
                      >
                        {{ truncatedError(sample.errorDetail) }}
                      </span>
                      <span v-else>{{ t('common.emptyValue') }}</span>
                    </td>
                    <td class="batch-test-history__sample-actions">
                      <el-button
                        link
                        type="primary"
                        data-testid="batch-history-open-data-set"
                        @click="handleOpenDataSet(sample)"
                      >
                        {{ t('templates.batchTestHistory.sampleResults.openDataSet') }}
                      </el-button>
                      <el-button
                        v-if="sample.previewId"
                        link
                        type="primary"
                        data-testid="batch-history-open-preview"
                        @click="handleOpenPreview(sample)"
                      >
                        {{ t('templates.batchTestHistory.sampleResults.openPreview') }}
                      </el-button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </template>
      </el-table-column>

      <el-table-column :label="t('templates.batchTestHistory.columns.runAt')" min-width="160">
        <template #default="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>
      </el-table-column>

      <el-table-column :label="t('templates.batchTestHistory.columns.runBy')" min-width="120">
        <template #default="{ row }">
          {{ formatUserDisplayLabel(row.createdBy, row.createdByDisplayName) }}
        </template>
      </el-table-column>

      <el-table-column :label="t('templates.batchTestHistory.columns.status')" width="120">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row)" size="small">
            {{ t(`templates.batchTestHistory.status.${effectiveStatus(row)}`) }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column :label="t('templates.batchTestHistory.columns.counts')" width="120">
        <template #default="{ row }">
          {{
            t('templates.batchTestHistory.counts', {
              succeeded: row.succeededCount,
              total: row.totalSamples,
            })
          }}
        </template>
      </el-table-column>

      <el-table-column :label="t('templates.batchTestHistory.columns.readiness')" width="140">
        <template #default="{ row }">
          <template v-if="row.gatePassed !== null && !row.invalidatedAt">
            <el-tag :type="gateTagType(row)" size="small">
              {{
                row.gatePassed
                  ? t('templates.batchTestHistory.status.readinessPassed')
                  : t('templates.batchTestHistory.status.readinessFailed')
              }}
            </el-tag>
          </template>
          <span v-else>{{ t('common.emptyValue') }}</span>
        </template>
      </el-table-column>

      <el-table-column :label="t('templates.batchTestHistory.columns.coverage')" min-width="180">
        <template #default="{ row }">
          <span v-if="row.anchorCoveragePct !== null" class="coverage-summary">
            {{
              t('templates.batchTestHistory.coverageSummary', {
                binding: row.anchorCoveragePct,
                variable: row.variableCoveragePct,
                sample: row.sampleCoveragePct,
              })
            }}
          </span>
          <span v-else>{{ t('common.emptyValue') }}</span>
        </template>
      </el-table-column>
    </AppDataTable>
  </div>
</template>

<style scoped lang="scss">
.batch-test-history {
  &__table {
    width: 100%;
  }

  &__samples {
    padding: 0.5rem 1rem 1rem;
  }

  &__samples-title {
    margin: 0 0 0.75rem;
    font-size: 0.875rem;
    font-weight: 600;
    color: var(--text-primary);
  }

  &__samples-scroll {
    max-height: 16rem;
    overflow: auto;
  }

  &__samples-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 0.8125rem;

    th,
    td {
      padding: 0.5rem 0.75rem;
      text-align: left;
      border-bottom: 1px solid var(--border-color);
      vertical-align: top;
    }

    th {
      color: var(--text-muted);
      font-weight: 600;
    }
  }

  &__error {
    color: var(--text-muted);
    word-break: break-word;
  }

  &__sample-actions {
    white-space: nowrap;
  }
}

.coverage-summary {
  font-size: 0.8125rem;
  color: var(--text-muted);
}
</style>
