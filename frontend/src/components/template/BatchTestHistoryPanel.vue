<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import SectionPanelHeader from '@/components/common/SectionPanelHeader.vue'
import AppDataTable from '@/components/common/AppDataTable.vue'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { useTemplatePanelDataStore } from '@/stores/templatePanelData'
import type { BatchTestRunSummary } from '@/types/template'

const props = defineProps<{
  templateId: string
  refreshToken?: number
}>()

const { t } = useI18n()
const { formatDateTime } = useLocaleFormatters()
const panelDataStore = useTemplatePanelDataStore()
const entry = computed(() => panelDataStore.getEntry(props.templateId))
const loading = computed(() => entry.value.loadingBatchTestHistory)
const history = computed(() => entry.value.batchTestHistory)

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

    <AppDataTable v-else :data="history" class="batch-test-history__table">
      <el-table-column :label="t('templates.batchTestHistory.columns.runAt')" min-width="160">
        <template #default="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>
      </el-table-column>

      <el-table-column :label="t('templates.batchTestHistory.columns.runBy')" min-width="120">
        <template #default="{ row }">
          {{ row.createdBy }}
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
}

.coverage-summary {
  font-size: 0.8125rem;
  color: var(--text-muted);
}
</style>
