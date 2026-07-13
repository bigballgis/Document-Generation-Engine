<script setup lang="ts">
import BrandLogo from '@/components/branding/BrandLogo.vue'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import type { BrandPreset } from '@/theme/tokens'
import { useLoginView } from '@/views/useLoginView'

const {
  t,
  appStore,
  formRef,
  form,
  passwordInlineError,
  submitting,
  brandOptions,
  localeOptions,
  rules,
  errorMessage,
  handleLocaleChange,
  clearPasswordInlineError,
  submitLogin,
} = useLoginView()
</script>

<template>
  <div class="login-page">
    <aside class="login-brand-panel" :aria-label="t('login.brandAriaLabel')">
      <div class="login-brand-panel__content">
        <BrandLogo
          :brand="appStore.brand"
          :size="64"
          show-wordmark
          :aria-label="t('login.brandAriaLabel')"
        />
        <p class="login-brand-panel__subtitle">{{ t('login.subtitle') }}</p>
      </div>
    </aside>

    <main class="login-form-panel">
      <div class="login-form-panel__inner">
        <header class="login-form-header">
          <h2>{{ t('login.title') }}</h2>
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
        </header>

        <el-alert
          v-if="errorMessage"
          class="login-alert"
          type="error"
          :title="errorMessage"
          show-icon
          :closable="false"
        />

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          @submit.prevent="submitLogin"
        >
          <el-form-item :label="t('login.username')" prop="username">
            <el-input
              v-model="form.username"
              autocomplete="username"
              maxlength="8"
              :placeholder="t('login.usernamePlaceholder')"
              :aria-label="t('login.username')"
            />
          </el-form-item>
          <el-form-item :label="t('login.password')" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              autocomplete="current-password"
              show-password
              :aria-label="t('login.password')"
              @input="clearPasswordInlineError"
            />
            <p
              v-if="passwordInlineError"
              class="el-form-item__error"
              data-testid="login-password-required"
            >
              {{ passwordInlineError }}
            </p>
          </el-form-item>
          <el-form-item :label="t('login.brandLabel')">
            <AppSearchSelect
              :model-value="appStore.brand"
              @update:model-value="appStore.setBrand($event as BrandPreset)"
            >
              <el-option
                v-for="option in brandOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </AppSearchSelect>
          </el-form-item>
          <el-button
            type="primary"
            native-type="submit"
            class="submit-btn"
            :loading="submitting"
          >
            {{ t('login.submit') }}
          </el-button>
        </el-form>
      </div>
    </main>
  </div>
</template>

<style scoped lang="scss" src="./LoginView.scss"></style>
