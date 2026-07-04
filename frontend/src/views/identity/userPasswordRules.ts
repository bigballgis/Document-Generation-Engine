import type { FormRules } from 'element-plus'

export function createPasswordValidator(t: (key: string) => string) {
  return (_rule: unknown, value: string, callback: (error?: Error) => void) => {
    if (!value) {
      callback(new Error(t('identity.users.validation.passwordRequired')))
      return
    }
    if (value.length < 12 || value.length > 128) {
      callback(new Error(t('identity.users.validation.passwordLength')))
      return
    }
    callback()
  }
}

export function createUserFormRules(t: (key: string) => string, passwordValidator: FormRules[string]): FormRules {
  return {
    username: [
      { required: true, message: t('identity.users.validation.usernameRequired'), trigger: 'blur' },
      {
        pattern: /^\d{8}$/,
        message: t('identity.users.validation.usernamePattern'),
        trigger: 'blur',
      },
    ],
    displayName: [
      { required: true, message: t('identity.users.validation.displayNameRequired'), trigger: 'blur' },
    ],
    email: [
      { required: true, message: t('identity.users.validation.emailRequired'), trigger: 'blur' },
      { type: 'email', message: t('identity.users.validation.emailInvalid'), trigger: 'blur' },
    ],
    initialPassword: [{ validator: passwordValidator, trigger: 'blur' }],
    roles: [
      {
        type: 'array',
        required: true,
        min: 1,
        message: t('identity.users.validation.rolesRequired'),
        trigger: 'change',
      },
    ],
    authorizedGroupCodes: [
      {
        type: 'array',
        required: true,
        min: 1,
        message: t('identity.users.validation.groupsRequired'),
        trigger: 'change',
      },
    ],
  }
}

export function createResetPasswordRules(passwordValidator: FormRules[string]): FormRules {
  return {
    newPassword: [{ validator: passwordValidator, trigger: 'blur' }],
  }
}
