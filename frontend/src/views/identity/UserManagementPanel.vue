<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import ScopedGroupSelect from '@/components/common/ScopedGroupSelect.vue'
import { rowSortMethod } from '@/composables/useDataTableFilters'
import { useScopedGroupOptions } from '@/composables/useScopedGroupOptions'
import { assignableGroupCodes, assignableRoles, canDeleteUsers } from '@/auth/identityRoles'
import { useIdentityStore } from '@/stores/identity'
import { useSessionStore } from '@/stores/session'
import type {
  CreateUserRequest,
  ManagementRole,
  ManagementUserView,
  UpdateUserRequest,
} from '@/types/identity'

const { t, te } = useI18n()
const identityStore = useIdentityStore()
const sessionStore = useSessionStore()

const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<string | null>(null)
const formRef = ref<FormInstance>()

const resetDialogVisible = ref(false)
const resetTargetId = ref<string | null>(null)
const resetForm = reactive({ newPassword: '' })
const resetFormRef = ref<FormInstance>()

const filterGroup = ref('')
const filterRole = ref('')
const currentPage = ref(1)

const form = reactive<{
  username: string
  displayName: string
  email: string
  initialPassword: string
  roles: ManagementRole[]
  authorizedGroupCodes: string[]
}>({
  username: '',
  displayName: '',
  email: '',
  initialPassword: '',
  roles: [],
  authorizedGroupCodes: [],
})

const actorRoles = computed(() => sessionStore.session?.roles ?? [])
const canDelete = computed(() => canDeleteUsers(actorRoles.value))
const roleOptions = computed(() => assignableRoles(actorRoles.value))
const groupCatalog = computed(() => identityStore.groups.map((group) => group.groupCode))
const groupOptions = computed(() => assignableGroupCodes(sessionStore.session, groupCatalog.value))
const {
  isGroupLocked: isFilterGroupLocked,
  resolveDefaultGroupCode,
  ensureGroupCatalog,
} = useScopedGroupOptions()

function roleLabel(role: string): string {
  return te(`identity.roles.${role}`) ? t(`identity.roles.${role}`) : role
}

const users = computed(() => identityStore.users)

const errorMessage = computed(() => {
  const key = identityStore.lastUserErrorMessageKey
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('identity.error.loadUsers')
})

const passwordValidator = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
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

const formRules = computed<FormRules>(() => ({
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
}))

const resetRules = computed<FormRules>(() => ({
  newPassword: [{ validator: passwordValidator, trigger: 'blur' }],
}))

onMounted(() => {
  void initializePanel()
})

async function initializePanel() {
  await ensureGroupCatalog()
  filterGroup.value = resolveDefaultGroupCode(filterGroup.value)
  await reload()
}

watch(currentPage, (page) => {
  void identityStore.fetchUsers({
    group: filterGroup.value || undefined,
    role: filterRole.value || undefined,
    page: page - 1,
    size: identityStore.userFilters.size,
  }).catch(() => {
    // Surfaced via store error key.
  })
})

