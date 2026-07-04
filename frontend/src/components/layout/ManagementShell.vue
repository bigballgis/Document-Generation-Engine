<script setup lang="ts">
import { computed, ref, type Component } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  ArrowRight,
  ArrowDown,
  SwitchButton,
  HomeFilled,
  Postcard,
  Document,
  Collection,
  Connection,
  Histogram,
  User,
  UserFilled,
} from '@element-plus/icons-vue'
import BrandLogo from '@/components/branding/BrandLogo.vue'
import AppBreadcrumb from '@/components/layout/AppBreadcrumb.vue'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import SessionLimitReminder from '@/components/session/SessionLimitReminder.vue'
import { useSessionRenewal } from '@/composables/useSessionRenewal'
import { BRAND_REGISTRY } from '@/config/brands'
import { LOCALE_REGISTRY, resolveAppLocale } from '@/i18n/localeRegistry'
import { buildBreadcrumbTrail } from '@/navigation/breadcrumbTrail'
import { buildVisibleNavGroups, resolveNavItemTarget, type NavItemDefinition } from '@/navigation/navStructure'
import { useAppStore } from '@/stores/app'
import { useSessionStore } from '@/stores/session'
import type { BrandPreset } from '@/theme/tokens'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const sessionStore = useSessionStore()

// ── Session renewal + absolute-limit reminder (LR-B6) ────────────────────────
const { reminderVisible } = useSessionRenewal()

async function handleSessionReminderAction() {
  await sessionStore.logout()
  await router.push({ name: 'login', query: { redirect: route.fullPath } })
}

// ── Sidebar collapse ──────────────────────────────────────────────────────────
const COLLAPSED_KEY = 'docgen.nav.collapsed'
const navCollapsed = ref(localStorage.getItem(COLLAPSED_KEY) === 'true')

function toggleCollapsed() {
  navCollapsed.value = !navCollapsed.value
  localStorage.setItem(COLLAPSED_KEY, String(navCollapsed.value))
}

// ── Nav icons ─────────────────────────────────────────────────────────────────
const NAV_ICON_MAP: Record<string, Component> = {
  dashboard: HomeFilled,
  users: User,
  groups: UserFilled,
  masters: Postcard,
  templates: Document,
  'content-modules': Collection,
  'api-policies': Connection,
  audit: Histogram,
}

function getNavIcon(itemId: string): Component | undefined {
  return NAV_ICON_MAP[itemId]
}

// ── Brand / locale ────────────────────────────────────────────────────────────
const brandConfig = computed(
  () => BRAND_REGISTRY.find((entry) => entry.code === appStore.brand) ?? BRAND_REGISTRY[0],
)
const brandLabel = computed(() => t(brandConfig.value.labelKey))
const localeOptions = computed(() =>
  LOCALE_REGISTRY.map((entry) => ({
    value: entry.code,
    label: t(entry.labelKey),
  })),
)
const brandOptions = computed(() =>
  BRAND_REGISTRY.map((entry) => ({
    value: entry.code as BrandPreset,
    label: t(entry.labelKey),
  })),
)

// ── Navigation ────────────────────────────────────────────────────────────────
const navGroups = computed(() => {
  const session = sessionStore.session
  if (!session) {
    return []
  }
  return buildVisibleNavGroups(session.visibleRoutes, session.roles, session.capabilities)
})

// Breadcrumb hidden on top-level list pages (see breadcrumbTrail.ts).
const breadcrumbSegments = computed(() => buildBreadcrumbTrail(route.path))

function isNavItemActive(item: NavItemDefinition): boolean {
  if (item.path === route.path) {
    return true
  }
  return route.path.startsWith(`${item.path}/`)
}

function navigateToItem(item: NavItemDefinition) {
  router.push(resolveNavItemTarget(item))
}

// ── User menu ─────────────────────────────────────────────────────────────────
async function handleLogout() {
  await sessionStore.logout()
  router.push('/login')
}

