<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { assignableGroupCodes, assignableRoles, canDeleteUsers, canManageUsers } from '@/auth/identityRoles'
import { useRouteScopedAbortController } from '@/composables/useRouteScopedAbortController'
import { useScopedGroupOptions } from '@/composables/useScopedGroupOptions'
import UserFormDialog, { type UserFormState } from '@/views/identity/UserFormDialog.vue'
import UserManagementListSection from '@/views/identity/UserManagementListSection.vue'
import UserResetPasswordDialog from '@/views/identity/UserResetPasswordDialog.vue'
import { useIdentityStore } from '@/stores/identity'
import { useSessionStore } from '@/stores/session'
import type {
  CreateUserRequest,
  ManagementUserView,
  UpdateUserRequest,
} from '@/types/identity'

const { t, te } = useI18n()
const identityStore = useIdentityStore()
const sessionStore = useSessionStore()
const { signal, renewController } = useRouteScopedAbortController()

const formDialogVisible = ref(false)
const formDialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<string | null>(null)
const formDialogRef = ref<InstanceType<typeof UserFormDialog> | null>(null)

const resetDialogVisible = ref(false)
const resetTargetId = ref<string | null>(null)

const filterGroup = ref('')
const filterRole = ref('')
const currentPage = ref(1)

const form = reactive<UserFormState>({
  username: '',
  displayName: '',
  email: '',
  initialPassword: '',
  roles: [],
  authorizedGroupCodes: [],
})

const actorRoles = computed(() => sessionStore.session?.roles ?? [])
const canDelete = computed(() => canDeleteUsers(actorRoles.value))
const canCreate = computed(() => canManageUsers(actorRoles.value))
const roleOptions = computed(() => assignableRoles(actorRoles.value))
const groupCatalog = computed(() => identityStore.groups.map((group) => group.groupCode))
const groupOptions = computed(() => assignableGroupCodes(sessionStore.session, groupCatalog.value))
const {
  isGroupLocked: isFilterGroupLocked,
  resolveDefaultGroupCode,
  ensureGroupCatalog,
} = useScopedGroupOptions()

const users = computed(() => identityStore.users)

const errorMessageKey = computed(() => identityStore.lastUserErrorMessageKey)

const errorMessage = computed(() => {
  const key = identityStore.lastUserErrorMessageKey
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('identity.error.loadUsers')
})

onMounted(() => {
  void initializePanel()
})

async function initializePanel() {
  await ensureGroupCatalog()
  filterGroup.value = resolveDefaultGroupCode(filterGroup.value)
  await reload()
}

watch(currentPage, (page) => {
  void fetchPage(page - 1)
})

async function fetchPage(page: number) {
  try {
    await identityStore.fetchUsers(
      {
        group: filterGroup.value || undefined,
        role: filterRole.value || undefined,
        page,
        size: identityStore.userFilters.size,
      },
      { signal: signal.value },
    )
  } catch {
    // Surfaced via store error key.
  }
}

async function reload() {
  renewController()
  currentPage.value = 1
  try {
    await identityStore.fetchUsers(
      {
        group: filterGroup.value || undefined,
        role: filterRole.value || undefined,
        page: 0,
      },
      { signal: signal.value },
    )
  } catch {
    // Surfaced via store error key.
  }
}

function resetFilters() {
  filterGroup.value = isFilterGroupLocked.value ? resolveDefaultGroupCode() : ''
  filterRole.value = ''
  void reload()
}

function openCreate() {
  formDialogMode.value = 'create'
  editingId.value = null
  form.username = ''
  form.displayName = ''
  form.email = ''
  form.initialPassword = ''
  form.roles = []
  form.authorizedGroupCodes = []
  formDialogVisible.value = true
}

function openEdit(user: ManagementUserView) {
  formDialogMode.value = 'edit'
  editingId.value = user.id
  form.username = user.username
  form.displayName = user.displayName
  form.email = user.email
  form.initialPassword = ''
  form.roles = [...user.roles]
  form.authorizedGroupCodes = [...user.authorizedGroupCodes]
  formDialogVisible.value = true
}

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

type UserMoreAction = 'toggleEnabled' | 'resetPassword' | 'delete'

function handleMoreAction(command: UserMoreAction, user: ManagementUserView) {
  switch (command) {
    case 'toggleEnabled':
      void toggleEnabled(user)
      break
    case 'resetPassword':
      openResetPassword(user)
      break
    case 'delete':
      void confirmDelete(user)
      break
    default:
      break
  }
}

defineExpose({
  openCreate,
  submitForm: () => formDialogRef.value?.submitForm(),
  form,
})
</script>

<template>
  <section class="user-panel">
    <UserManagementListSection
      v-model:filter-group="filterGroup"
      v-model:filter-role="filterRole"
      v-model:current-page="currentPage"
      :users="users"
      :loading="identityStore.loadingUsers"
      :error-message-key="errorMessageKey"
      :error-retryable="identityStore.lastUserErrorRetryable"
      :is-filter-group-locked="isFilterGroupLocked"
      :role-options="roleOptions"
      :can-create="canCreate"
      :can-delete="canDelete"
      :page-size="identityStore.userFilters.size ?? 20"
      :total-users="identityStore.usersTotal"
      @reload="reload"
      @reset-filters="resetFilters"
      @create="openCreate"
      @edit="openEdit"
      @more-action="handleMoreAction"
    />

    <UserFormDialog
      ref="formDialogRef"
      v-model:visible="formDialogVisible"
      v-model:form="form"
      :mode="formDialogMode"
      :role-options="roleOptions"
      :group-options="groupOptions"
      :submitting="identityStore.submitting"
      @submit="submitForm"
    />

    <UserResetPasswordDialog
      v-model:visible="resetDialogVisible"
      :submitting="identityStore.submitting"
      @submit="submitResetPassword"
    />
  </section>
</template>

<style scoped lang="scss">
.user-panel {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
</style>