async function reload() {
  currentPage.value = 1
  try {
    await identityStore.fetchUsers({
      group: filterGroup.value || undefined,
      role: filterRole.value || undefined,
      page: 0,
    })
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
  dialogMode.value = 'create'
  editingId.value = null
  form.username = ''
  form.displayName = ''
  form.email = ''
  form.initialPassword = ''
  form.roles = []
  form.authorizedGroupCodes = []
  dialogVisible.value = true
}

function openEdit(user: ManagementUserView) {
  dialogMode.value = 'edit'
  editingId.value = user.id
  form.username = user.username
  form.displayName = user.displayName
  form.email = user.email
  form.initialPassword = ''
  form.roles = [...user.roles]
  form.authorizedGroupCodes = [...user.authorizedGroupCodes]
  dialogVisible.value = true
}

async function submitForm() {
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  try {
    if (dialogMode.value === 'create') {
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
    dialogVisible.value = false
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
  resetForm.newPassword = ''
  resetDialogVisible.value = true
}

async function submitResetPassword() {
  if (!resetFormRef.value || !resetTargetId.value) {
    return
  }
  const valid = await resetFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  try {
    await identityStore.resetUserPassword(resetTargetId.value, resetForm.newPassword)
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

const sortUsersByEnabled = rowSortMethod<ManagementUserView>((row) => row.enabled)
</script>

<template>
  <section class="user-panel">
    <header class="panel-header">
      <form class="filters" @submit.prevent="reload">
        <ScopedGroupSelect
          v-model="filterGroup"
          class="filter-control"
          :clearable="!isFilterGroupLocked"
          :placeholder="t('identity.users.filters.groupPlaceholder')"
        />
        <AppSearchSelect
          v-model="filterRole"
          class="filter-control"
          clearable
          :placeholder="t('identity.users.filters.rolePlaceholder')"
        >
          <el-option
            v-for="role in roleOptions"
            :key="role"
            :label="roleLabel(role)"
            :value="role"
          />
        </AppSearchSelect>
        <el-button native-type="submit">{{ t('identity.users.filters.apply') }}</el-button>
        <el-button text @click="resetFilters">{{ t('identity.users.filters.reset') }}</el-button>
      </form>
      <el-button type="primary" @click="openCreate">{{ t('identity.users.create') }}</el-button>
    </header>

    <el-alert
      v-if="errorMessage"
      class="panel-alert"
      type="error"
      :title="errorMessage"
      show-icon
      :closable="false"
    />

    <el-skeleton v-if="identityStore.loadingUsers" :rows="6" animated />

    <template v-else>
      <template v-if="users.length > 0">
        <AppDataTable :data="users">
      <el-table-column prop="username" sortable width="140" :label="t('identity.users.columns.username')" />
      <el-table-column prop="displayName" sortable min-width="160" :label="t('identity.users.columns.displayName')" />
      <el-table-column prop="email" sortable min-width="200" :label="t('identity.users.columns.email')" />
      <el-table-column min-width="200" :label="t('identity.users.columns.roles')">
        <template #default="{ row }">
          <el-tag v-for="role in row.roles" :key="role" class="role-tag" size="small">
            {{ roleLabel(role) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column min-width="160" :label="t('identity.users.columns.groups')">
        <template #default="{ row }">
          {{ row.authorizedGroupCodes.join(', ') }}
        </template>
      </el-table-column>
      <el-table-column
        sortable
        :sort-method="sortUsersByEnabled"
        width="120"
        :label="t('identity.users.columns.status')"
      >
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
            {{ row.enabled ? t('identity.status.enabled') : t('identity.status.disabled') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column :label="t('identity.users.columns.actions')" width="160">
        <template #default="{ row }">
          <el-button link size="small" type="primary" @click="openEdit(row)">
            {{ t('identity.users.edit') }}
          </el-button>
          <el-dropdown trigger="click" @command="(command: UserMoreAction) => handleMoreAction(command, row)">
            <el-button link size="small">
              {{ t('common.more') }}
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="toggleEnabled">
                  {{ row.enabled ? t('identity.users.disable') : t('identity.users.enable') }}
                </el-dropdown-item>
                <el-dropdown-item command="resetPassword">
                  {{ t('identity.users.resetPassword') }}
                </el-dropdown-item>
                <el-dropdown-item
                  v-if="canDelete"
                  command="delete"
                  class="delete-action"
                >
                  {{ t('identity.users.delete') }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
      </AppDataTable>

        <AppTablePagination
          v-model:current-page="currentPage"
          :page-size="identityStore.userFilters.size ?? 20"
          :total="identityStore.usersTotal"
        />
      </template>

      <EmptyStatePanel v-else title-key="identity.users.empty" />
    </template>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? t('identity.users.createTitle') : t('identity.users.editTitle')"
      width="560px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-position="top">
        <el-form-item
          v-if="dialogMode === 'create'"
          :label="t('identity.users.form.username')"
          prop="username"
        >
          <el-input
            v-model="form.username"
            maxlength="8"
            :placeholder="t('identity.users.form.usernamePlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="t('identity.users.form.displayName')" prop="displayName">
          <el-input v-model="form.displayName" maxlength="128" />
        </el-form-item>
        <el-form-item :label="t('identity.users.form.email')" prop="email">
          <el-input v-model="form.email" maxlength="256" />
        </el-form-item>
        <el-form-item
          v-if="dialogMode === 'create'"
          :label="t('identity.users.form.initialPassword')"
          prop="initialPassword"
        >
          <el-input
            v-model="form.initialPassword"
            type="password"
            show-password
            maxlength="128"
          />
          <div class="field-hint">{{ t('identity.users.form.passwordHint') }}</div>
        </el-form-item>
        <el-form-item :label="t('identity.users.form.roles')" prop="roles">
          <AppSearchSelect
            v-model="form.roles"
            multiple
            class="full-width"
            :placeholder="t('identity.users.form.rolesPlaceholder')"
          >
            <el-option
              v-for="role in roleOptions"
              :key="role"
              :label="roleLabel(role)"
              :value="role"
            />
          </AppSearchSelect>
        </el-form-item>
        <el-form-item
          :label="t('identity.users.form.authorizedGroupCodes')"
          prop="authorizedGroupCodes"
        >
          <AppSearchSelect
            v-model="form.authorizedGroupCodes"
            multiple
            class="full-width"
            :placeholder="t('identity.users.form.authorizedGroupCodesPlaceholder')"
          >
            <el-option
              v-for="code in groupOptions"
              :key="code"
              :label="code"
              :value="code"
            />
          </AppSearchSelect>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('identity.actions.cancel') }}</el-button>
        <el-button type="primary" :loading="identityStore.submitting" @click="submitForm">
          {{ t('identity.actions.save') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="resetDialogVisible"
      :title="t('identity.users.resetPasswordTitle')"
      width="480px"
      destroy-on-close
    >
      <el-form ref="resetFormRef" :model="resetForm" :rules="resetRules" label-position="top">
        <el-form-item :label="t('identity.users.form.newPassword')" prop="newPassword">
          <el-input
            v-model="resetForm.newPassword"
            type="password"
            show-password
            maxlength="128"
          />
          <div class="field-hint">{{ t('identity.users.form.passwordHint') }}</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetDialogVisible = false">{{ t('identity.actions.cancel') }}</el-button>
        <el-button type="primary" :loading="identityStore.submitting" @click="submitResetPassword">
          {{ t('identity.actions.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped lang="scss">
.user-panel {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.filters {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex: 1;
  flex-wrap: wrap;
}

.filter-control {
  flex: 1 1 12rem;
  min-width: 0;
  max-width: 20rem;
}

.panel-alert {
  margin-bottom: 0.5rem;
}

.role-tag {
  margin-right: 0.25rem;
}

.full-width {
  width: 100%;
}

.field-hint {
  margin-top: 0.35rem;
  color: var(--text-muted);
  font-size: 0.8125rem;
}
</style>
