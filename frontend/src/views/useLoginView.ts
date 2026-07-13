import { computed, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormItemRule } from 'element-plus'
import { BRAND_REGISTRY } from '@/config/brands'
import { LOCALE_REGISTRY, resolveAppLocale } from '@/i18n/localeRegistry'
import { useAppStore } from '@/stores/app'
import { useSessionStore } from '@/stores/session'
import type { BrandPreset } from '@/theme/tokens'

export function useLoginView() {
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

  return {
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
  }
}
