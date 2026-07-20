import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { useAbortableCatalogLoader } from '@/composables/useAbortableCatalogLoader'
import { useCapabilities } from '@/composables/useCapabilities'
import { useScopedGroupOptions } from '@/composables/useScopedGroupOptions'
import { useDocumentBrandsStore } from '@/stores/documentBrands'
import type {
  CreateDocumentBrandPayload,
  DocumentBrandStatus,
  DocumentBrandView,
  UpdateDocumentBrandPayload,
} from '@/types/documentBrand'

export function useDocumentBrandListView() {
  const { t } = useI18n()
  const documentBrandsStore = useDocumentBrandsStore()
  const { manageDocumentBrandCatalogs } = useCapabilities()
  const { resolveDefaultGroupCode, ensureGroupCatalog } = useScopedGroupOptions()

  const groupCode = ref('')
  const statusFilter = ref<DocumentBrandStatus | ''>('')
  const formOpen = ref(false)
  const formMode = ref<'create' | 'edit'>('create')
  const editingBrand = ref<DocumentBrandView | null>(null)

  const { reload: reloadBrands, signal: listAbortSignal } = useAbortableCatalogLoader(
    async (signal) => {
      if (!groupCode.value) {
        return
      }
      await documentBrandsStore.fetchBrands(groupCode.value, {
        status: statusFilter.value,
        signal,
      })
    },
  )

  const showListLoadError = computed(
    () => Boolean(documentBrandsStore.lastErrorMessageKey) && !documentBrandsStore.loadingList,
  )

  const canManage = computed(() => manageDocumentBrandCatalogs.value)

  const statusOptions = computed(() => [
    { value: '' as const, label: t('documentBrands.filters.statusAll') },
    { value: 'ACTIVE' as const, label: t('documentBrands.status.ACTIVE') },
    { value: 'INACTIVE' as const, label: t('documentBrands.status.INACTIVE') },
  ])

  function statusLabel(status: DocumentBrandStatus | undefined): string {
    if (!status) {
      return '—'
    }
    return t(`documentBrands.status.${status}`)
  }

  function statusTagType(status: DocumentBrandStatus | undefined): 'success' | 'info' {
    return status === 'ACTIVE' ? 'success' : 'info'
  }

  function openCreate() {
    formMode.value = 'create'
    editingBrand.value = null
    formOpen.value = true
  }

  function openEdit(row: DocumentBrandView) {
    formMode.value = 'edit'
    editingBrand.value = row
    formOpen.value = true
  }

  async function handleCreate(payload: CreateDocumentBrandPayload) {
    try {
      await documentBrandsStore.createBrand(payload)
      ElMessage.success(t('documentBrands.form.createSuccess'))
      formOpen.value = false
      groupCode.value = payload.groupCode
      await reloadBrands()
    } catch {
      const key = documentBrandsStore.lastMutationErrorMessageKey ?? 'documentBrands.error.create'
      ElMessage.error(t(key))
    }
  }

  async function handleUpdate(payload: UpdateDocumentBrandPayload) {
    const code = editingBrand.value?.documentBrandCode
    if (!code) {
      return
    }
    try {
      await documentBrandsStore.updateBrand(code, payload)
      ElMessage.success(t('documentBrands.form.updateSuccess'))
      formOpen.value = false
      await reloadBrands()
    } catch {
      const key = documentBrandsStore.lastMutationErrorMessageKey ?? 'documentBrands.error.update'
      ElMessage.error(t(key))
    }
  }

  watch([groupCode, statusFilter], ([nextGroup]) => {
    if (!nextGroup) {
      return
    }
    void reloadBrands()
  })

  onMounted(async () => {
    await ensureGroupCatalog()
    groupCode.value = resolveDefaultGroupCode(groupCode.value)
  })

  return {
    t,
    documentBrandsStore,
    groupCode,
    statusFilter,
    statusOptions,
    formOpen,
    formMode,
    editingBrand,
    showListLoadError,
    canManage,
    listAbortSignal,
    reloadBrands,
    statusLabel,
    statusTagType,
    openCreate,
    openEdit,
    handleCreate,
    handleUpdate,
  }
}
