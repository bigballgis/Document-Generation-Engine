<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { DocumentCopy } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { listInvocations } from '@/api/apiPolicy'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import InvocationSummaryDrawer from '@/components/templates/InvocationSummaryDrawer.vue'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { SERVER_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import type { ManagementInvocationSummary } from '@/types/template'

const props = defineProps<{
  templateId: string
}>()

const { t } = useI18n()
const { formatDateTime } = useLocaleFormatters()

const pageSize = SERVER_TABLE_PAGE_SIZE
const currentPage = ref(1)
const totalElements = ref(0)
const loading = ref(true)
const loadFailed = ref(false)
const rows = ref<ManagementInvocationSummary[]>([])

const filterDraft = reactive({
  status: '',
  invocationKind: '',
  requestId: '',
})

const appliedFilters = reactive({
  status: '',
  invocationKind: '',
  requestId: '',
})

const drawerVisible = ref(false)
const selectedInvocationId = ref<string | null>(null)

const statusFilterOptions = computed(() =>
  ['SUCCEEDED', 'FAILED', 'PARTIAL_SUCCEEDED', 'PROCESSING', 'ACCEPTED', 'EXPIRED', 'CANCELLED'].map(
    (value) => ({ label: value, value }),
  ),
)

const kindFilterOptions = computed(() =>
  ['SINGLE', 'BATCH_ROOT', 'ASYNC_TASK'].map((value) => ({ label: value, value })),
)

const uiPage = computed({
  get: () => currentPage.value,
  set: (page: number) => {
    currentPage.value = page
    void loadInvocations()
  },
})

async function loadInvocations() {
  loading.value = true
  loadFailed.value = false
  try {
    const result = await listInvocations(
      props.templateId,
      currentPage.value - 1,
      pageSize,
      {
        status: appliedFilters.status || undefined,
        invocationKind: appliedFilters.invocationKind || undefined,
        requestId: appliedFilters.requestId || undefined,
      },
    )
    rows.value = result.content
    totalElements.value = result.totalElements
  } catch {
    rows.value = []
    totalElements.value = 0
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  appliedFilters.status = filterDraft.status
  appliedFilters.invocationKind = filterDraft.invocationKind
  appliedFilters.requestId = filterDraft.requestId
  currentPage.value = 1
  void loadInvocations()
}

function clearFilters() {
  filterDraft.status = ''
  filterDraft.invocationKind = ''
  filterDraft.requestId = ''
  appliedFilters.status = ''
  appliedFilters.invocationKind = ''
  appliedFilters.requestId = ''
  currentPage.value = 1
  void loadInvocations()
}

function openInvocationSummary(row: ManagementInvocationSummary) {
  selectedInvocationId.value = row.invocationId
  drawerVisible.value = true
}

async function copyTechnicalId(value: string) {
  if (!value) {
    return
  }
  try {
    await navigator.clipboard.writeText(value)
    ElMessage.success(t('common.copyToClipboardSuccess'))
  } catch {
    ElMessage.error(t('common.copyToClipboardError'))
  }
}

onMounted(() => {
  void loadInvocations()
})
</script>

<template>
  <el-card shadow="never" class="section-card">
    <h2>{{ t('templates.policy.invocations.title') }}</h2>
    <p class="panel-hint">{{ t('templates.policy.invocations.description') }}</p>

    <div class="filters-row">
      <el-form-item :label="t('templates.policy.invocations.filters.status')" class="filter-item">
        <el-select v-model="filterDraft.status" clearable>
          <el-option
            v-for="option in statusFilterOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('templates.policy.invocations.filters.kind')" class="filter-item">
        <el-select v-model="filterDraft.invocationKind" clearable>
          <el-option
            v-for="option in kindFilterOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item :label="t('templates.policy.invocations.filters.requestId')" class="filter-item">
        <div data-testid="invocation-request-id-filter">
          <el-input v-model="filterDraft.requestId" clearable />
        </div>
      </el-form-item>
      <div class="filters-actions">
        <el-button type="primary" @click="applyFilters">
          {{ t('templates.policy.invocations.filters.apply') }}
        </el-button>
        <el-button @click="clearFilters">
          {{ t('templates.policy.invocations.filters.clear') }}
        </el-button>
      </div>
    </div>

    <el-skeleton v-if="loading" :rows="4" animated />
    <LoadErrorPanel
      v-else-if="loadFailed"
      message-key="templates.policy.invocations.loadFailed"
      @retry="loadInvocations"
    />
    <EmptyStatePanel
      v-else-if="rows.length === 0"
      title-key="templates.policy.invocations.emptyTitle"
      description-key="templates.policy.invocations.emptyDescription"
    />
    <template v-else>
      <AppDataTable
        :data="rows"
        class="invocation-table"
        activatable
        @row-click="openInvocationSummary"
      >
        <el-table-column
          :label="t('templates.policy.invocations.columns.createdAt')"
          min-width="160"
        >
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column
          :label="t('templates.policy.invocations.columns.invocationId')"
          min-width="180"
        >
          <template #default="{ row }">
            <div class="technical-id-cell">
              <span class="technical-id-cell__value">{{ row.invocationId }}</span>
              <el-tooltip :content="t('common.copyToClipboard')">
                <el-button
                  link
                  type="primary"
                  class="technical-id-cell__copy"
                  :aria-label="t('common.copyToClipboard')"
                  data-testid="copy-invocation-id"
                  @click.stop="copyTechnicalId(row.invocationId)"
                >
                  <el-icon><DocumentCopy /></el-icon>
                </el-button>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          prop="invocationKind"
          :label="t('templates.policy.invocations.columns.kind')"
          min-width="120"
        />
        <el-table-column
          prop="status"
          :label="t('templates.policy.invocations.columns.status')"
          min-width="120"
        />
        <el-table-column
          :label="t('templates.policy.invocations.columns.requestId')"
          min-width="180"
        >
          <template #default="{ row }">
            <div class="technical-id-cell">
              <span class="technical-id-cell__value">{{ row.requestId }}</span>
              <el-tooltip :content="t('common.copyToClipboard')">
                <el-button
                  link
                  type="primary"
                  class="technical-id-cell__copy"
                  :aria-label="t('common.copyToClipboard')"
                  data-testid="copy-request-id"
                  @click.stop="copyTechnicalId(row.requestId)"
                >
                  <el-icon><DocumentCopy /></el-icon>
                </el-button>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          prop="accessAccountSummary"
          :label="t('templates.policy.invocations.columns.accessAccount')"
          min-width="120"
        />
      </AppDataTable>

      <AppTablePagination
        v-model:current-page="uiPage"
        :page-size="pageSize"
        :total="totalElements"
      />
    </template>

    <InvocationSummaryDrawer
      v-model:visible="drawerVisible"
      :template-id="templateId"
      :invocation-id="selectedInvocationId"
    />
  </el-card>
</template>

<style scoped lang="scss">
.section-card {
  margin-bottom: var(--space-6);

  h2 {
    margin: 0 0 var(--space-3);
    font-size: var(--font-size-lg);
  }
}

.panel-hint {
  margin: 0 0 var(--space-4);
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

.filters-row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: var(--space-4);
  margin-bottom: var(--space-4);
}

.filter-item {
  margin-bottom: 0;
  min-width: 12rem;
}

.filters-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
}

.invocation-table {
  width: 100%;
}

.technical-id-cell {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  min-width: 0;
}

.technical-id-cell__value {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.technical-id-cell__copy {
  flex-shrink: 0;
}
</style>