function handleUserMenuCommand(command: string) {
  if (command === 'logout') {
    void handleLogout()
  }
}

// ── Locale / brand ────────────────────────────────────────────────────────────
function handleLocaleChange(locale: string) {
  void appStore.setLocale(resolveAppLocale(locale))
}

function handleBrandChange(brand: BrandPreset) {
  appStore.setBrand(brand)
}
</script>

<template>
  <div class="management-shell">
    <header class="shell-header">
      <div class="header-brand">
        <BrandLogo
          :brand="appStore.brand"
          :size="40"
          show-wordmark
          :aria-label="brandLabel"
        />
        <div class="title-block">
          <h1 class="app-title">{{ t('app.title') }}</h1>
        </div>
      </div>
      <div class="header-actions">
        <AppSearchSelect
          class="brand-switcher"
          :model-value="appStore.brand"
          :options="brandOptions"
          :aria-label="t('login.brandLabel')"
          @update:model-value="handleBrandChange"
        />
        <el-select
          class="locale-switcher"
          size="small"
          :model-value="appStore.locale"
          :aria-label="t('common.language')"
          @update:model-value="handleLocaleChange"
        >
          <el-option
            v-for="option in localeOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>

        <el-dropdown trigger="click" @command="handleUserMenuCommand">
          <span class="user-menu-trigger">
            {{ sessionStore.session?.displayName }}
            <el-icon class="user-menu-chevron"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout" :icon="SwitchButton">
                {{ t('nav.logout') }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <SessionLimitReminder v-if="reminderVisible" @action="handleSessionReminderAction" />

    <div class="shell-body">
      <aside class="shell-nav" :class="{ 'shell-nav--collapsed': navCollapsed }">
        <nav :aria-label="t('nav.managementNavigation')">
          <section
            v-for="group in navGroups"
            :key="group.id"
            class="nav-group"
          >
            <h2 v-if="!navCollapsed" class="nav-group-label">{{ t(group.labelKey) }}</h2>
            <template v-for="item in group.items" :key="item.id">
              <el-tooltip
                v-if="navCollapsed"
                :content="t(item.labelKey)"
                placement="right"
                :show-after="200"
              >
                <button
                  type="button"
                  class="nav-item nav-item--icon-only"
                  :class="{ active: isNavItemActive(item) }"
                  :aria-label="t(item.labelKey)"
                  @click="navigateToItem(item)"
                >
                  <el-icon v-if="getNavIcon(item.id)">
                    <component :is="getNavIcon(item.id)" />
                  </el-icon>
                </button>
              </el-tooltip>
              <button
                v-else
                type="button"
                class="nav-item"
                :class="{ active: isNavItemActive(item) }"
                @click="navigateToItem(item)"
              >
                <el-icon v-if="getNavIcon(item.id)" class="nav-item__icon">
                  <component :is="getNavIcon(item.id)" />
                </el-icon>
                <span class="nav-item__label">{{ t(item.labelKey) }}</span>
              </button>
            </template>
          </section>
        </nav>

        <div class="nav-collapse-toggle">
          <el-tooltip
            :content="navCollapsed ? t('nav.expandSidebar') : t('nav.collapseSidebar')"
            placement="right"
          >
            <button
              type="button"
              class="collapse-btn"
              :aria-label="navCollapsed ? t('nav.expandSidebar') : t('nav.collapseSidebar')"
              @click="toggleCollapsed"
            >
              <el-icon>
                <ArrowLeft v-if="!navCollapsed" />
                <ArrowRight v-else />
              </el-icon>
            </button>
          </el-tooltip>
        </div>
      </aside>

      <main class="shell-content">
        <AppBreadcrumb v-if="breadcrumbSegments.length > 0" />
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
  background: var(--surface-page);
}

.shell-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: 0.65rem var(--space-6);
  border-top: 3px solid var(--brand-primary);
  border-bottom: 1px solid var(--border-default);
  background: var(--surface-card);
  box-shadow: var(--shadow-sm);
}

