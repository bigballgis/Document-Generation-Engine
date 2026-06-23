<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import DashboardStatCards from '@/components/dashboard/DashboardStatCards.vue'
import { useDashboardStats } from '@/composables/useDashboardStats'
import { dashboardQuickLinks, useWorkflowTasks, type WorkflowTask } from '@/composables/useWorkflowTasks'
import { useMastersStore } from '@/stores/masters'
import { useSessionStore } from '@/stores/session'
import { useTemplatesStore } from '@/stores/templates'

const { t } = useI18n()
const router = useRouter()
const sessionStore = useSessionStore()
const mastersStore = useMastersStore()
const templatesStore = useTemplatesStore()
const { tasks } = useWorkflowTasks()
const { stats } = useDashboardStats()

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

onMounted(async () => {
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
})

function openTask(path: string) {
  router.push(path)
}

function openQuickLink(path: string) {
  router.push(path)
}
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

    <el-alert
      v-if="loadError"
      type="error"
      :title="t('dashboard.loadError')"
      show-icon
      :closable="false"
      class="page-alert"
    />

    <DashboardStatCards :stats="stats" :loading="loading" />

    <section class="tasks-section">
      <header class="section-header">
        <h2>{{ t('dashboard.tasks.title') }}</h2>
        <p>{{ t('dashboard.tasks.description') }}</p>
      </header>

      <el-skeleton v-if="loading" :rows="5" animated />

      <el-empty
        v-else-if="tasks.length === 0"
        :description="t('dashboard.tasks.empty')"
      />

      <el-table
        v-else
        :data="tasks"
        stripe
        class="tasks-table"
        @row-click="(row: WorkflowTask) => openTask(row.path)"
      >
        <el-table-column :label="t('dashboard.tasks.columns.action')" min-width="200">
          <template #default="{ row }">
            <strong>{{ t(row.titleKey) }}</strong>
          </template>
        </el-table-column>
        <el-table-column :label="t('dashboard.tasks.columns.item')" min-width="220">
          <template #default="{ row }">
            {{ row.entityName }}
          </template>
        </el-table-column>
        <el-table-column :label="t('dashboard.tasks.columns.group')" width="140">
          <template #default="{ row }">
            {{ row.groupCode ?? '—' }}
          </template>
        </el-table-column>
        <el-table-column :label="t('dashboard.tasks.columns.hint')" min-width="260">
          <template #default="{ row }">
            {{ t(row.descriptionKey) }}
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section v-if="quickLinks.length > 0" class="quick-links">
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

.page-alert {
  margin-bottom: 1rem;
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
