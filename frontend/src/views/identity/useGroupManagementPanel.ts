import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { rowSortMethod } from '@/composables/useDataTableFilters'
import { canManageGroups } from '@/auth/identityRoles'
import { useIdentityStore } from '@/stores/identity'
import { useSessionStore } from '@/stores/session'
import type { BusinessGroupView, CreateGroupRequest, GroupDimension } from '@/types/identity'

export type GroupMoreAction = 'toggleEnabled'

export function useGroupManagementPanel() {
  const { t, te } = useI18n()
  const identityStore = useIdentityStore()
  const sessionStore = useSessionStore()

  const canManage = computed(() => canManageGroups(sessionStore.session?.roles ?? []))

  const dialogVisible = ref(false)
  const dialogMode = ref<'create' | 'edit'>('create')
  const editingId = ref<string | null>(null)
  const formRef = ref<FormInstance>()

  const form = reactive<{ groupCode: string; displayName: string; dimension: GroupDimension | '' }>({
    groupCode: '',
    displayName: '',
    dimension: '',
  })

  const currentPage = ref(1)
  const searchQuery = ref('')

  const filteredGroups = computed(() => {
    const query = searchQuery.value.trim().toLowerCase()
    if (!query) {
      return identityStore.groups
    }
    return identityStore.groups.filter(
      (group) =>
        group.groupCode.toLowerCase().includes(query) ||
        group.displayName.toLowerCase().includes(query),
    )
  })

  const dimensionOptions: GroupDimension[] = ['BUSINESS_LINE', 'DEPARTMENT']

  function dimensionLabel(dimension: string): string {
    return te(`identity.dimensions.${dimension}`) ? t(`identity.dimensions.${dimension}`) : dimension
  }

  function handleMoreAction(command: GroupMoreAction, group: BusinessGroupView) {
    if (command === 'toggleEnabled') {
      void toggleEnabled(group)
    }
  }

  const errorMessage = computed(() => {
    const key = identityStore.lastGroupErrorMessageKey
    if (!key) {
      return ''
    }
    return te(key) ? t(key) : t('identity.error.loadGroups')
  })

  const formRules = computed<FormRules>(() => ({
    groupCode: [
      { required: true, message: t('identity.groups.validation.groupCodeRequired'), trigger: 'blur' },
      {
        pattern: /^[A-Z0-9_]{1,64}$/,
        message: t('identity.groups.validation.groupCodePattern'),
        trigger: 'blur',
      },
    ],
    displayName: [
      { required: true, message: t('identity.groups.validation.displayNameRequired'), trigger: 'blur' },
    ],
    dimension: [
      { required: true, message: t('identity.groups.validation.dimensionRequired'), trigger: 'change' },
    ],
  }))

  onMounted(() => {
    void reload()
  })

  watch(currentPage, (page) => {
    void identityStore.fetchGroups({ page: page - 1, size: identityStore.groupFilters.size }).catch(() => {
      // Surfaced via store error key.
    })
  })

  async function reload() {
    currentPage.value = 1
    try {
      await identityStore.fetchGroups({ page: 0 })
    } catch {
      // Surfaced via store error key.
    }
  }

  function openCreate() {
    dialogMode.value = 'create'
    editingId.value = null
    form.groupCode = ''
    form.displayName = ''
    form.dimension = ''
    dialogVisible.value = true
  }

  function openEdit(group: BusinessGroupView) {
    dialogMode.value = 'edit'
    editingId.value = group.id
    form.groupCode = group.groupCode
    form.displayName = group.displayName
    form.dimension = group.dimension
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
      if (dialogMode.value === 'create' && form.dimension) {
        const payload: CreateGroupRequest = {
          groupCode: form.groupCode,
          displayName: form.displayName,
          dimension: form.dimension,
        }
        await identityStore.createGroup(payload)
        ElMessage.success(t('identity.groups.createSuccess'))
      } else if (editingId.value) {
        await identityStore.updateGroup(editingId.value, form.displayName)
        ElMessage.success(t('identity.groups.updateSuccess'))
      }
      dialogVisible.value = false
    } catch {
      ElMessage.error(errorMessage.value || t('identity.error.createGroup'))
    }
  }

  async function toggleEnabled(group: BusinessGroupView) {
    try {
      if (group.enabled) {
        await ElMessageBox.confirm(
          t('identity.groups.confirmDisableMessage', { groupCode: group.groupCode }),
          t('identity.groups.confirmDisableTitle'),
          { type: 'warning' },
        )
      }
      await identityStore.setGroupEnabled(group.id, !group.enabled)
      ElMessage.success(
        group.enabled ? t('identity.groups.disableSuccess') : t('identity.groups.enableSuccess'),
      )
    } catch (error) {
      if (error === 'cancel') {
        return
      }
      ElMessage.error(errorMessage.value || t('identity.error.updateGroup'))
    }
  }

  const sortByDimension = rowSortMethod<BusinessGroupView>((row) => row.dimension)
  const sortByEnabled = rowSortMethod<BusinessGroupView>((row) => row.enabled)

  return {
    t,
    identityStore,
    canManage,
    dialogVisible,
    dialogMode,
    formRef,
    form,
    currentPage,
    searchQuery,
    filteredGroups,
    dimensionOptions,
    dimensionLabel,
    handleMoreAction,
    errorMessage,
    formRules,
    reload,
    openCreate,
    openEdit,
    submitForm,
    sortByDimension,
    sortByEnabled,
  }
}
