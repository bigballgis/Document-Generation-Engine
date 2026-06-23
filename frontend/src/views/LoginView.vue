<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useSessionStore } from '@/stores/session'
import type { BrandPreset } from '@/theme/tokens'

const { t, te } = useI18n()
const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const sessionStore = useSessionStore()

const form = reactive({
  username: '',
  password: '',
})

const errorMessageKey = ref<string | null>(null)
const submitting = ref(false)

const brandOptions = computed(() => [
  { value: 'REDBC' as BrandPreset, label: t('brand.redbc') },
  { value: 'GREENBC' as BrandPreset, label: t('brand.greenbc') },
])

const errorMessage = computed(() => {
  if (!errorMessageKey.value) {
    return ''
  }
  return te(errorMessageKey.value) ? t(errorMessageKey.value) : t('login.errorGeneric')
})

async function submitLogin() {
  errorMessageKey.value = null
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
      <div class="brand-slot" :aria-label="appStore.brand">
        {{ appStore.brand }}
      </div>
      <h1>{{ t('app.title') }}</h1>
      <p>{{ t('login.subtitle') }}</p>
    </header>

    <el-card class="login-card" shadow="never">
      <h2>{{ t('login.title') }}</h2>
      <el-alert
        v-if="errorMessage"
        class="login-alert"
        type="error"
        :title="errorMessage"
        show-icon
        :closable="false"
      />
      <el-form label-position="top" @submit.prevent="submitLogin">
        <el-form-item :label="t('login.username')">
          <el-input
            v-model="form.username"
            autocomplete="username"
            maxlength="8"
            placeholder="10000001"
          />
        </el-form-item>
        <el-form-item :label="t('login.password')">
          <el-input
            v-model="form.password"
            type="password"
            autocomplete="current-password"
            show-password
          />
        </el-form-item>
        <el-form-item :label="t('login.brandLabel')">
          <el-select
            :model-value="appStore.brand"
            @update:model-value="appStore.setBrand($event as BrandPreset)"
          >
            <el-option
              v-for="option in brandOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
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
  background: var(--surface-bg);
}

.login-header {
  text-align: center;

  h1 {
    margin: 0.5rem 0 0;
    font-size: 1.75rem;
    font-weight: 600;
  }

  p {
    margin: 0.25rem 0 0;
    color: var(--text-muted);
  }
}

.brand-slot {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 7rem;
  padding: 0.5rem 1rem;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  font-weight: 700;
  color: var(--brand-primary);
  background: var(--brand-header-bg);
}

.login-card {
  width: min(420px, 100%);

  h2 {
    margin: 0 0 1rem;
    font-size: 1.25rem;
  }
}

.login-alert {
  margin-bottom: 1rem;
}

.submit-btn {
  width: 100%;
  margin-top: 0.5rem;
}
</style>
