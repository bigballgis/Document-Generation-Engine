<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import SectionPanelHeader from '@/components/common/SectionPanelHeader.vue'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import { useCatalogPagination } from '@/composables/useCatalogPagination'
import { CLIENT_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import { useTemplatePanelDataStore } from '@/stores/templatePanelData'
import { buildDevWorkspaceQuery } from '@/views/templates/templateDevWorkspaceTabs'
import { ElMessage } from 'element-plus'

const props = withDefaults(
  defineProps<{
    templateId: string
    refreshToken?: number
    compact?: boolean
  }>(),
  {
    compact: false,
  },
)

const { t, te } = useI18n()
const route = useRoute()
const router = useRouter()
const panelDataStore = useTemplatePanelDataStore()
const entry = computed(() => panelDataStore.getEntry(props.templateId))
const loading = computed(() => entry.value.loadingCoverage)
const summary = computed(() => entry.value.coverage)
const eligibility = computed(() => entry.value.submitTestEligibility)
const loadError = ref(false)

const dimensionLabelKey = computed(() => ({
  REQUIRED_VARIABLES: 'templates.coverage.dimensions.requiredVariables',
  REQUIRED_SAMPLES: 'templates.coverage.dimensions.requiredSamples',
  ANCHOR_BINDINGS: 'templates.coverage.dimensions.anchorBindings',
}))

const dimensionsSource = computed(() => summary.value?.dimensions ?? [])
const dimensionsCurrentPage = ref(1)
const { paginatedRows: paginatedDimensions, totalRows: totalDimensionRows } = useCatalogPagination(
  dimensionsSource,
  dimensionsCurrentPage,
  CLIENT_TABLE_PAGE_SIZE,
)

const uncoveredAnchors = computed(() => eligibility.value?.uncoveredAnchors ?? [])
const uncoveredVariables = computed(() => eligibility.value?.uncoveredVariables ?? [])
const hasUncoveredAnchors = computed(() => uncoveredAnchors.value.length > 0)
const hasUncoveredVariables = computed(() => uncoveredVariables.value.length > 0)
const uncoveredCollapseActive = computed(() => {
  const names: string[] = []
  if (hasUncoveredAnchors.value) names.push('anchors')
  if (hasUncoveredVariables.value) names.push('variables')
  return names
})

async function loadCoverage() {
  loadError.value = false
  const result = await panelDataStore.fetchCoverage(props.templateId)
  if (!result.coverage) {
    loadError.value = true
    ElMessage.error(t('templates.coverage.error.load'))
  }
}

function dimensionLabel(code: string): string {
  const key = dimensionLabelKey.value[code as keyof typeof dimensionLabelKey.value]
  return key ? t(key) : code
}

function scopeTypeLabel(scopeType: string): string {
  const key = `templates.coverage.scopeType.${scopeType}`
  return te(key) ? t(key) : scopeType
}

function navigateToBinding(anchorId: string) {
  void router.replace({
    query: {
      ...buildDevWorkspaceQuery(route.query, 'design', 'bindings'),
      anchorId,
    },
  })
}

function navigateToVariable(variableKey: string) {
  void router.replace({
    query: {
      ...buildDevWorkspaceQuery(route.query, 'design', 'variables'),
      variableKey,
    },
  })
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
    <SectionPanelHeader
      v-if="!compact"
      :title="t('templates.coverage.title')"
      :help-title="t('templates.coverage.helpTitle')"
      :help-content="t('templates.coverage.helpContent')"
    >
      <template #actions>
        <el-button link type="primary" @click="loadCoverage">
          {{ t('templates.coverage.refresh') }}
        </el-button>
      </template>
    </SectionPanelHeader>
    <div v-else class="coverage-panel__toolbar">
      <el-button link type="primary" @click="loadCoverage">
        {{ t('templates.coverage.refresh') }}
      </el-button>
    </div>

    <div v-if="loadError && !summary" data-testid="coverage-load-error">
      <LoadErrorPanel message-key="templates.coverage.error.load" @retry="loadCoverage" />
    </div>

    <template v-else-if="summary">
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

      <p class="threshold-hint" :class="{ 'threshold-hint--compact': compact }">
        {{
          t('templates.coverage.thresholdHint', {
            scope: scopeTypeLabel(summary.appliedThreshold.scopeType),
            variablePct: summary.appliedThreshold.minRequiredVariablePct,
            samplePct: summary.appliedThreshold.minRequiredSamplePct,
            anchorPct: summary.appliedThreshold.minAnchorBindingPct,
          })
        }}
      </p>

      <AppDataTable :data="paginatedDimensions" class="coverage-table">
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
      </AppDataTable>
      <AppTablePagination
        v-model:current-page="dimensionsCurrentPage"
        :page-size="CLIENT_TABLE_PAGE_SIZE"
        :total="totalDimensionRows"
      />

      <div v-if="hasUncoveredAnchors || hasUncoveredVariables" class="coverage-uncovered">
        <el-collapse :model-value="uncoveredCollapseActive">
          <el-collapse-item
            v-if="hasUncoveredAnchors"
            :title="t('templates.coverage.uncoveredAnchors.title') + ` (${uncoveredAnchors.length})`"
            name="anchors"
          >
            <ul class="coverage-uncovered__list">
              <li v-for="anchor in uncoveredAnchors" :key="anchor" class="coverage-uncovered__item">
                <el-button
                  link
                  type="primary"
                  data-testid="coverage-uncovered-anchor-link"
                  @click="navigateToBinding(anchor)"
                >
                  {{ anchor }}
                </el-button>
              </li>
            </ul>
          </el-collapse-item>

          <el-collapse-item
            v-if="hasUncoveredVariables"
            :title="t('templates.coverage.uncoveredVariables.title') + ` (${uncoveredVariables.length})`"
            name="variables"
          >
            <ul class="coverage-uncovered__list">
              <li v-for="varKey in uncoveredVariables" :key="varKey" class="coverage-uncovered__item">
                <el-button
                  link
                  type="primary"
                  data-testid="coverage-uncovered-variable-link"
                  @click="navigateToVariable(varKey)"
                >
                  {{ varKey }}
                </el-button>
              </li>
            </ul>
          </el-collapse-item>
        </el-collapse>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss" src="./TemplateCoveragePanel.scss"></style>
