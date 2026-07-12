<script setup lang="ts">
import { computed, ref, type Component } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import {
  HomeFilled,
  Postcard,
  Document,
  Collection,
  Connection,
  Histogram,
  User,
  UserFilled,
} from '@element-plus/icons-vue'
import AppBreadcrumb from '@/components/layout/AppBreadcrumb.vue'
import CommandPalette from '@/components/layout/CommandPalette.vue'
import ManagementShellHeader from '@/components/layout/ManagementShellHeader.vue'
import ManagementShellNav from '@/components/layout/ManagementShellNav.vue'
import OnboardingTour from '@/components/layout/OnboardingTour.vue'
import SessionLimitReminder from '@/components/session/SessionLimitReminder.vue'
import { useOnboardingTour } from '@/composables/useOnboardingTour'
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

const {
  open: onboardingTourOpen,
  current: onboardingTourCurrent,
  tourSteps: onboardingTourSteps,
  canReplay: onboardingTourCanReplay,
  targetSelectorFor: onboardingTourTargetFor,
  dismiss: dismissOnboardingTour,
  replay: replayOnboardingTour,
} = useOnboardingTour({ autoOpenOnMount: true })

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

// ── Help menu (LR-C8 onboarding tour replay) ──────────────────────────────────
function handleHelpMenuCommand(command: string) {
  if (command !== 'replay-tour') {
    return
  }
  if (!onboardingTourCanReplay.value) {
    ElMessage.info(t('onboardingTour.help.replayUnavailable'))
    return
  }
  void replayOnboardingTour()
}

// ── Locale / brand ────────────────────────────────────────────────────────────
function handleLocaleChange(locale: string) {
  void appStore.setLocale(resolveAppLocale(locale))
}

function handleBrandChange(brand: BrandPreset) {
  appStore.setBrand(brand)
}

// ── Skip link (LR-C12) ────────────────────────────────────────────────────────
const mainContentRef = ref<HTMLElement | null>(null)

function skipToMainContent(event: Event) {
  event.preventDefault()
  const main = mainContentRef.value
  if (!main) {
    return
  }
  main.focus({ preventScroll: false })
  if (typeof main.scrollIntoView === 'function') {
    main.scrollIntoView({ block: 'start' })
  }
}
</script>

<template>
  <div class="management-shell">
    <a
      class="skip-link"
      href="#main-content"
      @click="skipToMainContent"
    >
      {{ t('nav.skipToMainContent') }}
    </a>

    <ManagementShellHeader
      :brand="appStore.brand"
      :brand-label="brandLabel"
      :app-title="t('app.title')"
      :brand-options="brandOptions"
      :locale="appStore.locale"
      :locale-options="localeOptions"
      :display-name="sessionStore.session?.displayName"
      :brand-aria-label="t('login.brandLabel')"
      :language-aria-label="t('common.language')"
      :help-menu-aria-label="t('onboardingTour.help.menuAriaLabel')"
      :help-menu-label="t('onboardingTour.help.menu')"
      :help-replay-label="t('onboardingTour.help.replay')"
      :help-replay-disabled="!onboardingTourCanReplay"
      :logout-label="t('nav.logout')"
      @brand-change="handleBrandChange"
      @locale-change="handleLocaleChange"
      @help-command="handleHelpMenuCommand"
      @user-command="handleUserMenuCommand"
    />

    <SessionLimitReminder v-if="reminderVisible" @action="handleSessionReminderAction" />

    <div class="shell-body">
      <ManagementShellNav
        :collapsed="navCollapsed"
        :nav-groups="navGroups"
        :navigation-aria-label="t('nav.managementNavigation')"
        :expand-label="t('nav.expandSidebar')"
        :collapse-label="t('nav.collapseSidebar')"
        :get-nav-icon="getNavIcon"
        :is-nav-item-active="isNavItemActive"
        :resolve-item-label="(labelKey) => t(labelKey)"
        @navigate="navigateToItem"
        @toggle-collapsed="toggleCollapsed"
      />

      <main
        id="main-content"
        ref="mainContentRef"
        class="shell-content"
        tabindex="-1"
      >
        <AppBreadcrumb v-if="breadcrumbSegments.length > 0" class="shell-breadcrumb" />
        <div class="shell-page-root">
          <slot />
        </div>
      </main>
    </div>

    <CommandPalette />

    <OnboardingTour
      v-model:open="onboardingTourOpen"
      v-model:current="onboardingTourCurrent"
      :steps="onboardingTourSteps"
      :target-for="onboardingTourTargetFor"
      @dismiss="dismissOnboardingTour"
    />
  </div>
</template>

<style scoped lang="scss">
.management-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--surface-page);
}

.skip-link {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 1000;
  padding: var(--space-2) var(--space-4);
  background: var(--surface-card);
  color: var(--brand-primary);
  font-weight: 650;
  text-decoration: none;
  border: 1px solid var(--border-default);
  border-radius: 0 0 var(--radius-sm) 0;
  transform: translateY(-120%);
  transition: transform var(--transition-base);

  &:focus,
  &:focus-visible {
    outline: var(--focus-ring-width) solid var(--focus-ring-color);
    outline-offset: var(--focus-ring-offset);
    transform: translateY(0);
  }
}

.shell-body {
  display: flex;
  flex: 1;
  min-height: 0;
}

.shell-content {
  flex: 1;
  min-width: 0;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
  background: var(--surface-page);

  &:focus {
    outline: none;
  }

  &:focus-visible {
    outline: var(--focus-ring-width) solid var(--focus-ring-color);
    outline-offset: calc(-1 * var(--focus-ring-offset));
  }
}

.shell-breadcrumb {
  flex: 0 0 auto;
}

.shell-page-root {
  flex: 1 1 auto;
  min-height: 0;
  min-width: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
}
</style>
