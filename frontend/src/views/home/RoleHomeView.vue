<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import {
  pathForRouteKey,
  ROUTE_KEYS,
  type RouteKey,
} from '@/routing/routeKeys'
import { useMastersStore } from '@/stores/masters'
import { useSessionStore } from '@/stores/session'
import { useTemplatesStore } from '@/stores/templates'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'

const props = defineProps<{
  routeKey: string
  titleKey: string
  descriptionKey: string
}>()

const { t } = useI18n()
const router = useRouter()
const sessionStore = useSessionStore()
const mastersStore = useMastersStore()
const templatesStore = useTemplatesStore()

const dashboardLoading = ref(false)
const dashboardError = ref(false)

interface QuickLink {
  routeKey: RouteKey
  path: string
  titleKey: string
  descriptionKey: string
}

const QUICK_LINK_META: Partial<Record<RouteKey, { titleKey: string; descriptionKey: string }>> = {
  'route.master-management': {
    titleKey: 'home.nav.masters',
    descriptionKey: 'home.nav.mastersDescription',
  },
  'route.template-management': {
    titleKey: 'home.nav.templates',
    descriptionKey: 'home.nav.templatesDescription',
  },
  'route.tester-workbench': {
    titleKey: 'workbench.tester.title',
    descriptionKey: 'workbench.tester.description',
  },
  'route.approver-workbench': {
    titleKey: 'workbench.approver.title',
    descriptionKey: 'workbench.approver.description',
  },
  'route.api-policy-management': {
    titleKey: 'home.apiPolicy.title',
    descriptionKey: 'home.apiPolicy.description',
  },
  'route.audit-console': {
    titleKey: 'home.audit.title',
    descriptionKey: 'home.audit.description',
  },
}

const isGovernanceHome = computed(
  () =>
    props.routeKey === ROUTE_KEYS.globalGovernanceHome ||
    props.routeKey === ROUTE_KEYS.groupGovernanceHome,
)

const quickLinks = computed<QuickLink[]>(() => {
  const session = sessionStore.session
  if (!session) {
    return []
  }
  return session.visibleRoutes
    .filter((key) => key !== props.routeKey && key in QUICK_LINK_META)
    .map((routeKey) => {
      const meta = QUICK_LINK_META[routeKey as RouteKey]!
      return {
        routeKey: routeKey as RouteKey,
        path: pathForRouteKey(routeKey),
        titleKey: meta.titleKey,
        descriptionKey: meta.descriptionKey,
      }
    })
})

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

const dashboardStats = computed(() => {
  const pendingMasterReviews = mastersStore.masters.filter(
    (item) => item.status === 'PENDING_REVIEW',
  ).length
  const pendingTemplateLifecycle = templatesStore.templates.filter((item) =>
    ['TESTING', 'APPROVAL', 'PENDING_RELEASE'].includes(item.lifecycleStatus),
  ).length
  const publishedTemplates = templatesStore.templates.filter(
    (item) => item.lifecycleStatus === 'PUBLISHED',
  ).length
  const stoppedTemplates = templatesStore.templates.filter(
    (item) => item.lifecycleStatus === 'STOPPED',
  ).length

  return [
    {
      key: 'pendingMasterReviews',
      count: pendingMasterReviews,
      titleKey: 'home.dashboard.pendingMasterReviews',
      descriptionKey: 'home.dashboard.pendingMasterReviewsDescription',
      actionKey: 'home.dashboard.viewMasters',
      path: pathForRouteKey(ROUTE_KEYS.masterManagement),
    },
    {
      key: 'pendingTemplateLifecycle',
      count: pendingTemplateLifecycle,
      titleKey: 'home.dashboard.pendingTemplateLifecycle',
      descriptionKey: 'home.dashboard.pendingTemplateLifecycleDescription',
      actionKey: 'home.dashboard.viewTemplates',
      path: pathForRouteKey(ROUTE_KEYS.templateManagement),
    },
    {
      key: 'publishedTemplates',
      count: publishedTemplates,
      titleKey: 'home.dashboard.publishedTemplates',
      descriptionKey: 'home.dashboard.publishedTemplatesDescription',
      actionKey: 'home.dashboard.viewTemplates',
      path: pathForRouteKey(ROUTE_KEYS.templateManagement),
    },
    {
      key: 'stoppedTemplates',
      count: stoppedTemplates,
      titleKey: 'home.dashboard.stoppedTemplates',
      descriptionKey: 'home.dashboard.stoppedTemplatesDescription',
      actionKey: 'home.dashboard.viewTemplates',
      path: pathForRouteKey(ROUTE_KEYS.templateManagement),
    },
  ]
})

