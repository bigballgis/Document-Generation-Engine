import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { useAbortableCatalogLoader } from '@/composables/useAbortableCatalogLoader'
import { useCapabilities } from '@/composables/useCapabilities'
import { useScopedGroupOptions } from '@/composables/useScopedGroupOptions'
import { useLegalEntitiesStore } from '@/stores/legalEntities'
import type {
  CreateLegalEntityPayload,
  DocumentBrandStatus,
  LegalEntityView,
  UpdateLegalEntityPayload,
} from '@/types/documentBrand'

export function useLegalEntityListView() {
  const { t } = useI18n()
  const legalEntitiesStore = useLegalEntitiesStore()
  const { manageDocumentBrandCatalogs } = useCapabilities()
  const { resolveDefaultGroupCode, ensureGroupCatalog } = useScopedGroupOptions()

  const groupCode = ref('')
  const statusFilter = ref<DocumentBrandStatus | ''>('')
  const draftDefaultCode = ref('')
  const formOpen = ref(false)
  const formMode = ref<'create' | 'edit'>('create')
  const editingEntity = ref<LegalEntityView | null>(null)

  const { reload: reloadEntities, signal: listAbortSignal } = useAbortableCatalogLoader(
    async (signal) => {
      if (!groupCode.value) {
        return
      }
      await legalEntitiesStore.fetchEntities(groupCode.value, {
        status: statusFilter.value,
        signal,
      })
      try {
        await legalEntitiesStore.fetchDefault(groupCode.value, { signal })
        draftDefaultCode.value = legalEntitiesStore.defaultLegalEntityCode ?? ''
      } catch {
        /* list can still render; default panel shows mutation error on save */
      }
    },
  )

  const showListLoadError = computed(
    () => Boolean(legalEntitiesStore.lastErrorMessageKey) && !legalEntitiesStore.loadingList,
  )

  const canManage = computed(() => manageDocumentBrandCatalogs.value)

  const statusOptions = computed(() => [
    { value: '' as const, label: t('legalEntities.filters.statusAll') },
    { value: 'ACTIVE' as const, label: t('legalEntities.status.ACTIVE') },
    { value: 'INACTIVE' as const, label: t('legalEntities.status.INACTIVE') },
  ])

  const defaultOptions = computed(() => [
    { value: '', label: t('legalEntities.default.none') },
    ...legalEntitiesStore.entities.map((entity) => ({
      value: entity.legalEntityCode,
      label: `${entity.displayName} (${entity.legalEntityCode})`,
    })),
  ])

  function statusLabel(status: DocumentBrandStatus | undefined): string {
    if (!status) {
      return '—'
    }
    return t(`legalEntities.status.${status}`)
  }

  function statusTagType(status: DocumentBrandStatus | undefined): 'success' | 'info' {
    return status === 'ACTIVE' ? 'success' : 'info'
  }

  function openCreate() {
    formMode.value = 'create'
    editingEntity.value = null
    formOpen.value = true
  }

  function openEdit(row: LegalEntityView) {
    formMode.value = 'edit'
    editingEntity.value = row
    formOpen.value = true
  }

  async function handleCreate(payload: CreateLegalEntityPayload) {
    try {
      await legalEntitiesStore.createEntity(payload)
      ElMessage.success(t('legalEntities.form.createSuccess'))
      formOpen.value = false
      groupCode.value = payload.groupCode
      await reloadEntities()
    } catch {
      const key = legalEntitiesStore.lastMutationErrorMessageKey ?? 'legalEntities.error.create'
      ElMessage.error(t(key))
    }
  }

  async function handleUpdate(payload: UpdateLegalEntityPayload) {
    const code = editingEntity.value?.legalEntityCode
    if (!code) {
      return
    }
    try {
      await legalEntitiesStore.updateEntity(code, payload)
      ElMessage.success(t('legalEntities.form.updateSuccess'))
      formOpen.value = false
      await reloadEntities()
    } catch {
      const key = legalEntitiesStore.lastMutationErrorMessageKey ?? 'legalEntities.error.update'
      ElMessage.error(t(key))
    }
  }

  async function saveDefault() {
    if (!groupCode.value || !canManage.value) {
      return
    }
    try {
      await legalEntitiesStore.putDefault(groupCode.value, {
        defaultLegalEntityCode: draftDefaultCode.value?.trim() || null,
      })
      ElMessage.success(t('legalEntities.default.saveSuccess'))
    } catch {
      const key = legalEntitiesStore.lastMutationErrorMessageKey ?? 'legalEntities.error.saveDefault'
      ElMessage.error(t(key))
    }
  }

  watch([groupCode, statusFilter], ([nextGroup]) => {
    if (!nextGroup) {
      return
    }
    void reloadEntities()
  })

  onMounted(async () => {
    await ensureGroupCatalog()
    groupCode.value = resolveDefaultGroupCode(groupCode.value)
  })

  return {
    t,
    legalEntitiesStore,
    groupCode,
    statusFilter,
    statusOptions,
    draftDefaultCode,
    defaultOptions,
    formOpen,
    formMode,
    editingEntity,
    showListLoadError,
    canManage,
    listAbortSignal,
    reloadEntities,
    statusLabel,
    statusTagType,
    openCreate,
    openEdit,
    handleCreate,
    handleUpdate,
    saveDefault,
  }
}
