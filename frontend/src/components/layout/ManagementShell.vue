<script setup lang="ts">
import AppBreadcrumb from '@/components/layout/AppBreadcrumb.vue'
import CommandPalette from '@/components/layout/CommandPalette.vue'
import ManagementShellHeader from '@/components/layout/ManagementShellHeader.vue'
import ManagementShellNav from '@/components/layout/ManagementShellNav.vue'
import OnboardingTour from '@/components/layout/OnboardingTour.vue'
import SessionLimitReminder from '@/components/session/SessionLimitReminder.vue'
import { useManagementShell } from '@/components/layout/useManagementShell'

const {
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
} = useManagementShell()
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