const workbenchLinks = computed(() => {
  const session = sessionStore.session
  if (!session) {
    return []
  }
  const links: Array<{ titleKey: string; path: string }> = []
  if (session.visibleRoutes.includes(ROUTE_KEYS.testerWorkbench)) {
    links.push({
      titleKey: 'home.dashboard.viewTesterWorkbench',
      path: pathForRouteKey(ROUTE_KEYS.testerWorkbench),
    })
  }
  if (session.visibleRoutes.includes(ROUTE_KEYS.approverWorkbench)) {
    links.push({
      titleKey: 'home.dashboard.viewApproverWorkbench',
      path: pathForRouteKey(ROUTE_KEYS.approverWorkbench),
    })
  }
  return links
})

onMounted(async () => {
  if (!isGovernanceHome.value) {
    return
  }
  await loadDashboardStats()
})

async function loadDashboardStats() {
  dashboardLoading.value = true
  dashboardError.value = false
  try {
    await Promise.all([mastersStore.fetchMasters(), templatesStore.fetchTemplates()])
  } catch {
    dashboardError.value = true
  } finally {
    dashboardLoading.value = false
  }
}

function navigate(path: string) {
  router.push(path)
}
</script>

<template>
  <div class="home-page">
    <header class="home-header">
      <h1>{{ t(titleKey) }}</h1>
      <p>{{ t(descriptionKey) }}</p>
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
        <div>
          <dt>{{ t('home.summary.workspaceCount') }}</dt>
          <dd>{{ (sessionStore.session?.visibleRoutes.length ?? 0) - 1 }}</dd>
        </div>
      </dl>
    </el-card>

    <template v-if="isGovernanceHome">
      <LoadErrorPanel
        v-if="dashboardError"
        message-key="home.dashboard.loadError"
        @retry="loadDashboardStats"
      />

      <section v-else class="dashboard-section">
        <h2>{{ t('home.dashboard.title') }}</h2>
        <el-skeleton v-if="dashboardLoading" :rows="4" animated />
        <div v-else class="dashboard-grid">
          <el-card
            v-for="stat in dashboardStats"
            :key="stat.key"
            shadow="never"
            class="stat-card"
          >
            <p class="stat-count">{{ stat.count }}</p>
            <h3>{{ t(stat.titleKey) }}</h3>
            <p>{{ t(stat.descriptionKey) }}</p>
            <el-button type="primary" link @click="navigate(stat.path)">
              {{ t(stat.actionKey) }}
            </el-button>
          </el-card>
        </div>

        <div v-if="workbenchLinks.length > 0" class="workbench-links">
          <el-button
            v-for="link in workbenchLinks"
            :key="link.path"
            @click="navigate(link.path)"
          >
            {{ t(link.titleKey) }}
          </el-button>
        </div>
      </section>
    </template>

    <el-card
      v-for="link in quickLinks"
      :key="link.routeKey"
      shadow="never"
      class="nav-card"
    >
      <h2>{{ t(link.titleKey) }}</h2>
      <p>{{ t(link.descriptionKey) }}</p>
      <el-button type="primary" @click="navigate(link.path)">
        {{ t('home.summary.openWorkspace') }}
      </el-button>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.home-page {
  padding: 2rem;
}

.home-header {
  margin-bottom: 1.5rem;

  h1 {
    margin: 0.75rem 0 0.25rem;
    font-size: 1.75rem;
  }

  p {
    margin: 0;
    color: var(--text-muted);
  }
}

.summary-card,
.nav-card,
.stat-card {
  margin-bottom: 1rem;

  h2,
  h3 {
    margin: 0 0 0.5rem;
    font-size: 1.125rem;
  }

  p {
    margin: 0 0 1rem;
    color: var(--text-muted);
  }
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1rem;
  margin: 0;

  dt {
    margin: 0;
    font-size: 0.85rem;
    color: var(--text-muted);
  }

  dd {
    margin: 0.25rem 0 0;
    font-weight: 500;
  }
}

.dashboard-section {
  margin-bottom: 1.5rem;

  h2 {
    margin: 0 0 1rem;
    font-size: 1.25rem;
  }
}

.dashboard-alert {
  margin-bottom: 1rem;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 1rem;
}

.stat-count {
  margin: 0 0 0.5rem;
  font-size: 2rem;
  font-weight: 700;
  color: var(--brand-primary);
}

.workbench-links {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-top: 1rem;
}
</style>
