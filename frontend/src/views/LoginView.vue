<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import BrandLogo from '@/components/branding/BrandLogo.vue'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import { BRAND_REGISTRY } from '@/config/brands'
import { LOCALE_REGISTRY, resolveAppLocale } from '@/i18n/localeRegistry'
import { useAppStore } from '@/stores/app'
import { useSessionStore } from '@/stores/session'
import type { BrandPreset } from '@/theme/tokens'

const { t, te } = useI18n()
const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const sessionStore = useSessionStore()

const formRef = ref<FormInstance>()
const form = reactive({
  username: '',
  password: '',
})

const errorMessageKey = ref<string | null>(null)
const submitting = ref(false)

const brandOptions = computed(() => [
  ...BRAND_REGISTRY.map((entry) => ({
    value: entry.code as BrandPreset,
    label: t(entry.labelKey),
  })),
])

const localeOptions = computed(() =>
  LOCALE_REGISTRY.map((entry) => ({
    value: entry.code,
    label: t(entry.labelKey),
  })),
)

const rules = computed<FormRules>(() => ({
  username: [
    {
      required: true,
      message: t('login.validation.usernameRequired'),
      trigger: 'blur',
    },
    {
      pattern: /^\d{8}$/,
      message: t('login.validation.usernameFormat'),
      trigger: 'blur',
    },
  ],
  password: [
    {
      required: true,
      message: t('login.validation.passwordRequired'),
      trigger: 'blur',
    },
  ],
}))

const sessionExpired = computed(() => route.query.sessionExpired === '1')

const errorMessage = computed(() => {
  if (sessionExpired.value) {
    return t('api.error.authentication.sessionExpired')
  }
  if (!errorMessageKey.value) {
    return ''
  }
  return te(errorMessageKey.value) ? t(errorMessageKey.value) : t('login.errorGeneric')
})

function handleLocaleChange(locale: string) {
  void appStore.setLocale(resolveAppLocale(locale))
}

async function submitLogin() {
  errorMessageKey.value = null
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    await sessionStore.login(form.username.trim(), form.password)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : null
    await router.replace(redirect ?? sessionStore.defaultHomePath())
  } catch (error) {
    errorMessageKey.value = sessionStore.loginErrorMessageKey(error)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <header class="login-header">
      <BrandLogo
        :brand="appStore.brand"
        :size="52"
        show-wordmark
        :aria-label="t('login.brandAriaLabel')"
      />
      <h1>{{ t('app.title') }}</h1>
      <p>{{ t('login.subtitle') }}</p>
    </header>

    <el-card class="login-card" shadow="never">
      <div class="login-card-header">
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
      </div>
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
          />
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
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1.5rem;
  padding: 2rem;
  background:
    radial-gradient(
      circle at 12% 8%,
      color-mix(in srgb, var(--brand-primary) 12%, transparent) 0%,
      transparent 42%
    ),
    linear-gradient(
      165deg,
      var(--brand-header-bg) 0%,
      var(--surface-bg) 48%,
      var(--surface-gradient-end) 100%
    );
}

.login-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;

  h1 {
    margin: 0.85rem 0 0;
    font-size: 1.75rem;
    font-weight: 650;
    letter-spacing: -0.02em;
  }

  p {
    margin: 0.35rem 0 0;
    font-size: 0.9375rem;
    color: var(--text-muted);
  }
}

.login-card {
  width: min(420px, 100%);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-card);

  h2 {
    margin: 0;
    font-size: 1.2rem;
    font-weight: 650;
  }
}

.login-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.locale-switcher {
  width: 9rem;
}

.login-alert {
  margin-bottom: 1rem;
}

.submit-btn {
  width: 100%;
  margin-top: 0.5rem;
}
</style>
