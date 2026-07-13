<script setup lang="ts">
import { toRef } from 'vue'
import { DocumentCopy } from '@element-plus/icons-vue'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import InvocationSummaryDrawer from '@/components/templates/InvocationSummaryDrawer.vue'
import { useTemplateInvocationsPanel } from '@/components/templates/useTemplateInvocationsPanel'

const props = defineProps<{
  templateId: string
}>()

const {
  t,
  formatDateTime,
  pageSize,
  totalElements,
  loading,
  loadFailed,
  rows,
  filterDraft,
  drawerVisible,
  selectedInvocationId,
  statusFilterOptions,
  kindFilterOptions,
  uiPage,
  loadInvocations,
  applyFilters,
  clearFilters,
  openInvocationSummary,
  copyTechnicalId,
} = useTemplateInvocationsPanel({
  templateId: toRef(props, 'templateId'),
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

<style scoped lang="scss" src="./TemplateInvocationsPanel.scss"></style>
