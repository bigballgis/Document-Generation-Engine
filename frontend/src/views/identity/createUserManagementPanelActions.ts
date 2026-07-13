import { ElMessage, ElMessageBox } from 'element-plus'
import type { Ref } from 'vue'
import type {
  CreateUserRequest,
  ManagementUserView,
  UpdateUserRequest,
} from '@/types/identity'
import type { UserFormState } from '@/views/identity/UserFormDialog.vue'

type UserMoreAction = 'toggleEnabled' | 'resetPassword' | 'delete'

export function createUserManagementPanelActions(options: {
  t: (key: string, params?: Record<string, unknown>) => string
  errorMessage: { value: string }
  identityStore: {
    createUser: (payload: CreateUserRequest) => Promise<unknown>
    updateUser: (id: string, payload: UpdateUserRequest) => Promise<unknown>
    setUserEnabled: (id: string, enabled: boolean) => Promise<unknown>
    resetUserPassword: (id: string, password: string) => Promise<unknown>
    deleteUser: (id: string) => Promise<unknown>
  }
  form: UserFormState
  formDialogMode: Ref<'create' | 'edit'>
  formDialogVisible: Ref<boolean>
  editingId: Ref<string | null>
  resetDialogVisible: Ref<boolean>
  resetTargetId: Ref<string | null>
}) {
  const {
    t,
    errorMessage,
    identityStore,
    form,
    formDialogMode,
    formDialogVisible,
    editingId,
    resetDialogVisible,
    resetTargetId,
  } = options

  async function submitForm() {
    try {
      if (formDialogMode.value === 'create') {
        const payload: CreateUserRequest = {
          username: form.username,
          displayName: form.displayName,
          email: form.email,
          initialPassword: form.initialPassword,
          roles: form.roles,
          authorizedGroupCodes: form.authorizedGroupCodes,
        }
        await identityStore.createUser(payload)
        ElMessage.success(t('identity.users.createSuccess'))
      } else if (editingId.value) {
        const payload: UpdateUserRequest = {
          displayName: form.displayName,
          email: form.email,
          roles: form.roles,
          authorizedGroupCodes: form.authorizedGroupCodes,
        }
        await identityStore.updateUser(editingId.value, payload)
        ElMessage.success(t('identity.users.updateSuccess'))
      }
      formDialogVisible.value = false
    } catch {
      ElMessage.error(errorMessage.value || t('identity.error.createUser'))
    }
  }

  async function toggleEnabled(user: ManagementUserView) {
    try {
      if (user.enabled) {
        await ElMessageBox.confirm(
          t('identity.users.confirmDisableMessage', { username: user.username }),
          t('identity.users.confirmDisableTitle'),
          { type: 'warning' },
        )
      }
      await identityStore.setUserEnabled(user.id, !user.enabled)
      ElMessage.success(user.enabled ? t('identity.users.disableSuccess') : t('identity.users.enableSuccess'))
    } catch (error) {
      if (error === 'cancel') {
        return
      }
      ElMessage.error(errorMessage.value || t('identity.error.updateUser'))
    }
  }

  function openResetPassword(user: ManagementUserView) {
    resetTargetId.value = user.id
    resetDialogVisible.value = true
  }

  async function submitResetPassword(newPassword: string) {
    if (!resetTargetId.value) {
      return
    }
    try {
      await identityStore.resetUserPassword(resetTargetId.value, newPassword)
      ElMessage.success(t('identity.users.resetPasswordSuccess'))
      resetDialogVisible.value = false
    } catch {
      ElMessage.error(errorMessage.value || t('identity.error.resetPassword'))
    }
  }

  async function confirmDelete(user: ManagementUserView) {
    try {
      await ElMessageBox.confirm(
        t('identity.users.confirmDeleteMessage', { username: user.username }),
        t('identity.users.confirmDeleteTitle'),
        { type: 'warning' },
      )
    } catch {
      return
    }
    try {
      await identityStore.deleteUser(user.id)
      ElMessage.success(t('identity.users.deleteSuccess'))
    } catch {
      ElMessage.error(errorMessage.value || t('identity.error.deleteUser'))
    }
  }

  function handleMoreAction(command: UserMoreAction, user: ManagementUserView) {
    if (command === 'toggleEnabled') {
      void toggleEnabled(user)
    } else if (command === 'resetPassword') {
      openResetPassword(user)
    } else if (command === 'delete') {
      void confirmDelete(user)
    }
  }

  return {
    submitForm,
    submitResetPassword,
    handleMoreAction,
  }
}