.header-brand {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.title-block {
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
}

.app-title {
  margin: 0;
  font-size: 1.0625rem;
  font-weight: 650;
  line-height: 1.2;
  letter-spacing: -0.02em;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

// User menu trigger — styled to look like a label + chevron affordance
.user-menu-trigger {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
  font-weight: 600;
  cursor: pointer;
  padding: 0.3rem 0.5rem;
  border-radius: var(--radius-sm);
  transition: background-color var(--transition-base);

  &:hover {
    background: color-mix(in srgb, var(--brand-primary) 6%, var(--surface-card));
  }

  &:focus-visible {
    outline: 2px solid var(--brand-primary);
    outline-offset: 2px;
  }
}

.user-menu-chevron {
  font-size: 0.75rem;
  color: var(--text-tertiary);
}

.locale-switcher {
  width: 140px;
}

.shell-body {
  display: flex;
  flex: 1;
  min-height: 0;
}

// ── Sidebar ──────────────────────────────────────────────────────────────────
.shell-nav {
  width: 260px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  padding: var(--space-4) 0 var(--space-2);
  border-right: 1px solid var(--border-default);
  background: var(--surface-card);
  transition: width var(--transition-base);

  &.shell-nav--collapsed {
    width: 64px;
  }
}

nav {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  padding: 0 0.75rem;

  .shell-nav--collapsed & {
    padding: 0 0.5rem;
    gap: 0.75rem;
  }
}

.nav-group-label {
  margin: 0 0 var(--space-2) var(--space-4);
  font-size: var(--font-size-xs);
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--gray-500);
  white-space: nowrap;
  overflow: hidden;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  width: 100%;
  padding: 0.6rem var(--space-4);
  border: none;
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  background: transparent;
  text-align: left;
  font: inherit;
  font-size: var(--font-size-base);
  font-weight: 500;
  color: var(--text-primary);
  cursor: pointer;
  transition:
    background-color var(--transition-base),
    color var(--transition-base),
    box-shadow var(--transition-base);

  &:hover {
    background: color-mix(in srgb, var(--brand-primary) 6%, var(--surface-card));
  }

  &.active {
    background: color-mix(in srgb, var(--brand-primary) 10%, var(--surface-card));
    color: var(--brand-primary);
    font-weight: 650;
    box-shadow: inset 3px 0 0 var(--brand-primary);
  }

  &:focus-visible {
    outline: 2px solid var(--brand-primary);
    outline-offset: 2px;
  }

  &--icon-only {
    justify-content: center;
    padding: 0.6rem;
    border-radius: var(--radius-sm);
    box-shadow: none;

    &.active {
      box-shadow: none;
      border: 1.5px solid color-mix(in srgb, var(--brand-primary) 40%, transparent);
    }
  }
}

.nav-item__icon {
  flex-shrink: 0;
  font-size: 1rem;
}

.nav-item__label {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

// ── Collapse toggle ───────────────────────────────────────────────────────────
.nav-collapse-toggle {
  display: flex;
  justify-content: flex-end;
  padding: var(--space-2) var(--space-3) var(--space-2);
  border-top: 1px solid var(--border-default);

  .shell-nav--collapsed & {
    justify-content: center;
  }
}

.collapse-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  padding: 0;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  transition:
    background-color var(--transition-base),
    color var(--transition-base);

  &:hover {
    background: color-mix(in srgb, var(--brand-primary) 6%, var(--surface-card));
    color: var(--brand-primary);
    border-color: color-mix(in srgb, var(--brand-primary) 40%, transparent);
  }

  &:focus-visible {
    outline: 2px solid var(--brand-primary);
    outline-offset: 2px;
  }
}

.shell-content {
  flex: 1;
  min-width: 0;
  overflow: auto;
  background: var(--surface-page);
}
</style>
