<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import AppDataTable from '@/components/common/AppDataTable.vue'
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import DashboardStatCards from '@/components/dashboard/DashboardStatCards.vue'
import { rowSortMethod, useDataTableFilters } from '@/composables/useDataTableFilters'
import { useDashboardStats } from '@/composables/useDashboardStats'
import { dashboardQuickLinks, useWorkflowTasks, type WorkflowTask } from '@/composables/useWorkflowTasks'
import { useMastersStore } from '@/stores/masters'
import { useSessionStore } from '@/stores/session'
import { useTemplatesStore } from '@/stores/templates'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const sessionStore = useSessionStore()
const mastersStore = useMastersStore()
const templatesStore = useTemplatesStore()
const { tasks } = useWorkflowTasks()
const visibleRoutes = computed(() => sessionStore.session?.visibleRoutes ?? [])
const { stats } = useDashboardStats(visibleRoutes)
const { filters: taskColumnFilters, filteredRows: filteredTasks } = useDataTableFilters(tasks, [
  {
    key: 'action',
    getValue: (row) => t(row.titleKey),
  },
  { key: 'item', getValue: (row) => row.entityName },
  { key: 'group', getValue: (row) => row.groupCode ?? '' },
  {
    key: 'hint',
    getValue: (row) => t(row.descriptionKey),
  },
])

const loading = ref(false)
const loadError = ref(false)

const quickLinks = computed(() =>
  dashboardQuickLinks(sessionStore.session?.visibleRoutes ?? []),
)

const authorizedGroupsSummary = computed(() => {
  const groups = sessionStore.session?.authorizedGroupCodes ?? []
  if (groups.includes('*')) {
    return t('home.summary.allGroups')
  }
  if (groups.length === 0) {
    return t('home.summary.noGroups')
  }
  return groups.join(', ')
})

const showDataSections = computed(() => !loadError.value)

async function loadDashboardData() {
  loading.value = true
  loadError.value = false
  try {
    const jobs: Promise<void>[] = []
    if (sessionStore.canAccessRoute('route.master-management')) {
      jobs.push(mastersStore.fetchMasters())
    }
    if (sessionStore.canAccessRoute('route.template-management')) {
      jobs.push(templatesStore.fetchTemplates())
    }
    await Promise.all(jobs)
  } catch {
    loadError.value = true
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadDashboardData()
  void scrollToTasksIfRequested()
})

watch(
  () => route.hash,
  () => {
    void scrollToTasksIfRequested()
  },
)

async function scrollToTasksIfRequested() {
  if (route.hash !== '#tasks-section') {
    return
  }
  await nextTick()
  document.getElementById('tasks-section')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function openTask(path: string) {
  router.push(path)
}

function openQuickLink(path: string) {
  router.push(path)
}

const sortTasksByTitle = rowSortMethod<WorkflowTask>((row) => t(row.titleKey))
const sortTasksByGroup = rowSortMethod<WorkflowTask>((row) => row.groupCode ?? '')
const sortTasksByHint = rowSortMethod<WorkflowTask>((row) => t(row.descriptionKey))
</script>

<template>
  <div class="dashboard-page">
    <header class="page-header">
      <h1>{{ t('dashboard.title') }}</h1>
      <p>{{ t('dashboard.description') }}</p>
    </header>

    <el-card shadow="never" class="summary-card">
      <h2>{{ t('home.summary.title') }}</h2>
      <dl class="summary-grid">
        <div>
          <dt>{{ t('home.summary.displayName') }}</dt>
          <dd>{{ sessionStore.session?.displayName }}</dd>
        </div>
        <div>
          <dt>{{ t('home.summary.authorizedGroups') }}</dt>
          <dd>{{ authorizedGroupsSummary }}</dd>
        </div>
      </dl>
    </el-card>

    <LoadErrorPanel
      v-if="loadError"
      message-key="dashboard.loadError"
      @retry="loadDashboardData"
    />

    <DashboardStatCards v-if="showDataSections" :stats="stats" :loading="loading" />

    <section v-if="showDataSections" id="tasks-section" class="tasks-section">
      <header class="section-header">
        <h2>{{ t('dashboard.tasks.title') }}</h2>
        <p>{{ t('dashboard.tasks.description') }}</p>
      </header>

      <el-skeleton v-if="loading" :rows="5" animated />

      <el-empty
        v-else-if="filteredTasks.length === 0"
        :description="t('dashboard.tasks.empty')"
      />

      <AppDataTable
        v-else
        :data="filteredTasks"
        class="tasks-table"
        @row-click="(row: WorkflowTask) => openTask(row.path)"
      >
        <el-table-column
          sortable
          :sort-method="sortTasksByTitle"
          min-width="200"
        >
          <template #header>
            <TableColumnHeader
              :label="t('dashboard.tasks.columns.action')"
              v-model="taskColumnFilters.action"
            />
          </template>
          <template #default="{ row }">
            <strong>{{ t(row.titleKey) }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="entityName" sortable min-width="220">
          <template #header>
            <TableColumnHeader
              :label="t('dashboard.tasks.columns.item')"
              v-model="taskColumnFilters.item"
            />
          </template>
          <template #default="{ row }">
            {{ row.entityName }}
          </template>
        </el-table-column>
        <el-table-column
          sortable
          :sort-method="sortTasksByGroup"
          width="140"
        >
          <template #header>
            <TableColumnHeader
              :label="t('dashboard.tasks.columns.group')"
              v-model="taskColumnFilters.group"
            />
          </template>
          <template #default="{ row }">
            {{ row.groupCode ?? '—' }}
          </template>
        </el-table-column>
        <el-table-column
          sortable
          :sort-method="sortTasksByHint"
          min-width="260"
        >
          <template #header>
            <TableColumnHeader
              :label="t('dashboard.tasks.columns.hint')"
              v-model="taskColumnFilters.hint"
            />
          </template>
          <template #default="{ row }">
            {{ t(row.descriptionKey) }}
          </template>
        </el-table-column>
      </AppDataTable>
    </section>

    <section v-if="showDataSections && quickLinks.length > 0" class="quick-links">
      <h2>{{ t('dashboard.quickLinks.title') }}</h2>
      <div class="quick-link-grid">
        <el-button
          v-for="link in quickLinks"
          :key="link.path"
          type="primary"
          plain
          @click="openQuickLink(link.path)"
        >
          {{ t(link.labelKey) }}
        </el-button>
      </div>
    </section>
  </div>
</template>

<style scoped lang="scss">
.dashboard-page {
  padding: 2rem;
  max-width: 1200px;
}

.page-header {
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

.summary-card {
  margin-bottom: 1.5rem;

  h2 {
    margin: 0 0 1rem;
    font-size: 1.1rem;
  }
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;

  dt {
    font-size: 0.8rem;
    color: var(--text-muted);
    margin-bottom: 0.25rem;
  }

  dd {
    margin: 0;
    font-weight: 600;
  }
}

.tasks-section {
  margin-bottom: 2rem;
}

.section-header {
  margin-bottom: 1rem;

  h2 {
    margin: 0 0 0.25rem;
    font-size: 1.25rem;
  }

  p {
    margin: 0;
    color: var(--text-muted);
  }
}

.tasks-table {
  cursor: pointer;
}

.quick-links {
  h2 {
    margin: 0 0 1rem;
    font-size: 1.1rem;
  }
}

.quick-link-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
}
</style>
