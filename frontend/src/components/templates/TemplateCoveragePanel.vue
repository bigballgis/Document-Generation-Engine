<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import * as templatesApi from '@/api/templates'
import type { CoverageSummary } from '@/types/template'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  templateId: string
  refreshToken?: number
}>()

const { t } = useI18n()
const loading = ref(false)
const summary = ref<CoverageSummary | null>(null)

const dimensionLabelKey = computed(() => ({
  REQUIRED_VARIABLES: 'templates.coverage.dimensions.requiredVariables',
  REQUIRED_SAMPLES: 'templates.coverage.dimensions.requiredSamples',
  ANCHOR_BINDINGS: 'templates.coverage.dimensions.anchorBindings',
}))

async function loadCoverage() {
  loading.value = true
  try {
    summary.value = await templatesApi.getTemplateCoverage(props.templateId)
  } catch {
    ElMessage.error(t('templates.coverage.error.load'))
  } finally {
    loading.value = false
  }
}

function dimensionLabel(code: string): string {
  const key = dimensionLabelKey.value[code as keyof typeof dimensionLabelKey.value]
  return key ? t(key) : code
}

onMounted(() => {
  void loadCoverage()
})

watch(
  () => props.refreshToken,
  () => {
    void loadCoverage()
  },
)
</script>

<template>
  <div v-loading="loading" class="coverage-panel">
    <div class="coverage-header">
      <h3>{{ t('templates.coverage.title') }}</h3>
      <el-button link type="primary" @click="loadCoverage">
        {{ t('templates.coverage.refresh') }}
      </el-button>
    </div>
    <p>{{ t('templates.coverage.description') }}</p>

    <template v-if="summary">
      <el-alert
        :type="summary.belowThreshold ? 'warning' : 'success'"
        :closable="false"
        show-icon
        class="coverage-alert"
      >
        <template #title>
          {{
            summary.belowThreshold
              ? t('templates.coverage.status.belowThreshold', { percentage: summary.aggregatePercentage })
              : t('templates.coverage.status.meetsThreshold', { percentage: summary.aggregatePercentage })
          }}
        </template>
      </el-alert>

      <p class="threshold-hint">
        {{
          t('templates.coverage.thresholdHint', {
            scope: summary.appliedThreshold.scopeType,
            variablePct: summary.appliedThreshold.minRequiredVariablePct,
            samplePct: summary.appliedThreshold.minRequiredSamplePct,
            anchorPct: summary.appliedThreshold.minAnchorBindingPct,
          })
        }}
      </p>

      <el-table :data="summary.dimensions" size="small" class="coverage-table">
        <el-table-column :label="t('templates.coverage.table.dimension')" min-width="180">
          <template #default="{ row }">
            {{ dimensionLabel(row.dimensionCode) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('templates.coverage.table.exercised')" width="120">
          <template #default="{ row }">
            {{ row.exercisedCount }} / {{ row.totalCount }}
          </template>
        </el-table-column>
        <el-table-column prop="percentage" :label="t('templates.coverage.table.percentage')" width="100">
          <template #default="{ row }">
            {{ row.percentage }}%
          </template>
        </el-table-column>
        <el-table-column prop="thresholdPercentage" :label="t('templates.coverage.table.threshold')" width="100">
          <template #default="{ row }">
            {{ row.thresholdPercentage }}%
          </template>
        </el-table-column>
        <el-table-column :label="t('templates.coverage.table.status')" width="120">
          <template #default="{ row }">
            <el-tag :type="row.belowThreshold ? 'warning' : 'success'" size="small">
              {{
                row.belowThreshold
                  ? t('templates.coverage.table.belowThreshold')
                  : t('templates.coverage.table.meetsThreshold')
              }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </template>
  </div>
</template>

<style scoped lang="scss">
.coverage-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.5rem;

  h3 {
    margin: 0;
  }
}

.coverage-alert {
  margin: 1rem 0;
}

.threshold-hint {
  margin: 0 0 1rem;
  color: var(--text-muted);
}

.coverage-table {
  width: 100%;
}
</style>
