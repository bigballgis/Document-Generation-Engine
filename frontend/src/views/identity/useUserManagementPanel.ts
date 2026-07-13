import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { assignableGroupCodes, assignableRoles, canDeleteUsers, canManageUsers } from '@/auth/identityRoles'
import { useRouteScopedAbortController } from '@/composables/useRouteScopedAbortController'
import { useScopedGroupOptions } from '@/composables/useScopedGroupOptions'
import UserFormDialog, { type UserFormState } from '@/views/identity/UserFormDialog.vue'
import { createUserManagementPanelActions } from '@/views/identity/createUserManagementPanelActions'
import { useIdentityStore } from '@/stores/identity'
import { useSessionStore } from '@/stores/session'
import type { ManagementUserView } from '@/types/identity'

export function useUserManagementPanel() {
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

  const { submitForm, submitResetPassword, handleMoreAction } = createUserManagementPanelActions({
    t,
    errorMessage,
    identityStore,
    form,
    formDialogMode,
    formDialogVisible,
    editingId,
    resetDialogVisible,
    resetTargetId,
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
    Object.assign(form, {
      username: '',
      displayName: '',
      email: '',
      initialPassword: '',
      roles: [],
      authorizedGroupCodes: [],
    })
    formDialogVisible.value = true
  }

  function openEdit(user: ManagementUserView) {
    formDialogMode.value = 'edit'
    editingId.value = user.id
    Object.assign(form, {
      username: user.username,
      displayName: user.displayName,
      email: user.email,
      initialPassword: '',
      roles: [...user.roles],
      authorizedGroupCodes: [...user.authorizedGroupCodes],
    })
    formDialogVisible.value = true
  }

  return {
    t,
    identityStore,
    formDialogVisible,
    formDialogMode,
    formDialogRef,
    resetDialogVisible,
    filterGroup,
    filterRole,
    currentPage,
    form,
    canDelete,
    canCreate,
    roleOptions,
    groupOptions,
    isFilterGroupLocked,
    users,
    errorMessageKey,
    reload,
    resetFilters,
    openCreate,
    openEdit,
    submitForm,
    submitResetPassword,
    handleMoreAction,
  }
}
