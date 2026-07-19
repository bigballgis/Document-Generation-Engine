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
  FolderOpened,
  Lock,
} from '@element-plus/icons-vue'
import { useOnboardingTour } from '@/composables/useOnboardingTour'
import { useSessionRenewal } from '@/composables/useSessionRenewal'
import { BRAND_REGISTRY } from '@/config/brands'
import { LOCALE_REGISTRY, resolveAppLocale } from '@/i18n/localeRegistry'
import { buildBreadcrumbTrail } from '@/navigation/breadcrumbTrail'
import { buildVisibleNavGroups, resolveNavItemTarget, type NavItemDefinition } from '@/navigation/navStructure'
import { useAppStore } from '@/stores/app'
import { useSessionStore } from '@/stores/session'
import type { BrandPreset } from '@/theme/tokens'

const NAV_ICON_MAP: Record<string, Component> = {
  dashboard: HomeFilled,
  users: User,
  groups: UserFilled,
  masters: Postcard,
  templates: Document,
  'content-modules': Collection,
  'asset-library': FolderOpened,
  'api-policies': Connection,
  audit: Histogram,
  'legal-holds': Lock,
}

export function useManagementShell() {
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

  const { reminderVisible } = useSessionRenewal()

  async function handleSessionReminderAction() {
    await sessionStore.logout()
    await router.push({ name: 'login', query: { redirect: route.fullPath } })
  }

  const COLLAPSED_KEY = 'docgen.nav.collapsed'
  const navCollapsed = ref(localStorage.getItem(COLLAPSED_KEY) === 'true')

  function toggleCollapsed() {
    navCollapsed.value = !navCollapsed.value
    localStorage.setItem(COLLAPSED_KEY, String(navCollapsed.value))
  }

  function getNavIcon(itemId: string): Component | undefined {
    return NAV_ICON_MAP[itemId]
  }

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

  const navGroups = computed(() => {
    const session = sessionStore.session
    if (!session) {
      return []
    }
    return buildVisibleNavGroups(session.visibleRoutes, session.roles, session.capabilities)
  })

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

  async function handleLogout() {
    await sessionStore.logout()
    router.push('/login')
  }

  function handleUserMenuCommand(command: string) {
    if (command === 'logout') {
      void handleLogout()
    }
  }

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

  function handleLocaleChange(locale: string) {
    void appStore.setLocale(resolveAppLocale(locale))
  }

  function handleBrandChange(brand: BrandPreset) {
    appStore.setBrand(brand)
  }

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

  return {
    t,
    appStore,
    sessionStore,
    onboardingTourOpen,
    onboardingTourCurrent,
    onboardingTourSteps,
    onboardingTourCanReplay,
    onboardingTourTargetFor,
    dismissOnboardingTour,
    reminderVisible,
    handleSessionReminderAction,
    navCollapsed,
    toggleCollapsed,
    getNavIcon,
    brandLabel,
    localeOptions,
    brandOptions,
    navGroups,
    breadcrumbSegments,
    isNavItemActive,
    navigateToItem,
    handleUserMenuCommand,
    handleHelpMenuCommand,
    handleLocaleChange,
    handleBrandChange,
    mainContentRef,
    skipToMainContent,
  }
}
