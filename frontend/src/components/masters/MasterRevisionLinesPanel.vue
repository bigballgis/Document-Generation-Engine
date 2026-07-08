<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import MasterStatusBadge from '@/components/masters/MasterStatusBadge.vue'
import { useActivatableTableRow } from '@/composables/useActivatableTableRow'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { SERVER_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import { masterRevisionDetailPath } from '@/routing/routeKeys'
import * as mastersApi from '@/api/masters'
import type { MasterRevisionLineSummary } from '@/types/master'
import { formatMasterRevisionLineLabel } from '@/utils/masterRevisionLineLabel'
import { resolveUpdatedByDisplay } from '@/utils/userDisplay'

const props = defineProps<{
  masterId: string
}>()

const { t } = useI18n()
const { formatDateTime } = useLocaleFormatters()
const router = useRouter()

const loading = ref(false)
const loadError = ref(false)
const currentPage = ref(1)
const totalElements = ref(0)
const totalPages = ref(0)
const revisionLines = ref<MasterRevisionLineSummary[]>([])

const pageSize = SERVER_TABLE_PAGE_SIZE

function lineDisplayLabel(row: MasterRevisionLineSummary): string {
  return formatMasterRevisionLineLabel(t, row.lineLabel, row.revisionSequence)
}

async function loadRevisionLines() {
  loading.value = true
  loadError.value = false
  try {
    const page = await mastersApi.listMasterRevisionLines(
      props.masterId,
      currentPage.value - 1,
      pageSize,
    )
    revisionLines.value = page.content
    totalElements.value = page.totalElements
    totalPages.value = page.totalPages
  } catch {
    loadError.value = true
    revisionLines.value = []
    totalElements.value = 0
    totalPages.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadRevisionLines()
})

watch(
  () => props.masterId,
  () => {
    currentPage.value = 1
    void loadRevisionLines()
  },
)

watch(currentPage, () => {
  void loadRevisionLines()
})

function openRevisionDetail(row: MasterRevisionLineSummary) {
  router.push(masterRevisionDetailPath(props.masterId, row.id))
}

const { onRowClick } = useActivatableTableRow<MasterRevisionLineSummary>(openRevisionDetail)

const showPagination = computed(() => totalPages.value > 1)

defineExpose({
  reload: loadRevisionLines,
})
</script>

<template>
  <el-card shadow="never" class="revision-lines-card">
    <template #header>
      <div class="card-header">
        <span>{{ t('masters.revisionLines.title') }}</span>
        <p class="card-hint">{{ t('masters.revisionLines.hint') }}</p>
      </div>
    </template>

    <LoadErrorPanel
      v-if="loadError"
      message-key="masters.revisionLines.loadError"
      @retry="loadRevisionLines"
    />

    <el-skeleton v-else-if="loading" :rows="4" animated />

    <template v-else>
      <AppDataTable
        activatable
        :data="revisionLines"
        @row-click="onRowClick"
      >
        <template #empty>
          <el-empty :description="t('masters.revisionLines.empty')" />
        </template>
        <el-table-column min-width="180" :label="t('masters.revisionLines.line')">
          <template #default="{ row }">
            <span>{{ lineDisplayLabel(row) }}</span>
            <el-tag v-if="row.current" size="small" type="success" class="line-tag">
              {{ t('masters.revisionLines.currentBadge') }}
            </el-tag>
            <el-tag v-else size="small" type="info" class="line-tag">
              {{ t('masters.revisionLines.historicalBadge') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column min-width="140" :label="t('masters.revisionLines.status')">
          <template #default="{ row }">
            <MasterStatusBadge :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column
          prop="originalFilename"
          min-width="180"
          :label="t('masters.revisionLines.sourceFile')"
        />
        <el-table-column
          prop="anchorCount"
          width="100"
          :label="t('masters.revisionLines.anchors')"
        />
        <el-table-column min-width="170" :label="t('masters.revisionLines.updatedAt')">
          <template #default="{ row }">
            {{ formatDateTime(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column min-width="120" :label="t('masters.revisionLines.updatedBy')">
          <template #default="{ row }">
            {{ resolveUpdatedByDisplay(row.updatedBy, row.updatedByDisplayName) }}
          </template>
        </el-table-column>
        <el-table-column width="120" :label="t('masters.revisionLines.actions')">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="openRevisionDetail(row)">
              {{ t('masters.revisionLines.viewDetail') }}
            </el-button>
          </template>
        </el-table-column>
      </AppDataTable>

      <AppTablePagination
        v-if="showPagination"
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="totalElements"
      />
    </template>
  </el-card>
</template>

<style scoped lang="scss">
.revision-lines-card {
  margin-bottom: 1rem;
}

.card-header {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.card-hint {
  margin: 0;
  font-size: 0.875rem;
  color: var(--text-muted);
}

.line-tag {
  margin-left: 0.5rem;
}
</style>
