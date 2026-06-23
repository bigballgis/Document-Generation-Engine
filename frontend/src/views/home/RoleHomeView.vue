<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  canAccessMasterManagement,
  canAccessTemplateManagement,
} from '@/auth/roles'
import { ROUTE_PATH_BY_KEY, ROUTE_KEYS } from '@/routing/routeKeys'
import { useSessionStore } from '@/stores/session'

const props = defineProps<{
  routeKey: string
  titleKey: string
  descriptionKey: string
}>()

const { t } = useI18n()
const router = useRouter()
const sessionStore = useSessionStore()

const homePath = computed(() => ROUTE_PATH_BY_KEY[props.routeKey as keyof typeof ROUTE_PATH_BY_KEY])
const showMasterNav = computed(() =>
  canAccessMasterManagement(sessionStore.session?.roles ?? []),
)
const showTemplateNav = computed(() =>
  canAccessTemplateManagement(sessionStore.session?.roles ?? []),
)

function openMasters() {
  router.push(ROUTE_PATH_BY_KEY[ROUTE_KEYS.masterManagement])
}

function openTemplates() {
  router.push(ROUTE_PATH_BY_KEY[ROUTE_KEYS.templateManagement])
}
</script>

<template>
  <div class="home-page">
    <header class="home-header">
      <h1>{{ t(titleKey) }}</h1>
      <p>{{ t(descriptionKey) }}</p>
      <p class="route-meta">{{ routeKey }}</p>
    </header>
    <el-card v-if="showMasterNav" shadow="never" class="nav-card">
      <h2>{{ t('home.nav.masters') }}</h2>
      <p>{{ t('home.nav.mastersDescription') }}</p>
      <el-button type="primary" @click="openMasters">
        {{ t('home.nav.masters') }}
      </el-button>
    </el-card>
    <el-card v-if="showTemplateNav" shadow="never" class="nav-card">
      <h2>{{ t('home.nav.templates') }}</h2>
      <p>{{ t('home.nav.templatesDescription') }}</p>
      <el-button type="primary" @click="openTemplates">
        {{ t('home.nav.templates') }}
      </el-button>
    </el-card>
    <el-card shadow="never">
      <p>{{ t('home.placeholder') }}</p>
      <p class="path-meta">{{ homePath }}</p>
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

.route-meta,
.path-meta {
  font-family: monospace;
  font-size: 0.85rem;
}
</style>
