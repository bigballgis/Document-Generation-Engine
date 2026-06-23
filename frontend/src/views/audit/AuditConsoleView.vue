<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { isGroupScopedAuditRole } from '@/auth/roles'
import { useAuditStore } from '@/stores/audit'
import { useSessionStore } from '@/stores/session'
import type { LifecycleAuditEvent, ManagementAuditEvent } from '@/types/audit'
import { ElMessage } from 'element-plus'

const { t, te } = useI18n()
const auditStore = useAuditStore()
const sessionStore = useSessionStore()

const activeTab = ref<'management' | 'lifecycle'>('management')

const errorMessage = computed(() => {
  const key = auditStore.lastErrorMessageKey
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('audit.error.loadManagement')
})

const showGroupFilters = computed(() => isGroupScopedAuditRole(auditStore.actorRole))
const groupOptions = computed(() => sessionStore.session?.authorizedGroupCodes ?? [])

onMounted(async () => {
  auditStore.initializeFiltersFromSession()
  await refreshActiveTab()
})

async function refreshActiveTab() {
  try {
    if (activeTab.value === 'management') {
      await auditStore.fetchManagementEvents()
    } else {
      await auditStore.fetchLifecycleEvents()
    }
  } catch {
    // Error surfaced via store message key.
  }
}

async function handleTabChange(tab: string | number | boolean) {
  activeTab.value = tab as 'management' | 'lifecycle'
  await refreshActiveTab()
}

async function applyFilters() {
  await refreshActiveTab()
}

async function handleExport() {
  try {
    const result = await auditStore.exportManagementEvents()
    const blob = new Blob([JSON.stringify(result, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = 'management-audit-export.json'
    anchor.click()
    URL.revokeObjectURL(url)
    ElMessage.success(t('audit.export.success'))
  } catch {
    ElMessage.error(errorMessage.value || t('audit.error.export'))
  }
}

function formatDate(value: string) {
  return new Date(value).toLocaleString()
}
</script>

<template>
  <div class="audit-page">
    <header class="page-header">
      <div>
        <h1>{{ t('audit.title') }}</h1>
        <p>{{ t('audit.description') }}</p>
      </div>
      <el-button
        v-if="activeTab === 'management'"
        type="primary"
        :loading="auditStore.exporting"
        @click="handleExport"
      >
        {{ t('audit.export.action') }}
      </el-button>
    </header>

    <el-alert
      v-if="errorMessage"
      class="page-alert"
      type="error"
      :title="errorMessage"
      show-icon
      :closable="false"
    />

    <el-card shadow="never" class="filters-card">
      <div class="filters-grid">
        <el-form-item :label="t('audit.filters.eventType')">
          <el-input
            v-model="auditStore.filters.eventType"
            :placeholder="t('audit.filters.eventTypePlaceholder')"
            clearable
          />
        </el-form-item>
        <el-form-item :label="t('audit.filters.eventAtFrom')">
          <el-date-picker
            v-model="auditStore.filters.eventAtFrom"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss[Z]"
            :placeholder="t('audit.filters.eventAtFrom')"
            clearable
          />
        </el-form-item>
        <el-form-item :label="t('audit.filters.eventAtTo')">
          <el-date-picker
            v-model="auditStore.filters.eventAtTo"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss[Z]"
            :placeholder="t('audit.filters.eventAtTo')"
            clearable
          />
        </el-form-item>
        <el-form-item v-if="showGroupFilters" :label="t('audit.filters.groupScope')">
          <el-select v-model="auditStore.filters.groupScope" clearable>
            <el-option
              v-for="group in groupOptions"
              :key="group"
              :label="group"
              :value="group"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="showGroupFilters" :label="t('audit.filters.templateId')">
          <el-input
            v-model="auditStore.filters.templateId"
            :placeholder="t('audit.filters.templateIdPlaceholder')"
            clearable
          />
        </el-form-item>
        <div class="filters-actions">
          <el-button type="primary" @click="applyFilters">
            {{ t('audit.filters.apply') }}
          </el-button>
        </div>
      </div>
    </el-card>

    <el-tabs :model-value="activeTab" @tab-change="handleTabChange">
      <el-tab-pane :label="t('audit.tabs.management')" name="management">
        <el-skeleton v-if="auditStore.loadingManagement" :rows="6" animated />
        <el-table
          v-else
          :data="auditStore.managementEvents"
          stripe
          empty-text=""
        >
          <template #empty>
            <el-empty :description="t('audit.empty.management')" />
          </template>
          <el-table-column :label="t('audit.columns.eventAt')" min-width="180">
            <template #default="{ row }: { row: ManagementAuditEvent }">
              {{ formatDate(row.eventAt) }}
            </template>
          </el-table-column>
          <el-table-column prop="eventType" :label="t('audit.columns.eventType')" min-width="160" />
          <el-table-column prop="templateId" :label="t('audit.columns.templateId')" min-width="200" />
          <el-table-column prop="actorSummary" :label="t('audit.columns.actorSummary')" min-width="160" />
          <el-table-column prop="statusSummary" :label="t('audit.columns.statusSummary')" min-width="160" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane :label="t('audit.tabs.lifecycle')" name="lifecycle">
        <el-skeleton v-if="auditStore.loadingLifecycle" :rows="6" animated />
        <el-table
          v-else
          :data="auditStore.lifecycleEvents"
          stripe
          empty-text=""
        >
          <template #empty>
            <el-empty :description="t('audit.empty.lifecycle')" />
          </template>
          <el-table-column :label="t('audit.columns.eventAt')" min-width="180">
            <template #default="{ row }: { row: LifecycleAuditEvent }">
              {{ formatDate(row.eventAt) }}
            </template>
          </el-table-column>
          <el-table-column prop="eventType" :label="t('audit.columns.eventType')" min-width="160" />
          <el-table-column prop="templateId" :label="t('audit.columns.templateId')" min-width="200" />
          <el-table-column prop="fromState" :label="t('audit.columns.fromState')" width="140" />
          <el-table-column prop="toState" :label="t('audit.columns.toState')" width="140" />
          <el-table-column prop="summary" :label="t('audit.columns.summary')" min-width="200" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped lang="scss">
.audit-page {
  padding: 2rem;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.5rem;

  h1 {
    margin: 0 0 0.25rem;
    font-size: 1.75rem;
  }

  p {
    margin: 0;
    color: var(--text-muted);
  }
}

.page-alert {
  margin-bottom: 1rem;
}

.filters-card {
  margin-bottom: 1.5rem;
}

.filters-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 0.75rem 1rem;
  align-items: end;
}

.filters-actions {
  display: flex;
  align-items: flex-end;
  padding-bottom: 4px;
}
</style>
