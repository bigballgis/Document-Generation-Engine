<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { BRAND_THEMES } from '@/theme/tokens'
import {
  pathForRouteKey,
  ROUTE_NAV_LABEL_KEY,
  type RouteKey,
} from '@/routing/routeKeys'
import { useAppStore } from '@/stores/app'
import { useSessionStore } from '@/stores/session'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const sessionStore = useSessionStore()

const brandLabel = computed(() => t(`brand.${appStore.brand.toLowerCase()}`))
const logoLabel = computed(() => BRAND_THEMES[appStore.brand].logoSlotLabel)

interface NavItem {
  routeKey: RouteKey
  path: string
  labelKey: string
}

const navItems = computed<NavItem[]>(() => {
  const session = sessionStore.session
  if (!session) {
    return []
  }

  return session.visibleRoutes
    .filter((key): key is RouteKey => key in ROUTE_NAV_LABEL_KEY)
    .map((routeKey) => ({
      routeKey,
      path: pathForRouteKey(routeKey),
      labelKey: ROUTE_NAV_LABEL_KEY[routeKey],
    }))
})

function isActive(path: string): boolean {
  if (path === route.path) {
    return true
  }
  return route.path.startsWith(`${path}/`)
}

async function handleLogout() {
  await sessionStore.logout()
  router.push('/login')
}

function navigate(path: string) {
  router.push(path)
}
</script>

<template>
  <div class="management-shell">
    <header class="shell-header">
      <div class="header-brand">
        <div class="brand-slot" :aria-label="brandLabel">{{ logoLabel }}</div>
        <h1 class="app-title">{{ t('app.title') }}</h1>
      </div>
      <div class="header-actions">
        <span class="user-label">{{ sessionStore.session?.displayName }}</span>
        <el-button type="primary" plain @click="handleLogout">
          {{ t('nav.logout') }}
        </el-button>
      </div>
    </header>

    <div class="shell-body">
      <aside class="shell-nav">
        <nav :aria-label="t('nav.managementNavigation')">
          <button
            v-for="item in navItems"
            :key="item.routeKey"
            type="button"
            class="nav-item"
            :class="{ active: isActive(item.path) }"
            @click="navigate(item.path)"
          >
            {{ t(item.labelKey) }}
          </button>
        </nav>
      </aside>

      <main class="shell-content">
        <slot />
      </main>
    </div>
  </div>
</template>

<style scoped lang="scss">
.management-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--surface-bg);
}

.shell-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.75rem 1.5rem;
  border-bottom: 1px solid var(--border-color);
  background: var(--brand-header-bg);
}

.header-brand {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.brand-slot {
  display: inline-flex;
  padding: 0.35rem 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  font-weight: 600;
  color: var(--brand-primary);
}

.app-title {
  margin: 0;
  font-size: 1.125rem;
  font-weight: 600;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.user-label {
  color: var(--text-muted);
  font-weight: 500;
}

.shell-body {
  display: flex;
  flex: 1;
  min-height: 0;
}

.shell-nav {
  width: 240px;
  flex-shrink: 0;
  padding: 1rem 0;
  border-right: 1px solid var(--border-color);
  background: #fafbfc;
}

nav {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  padding: 0 0.75rem;
}

.nav-item {
  display: block;
  width: 100%;
  padding: 0.65rem 0.85rem;
  border: none;
  border-radius: 4px;
  background: transparent;
  text-align: left;
  font: inherit;
  color: #1a1a1a;
  cursor: pointer;

  &:hover {
    background: #eef1f4;
  }

  &.active {
    background: color-mix(in srgb, var(--brand-primary) 12%, white);
    color: var(--brand-primary);
    font-weight: 600;
  }
}

.shell-content {
  flex: 1;
  min-width: 0;
  overflow: auto;
}
</style>
