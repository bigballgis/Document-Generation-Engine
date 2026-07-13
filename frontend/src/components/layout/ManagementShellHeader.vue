<script setup lang="ts">
import { ArrowDown, QuestionFilled, SwitchButton } from '@element-plus/icons-vue'
import BrandLogo from '@/components/branding/BrandLogo.vue'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import NotificationBell from '@/components/layout/NotificationBell.vue'
import type { BrandPreset } from '@/theme/tokens'

defineProps<{
  brand: BrandPreset
  brandLabel: string
  appTitle: string
  brandOptions: Array<{ value: BrandPreset; label: string }>
  locale: string
  localeOptions: Array<{ value: string; label: string }>
  displayName: string | undefined
  brandAriaLabel: string
  languageAriaLabel: string
  helpMenuAriaLabel: string
  helpMenuLabel: string
  helpReplayLabel: string
  helpReplayDisabled: boolean
  logoutLabel: string
}>()

const emit = defineEmits<{
  'brand-change': [brand: BrandPreset]
  'locale-change': [locale: string]
  'help-command': [command: string]
  'user-command': [command: string]
}>()
</script>

<template>
  <header class="shell-header">
    <div class="header-brand">
      <BrandLogo
        :brand="brand"
        :size="40"
        show-wordmark
        :aria-label="brandLabel"
      />
      <div class="title-block">
        <h1 class="app-title">{{ appTitle }}</h1>
      </div>
    </div>
    <div class="header-actions">
      <AppSearchSelect
        class="brand-switcher"
        :model-value="brand"
        :options="brandOptions"
        :aria-label="brandAriaLabel"
        @update:model-value="emit('brand-change', $event as BrandPreset)"
      />
      <el-select
        class="locale-switcher"
        size="small"
        :model-value="locale"
        :aria-label="languageAriaLabel"
        @update:model-value="emit('locale-change', String($event))"
      >
        <el-option
          v-for="option in localeOptions"
          :key="option.value"
          :label="option.label"
          :value="option.value"
        />
      </el-select>

      <NotificationBell />

      <el-dropdown
        trigger="click"
        data-testid="help-menu"
        @command="emit('help-command', String($event))"
      >
        <button
          type="button"
          class="help-menu-trigger"
          data-testid="help-menu-trigger"
          :aria-label="helpMenuAriaLabel"
        >
          <el-icon class="help-menu-icon"><QuestionFilled /></el-icon>
          <span class="help-menu-label">{{ helpMenuLabel }}</span>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item
              command="replay-tour"
              data-testid="help-menu-replay-tour"
              :disabled="helpReplayDisabled"
            >
              {{ helpReplayLabel }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>

      <el-dropdown trigger="click" @command="emit('user-command', String($event))">
        <span class="user-menu-trigger">
          {{ displayName }}
          <el-icon class="user-menu-chevron"><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="logout" :icon="SwitchButton">
              {{ logoutLabel }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<style scoped lang="scss" src="./ManagementShellHeader.scss"></style>
