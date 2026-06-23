<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import BrandLogo from '@/components/branding/BrandLogo.vue'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import { BRAND_REGISTRY } from '@/config/brands'
import { LOCALE_REGISTRY, resolveAppLocale } from '@/i18n/localeRegistry'
import { buildVisibleNavGroups } from '@/navigation/navStructure'
import { useAppStore } from '@/stores/app'
import { useSessionStore } from '@/stores/session'
import type { BrandPreset } from '@/theme/tokens'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const sessionStore = useSessionStore()

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
  return buildVisibleNavGroups(session.visibleRoutes)
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
        <span class="user-label">{{ sessionStore.session?.displayName }}</span>
        <el-button type="primary" plain @click="handleLogout">
          {{ t('nav.logout') }}
        </el-button>
      </div>
    </header>

    <div class="shell-body">
      <aside class="shell-nav">
        <nav :aria-label="t('nav.managementNavigation')">
          <section
            v-for="group in navGroups"
            :key="group.id"
            class="nav-group"
          >
            <h2 class="nav-group-label">{{ t(group.labelKey) }}</h2>
            <button
              v-for="item in group.items"
              :key="item.id"
              type="button"
              class="nav-item"
              :class="{ active: isActive(item.path) }"
              @click="navigate(item.path)"
            >
              {{ t(item.labelKey) }}
            </button>
          </section>
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
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 0.65rem 1.5rem;
  border-bottom: 1px solid var(--brand-header-border);
  background: linear-gradient(
    180deg,
    color-mix(in srgb, var(--brand-header-bg) 88%, white) 0%,
    var(--brand-header-bg) 100%
  );
  box-shadow: var(--shadow-soft);

  &::after {
    content: '';
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    height: 2px;
    background: linear-gradient(
      90deg,
      var(--brand-primary) 0%,
      color-mix(in srgb, var(--brand-primary) 35%, transparent) 42%,
      transparent 72%
    );
    opacity: 0.85;
    pointer-events: none;
  }
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

.user-label {
  font-size: 0.875rem;
  color: var(--text-muted);
  font-weight: 600;
}

.locale-switcher {
  width: 140px;
}

.shell-body {
  display: flex;
  flex: 1;
  min-height: 0;
}

.shell-nav {
  width: 260px;
  flex-shrink: 0;
  padding: 1rem 0;
  border-right: 1px solid var(--border-color);
  background: var(--nav-surface-bg);
}

nav {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
  padding: 0 0.75rem;
}

.nav-group-label {
  margin: 0 0 0.35rem 0.85rem;
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--text-muted);
}

.nav-item {
  display: block;
  width: 100%;
  padding: 0.6rem 0.85rem;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  text-align: left;
  font: inherit;
  font-size: 0.9375rem;
  font-weight: 500;
  color: var(--text-primary);
  cursor: pointer;
  transition:
    background-color 0.15s ease,
    color 0.15s ease;

  &:hover {
    background: color-mix(in srgb, var(--brand-accent-soft) 55%, white);
  }

  &.active {
    background: color-mix(in srgb, var(--brand-primary) 11%, white);
    color: var(--brand-primary);
    font-weight: 650;
    box-shadow: inset 3px 0 0 var(--brand-primary);
  }
}

.shell-content {
  flex: 1;
  min-width: 0;
  overflow: auto;
}
</style>
