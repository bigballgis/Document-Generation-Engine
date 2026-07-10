<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormItemRule } from 'element-plus'
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
const passwordInlineError = ref('')
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

const rules = computed<Record<string, FormItemRule[]>>(() => ({
  username: [
    {
      validator: (_rule, value, callback) => {
        const trimmed = String(value ?? '').trim()
        if (!trimmed) {
          callback(new Error(t('login.validation.usernameRequired')))
          return
        }
        if (!/^\d{8}$/.test(trimmed)) {
          callback(new Error(t('login.validation.usernameFormat')))
          return
        }
        callback()
      },
      trigger: ['blur', 'change'],
    },
  ],
  password: [
    {
      required: true,
      message: t('login.validation.passwordRequired'),
      trigger: ['blur', 'change'],
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

function clearPasswordInlineError() {
  passwordInlineError.value = ''
}

async function submitLogin() {
  errorMessageKey.value = null
  passwordInlineError.value = ''
  form.username = form.username.trim()
  form.password = form.password.trim()
  // Edge-only trim: whitespace-only becomes empty → same required outcome as blank.
  if (!form.password) {
    passwordInlineError.value = t('login.validation.passwordRequired')
    return
  }
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitting.value = true
  try {
    await sessionStore.login(form.username, form.password)
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

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(320px, 42%) 1fr;

  @media (max-width: 900px) {
    grid-template-columns: 1fr;
  }
}

.login-brand-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-12) var(--space-8);
  background: linear-gradient(
    160deg,
    var(--brand-primary) 0%,
    color-mix(in srgb, var(--brand-primary) 82%, var(--gray-900)) 100%
  );
  color: var(--on-primary);
  transition: background var(--transition-base);

  @media (max-width: 900px) {
    min-height: 240px;
    padding: var(--space-8) var(--space-6);
  }
}

.login-brand-panel__content {
  max-width: 22rem;
  text-align: center;

  :deep(.brand-logo__wordmark) {
    color: var(--on-primary);
  }
}

.login-brand-panel__subtitle {
  margin: var(--space-6) 0 0;
  font-size: var(--font-size-base);
  line-height: 1.55;
  color: color-mix(in srgb, var(--on-primary) 88%, transparent);
}

.login-form-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-8);
  background: var(--surface-card);
}

.login-form-panel__inner {
  width: min(420px, 100%);
}

.login-form-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  margin-bottom: var(--space-6);

  h2 {
    margin: 0;
    font-size: var(--font-size-xl);
    font-weight: 650;
  }
}

.locale-switcher {
  width: 9rem;
}

.login-alert {
  margin-bottom: var(--space-4);
}

.submit-btn {
  width: 100%;
  margin-top: var(--space-2);
}
</style>
