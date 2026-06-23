<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import {
  pathForRouteKey,
  type RouteKey,
} from '@/routing/routeKeys'
import { useSessionStore } from '@/stores/session'

const props = defineProps<{
  routeKey: string
  titleKey: string
  descriptionKey: string
}>()

const { t } = useI18n()
const router = useRouter()
const sessionStore = useSessionStore()

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
.nav-card {
  margin-bottom: 1rem;

  h2 {
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
</style>
