import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { useAbortableCatalogLoader } from '@/composables/useAbortableCatalogLoader'
import { useConfirmAction } from '@/composables/useConfirmAction'
import { useCapabilities } from '@/composables/useCapabilities'
import { useEntityLinkTargets } from '@/composables/useEntityLinkTargets'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { useScopedGroupOptions } from '@/composables/useScopedGroupOptions'
import { SERVER_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import { useLibraryAssetsStore } from '@/stores/libraryAssets'
import type {
  LibraryAssetClass,
  LibraryAssetListStatusFilter,
  LibraryAssetView,
} from '@/types/libraryAsset'
import { createAssetLibraryCatalogControls } from '@/views/library/createAssetLibraryCatalogControls'

export function useAssetLibraryListView() {
  const { t, te } = useI18n()
  const { formatDateTime, formatNumber } = useLocaleFormatters()
  const { confirmAction } = useConfirmAction()
  const { groupCatalogLink } = useEntityLinkTargets()
  const { ensureGroupCatalog, isGroupLocked, resolveDefaultGroupCode } = useScopedGroupOptions()
  const libraryAssetsStore = useLibraryAssetsStore()
  const {
    uploadAnyLibraryAsset,
    uploadImageOrOtherAsset,
    uploadSealAsset,
    disableAssetLibrary,
    assetLibraryTesterOnly,
  } = useCapabilities()

  const uploadDialogOpen = ref(false)
  const uploadFailurePending = ref(false)
  const currentPage = ref(1)
  const listHydrated = ref(false)
  const listGroupCode = ref('')

  const allAssets = computed(() => libraryAssetsStore.assets)

  const classFilterOptions = computed(() =>
    (['IMAGE', 'SEAL', 'OTHER'] as LibraryAssetClass[]).map((value) => ({
      value,
      label: t(`assetLibrary.assetClass.${value}`),
    })),
  )

  const statusFilterOptions = computed(() => {
    if (assetLibraryTesterOnly.value) {
      return []
    }
    return (
      [
        { value: 'ACTIVE', labelKey: 'assetLibrary.status.ACTIVE' },
        { value: 'DISABLED', labelKey: 'assetLibrary.status.DISABLED' },
        { value: 'ALL', labelKey: 'assetLibrary.status.ALL' },
      ] as const
    ).map((option) => ({
      value: option.value,
      label: t(option.labelKey),
    }))
  })

  const {
    searchQuery,
    filters,
    activeSortKey,
    hasAnyActive,
    activeFilterChips,
    clearAll,
    removeFilterChip,
    catalogToolbarFilters,
    catalogSortOptions,
  } = createAssetLibraryCatalogControls(allAssets, classFilterOptions, statusFilterOptions)

  const showListLoadError = computed(
    () =>
      Boolean(libraryAssetsStore.lastErrorMessageKey) &&
      !libraryAssetsStore.loadingList &&
      !uploadDialogOpen.value &&
      !uploadFailurePending.value,
  )

  const showCatalogChrome = computed(
    () =>
      listHydrated.value &&
      !showListLoadError.value &&
      (libraryAssetsStore.assetListTotalElements > 0 ||
        hasAnyActive.value ||
        libraryAssetsStore.assets.length > 0),
  )

  const showGroupFilter = computed(() => listHydrated.value && !showListLoadError.value)

  function buildListQuery() {
    const statusRaw = filters.status?.trim()
    let status: LibraryAssetListStatusFilter | undefined
    if (!assetLibraryTesterOnly.value && statusRaw) {
      status = statusRaw as LibraryAssetListStatusFilter
    }
    const assetClassRaw = filters.assetClass?.trim()
    const groupCode = listGroupCode.value.trim() || undefined
    return {
      groupCode,
      q: searchQuery.value.trim() || undefined,
      assetClass: (assetClassRaw || undefined) as LibraryAssetClass | undefined,
      status,
    }
  }

  const canUpload = computed(() => uploadAnyLibraryAsset.value)
  const canDisable = computed(() => disableAssetLibrary.value)
  const canClearGroupFilter = computed(() => !isGroupLocked.value)

  const errorMessage = computed(() => {
    const key = libraryAssetsStore.lastErrorMessageKey
    if (!key || uploadDialogOpen.value || uploadFailurePending.value) {
      return ''
    }
    return te(key) ? t(key) : t('assetLibrary.error.loadList')
  })

  const { reload: reloadAssets, signal: abortSignal } = useAbortableCatalogLoader(async (signal) => {
    await libraryAssetsStore.fetchAssets(currentPage.value - 1, SERVER_TABLE_PAGE_SIZE, {
      signal,
      ...buildListQuery(),
    })
    listHydrated.value = true
  })

  watch(currentPage, async (page) => {
    const serverPage = page - 1
    if (serverPage === libraryAssetsStore.assetListPage) {
      return
    }
    try {
      await libraryAssetsStore.fetchAssets(serverPage, SERVER_TABLE_PAGE_SIZE, {
        signal: abortSignal.value,
        ...buildListQuery(),
      })
    } catch {
      // Error surfaced via store message key.
    }
  })

  watch(
    [searchQuery, filters, listGroupCode],
    async () => {
      if (!listHydrated.value) {
        return
      }
      if (currentPage.value !== 1) {
        currentPage.value = 1
        return
      }
      await reloadAssets()
    },
    { deep: true },
  )

  onMounted(async () => {
    await ensureGroupCatalog()
    listGroupCode.value = resolveDefaultGroupCode(listGroupCode.value)
    await reloadAssets()
  })

  watch(uploadDialogOpen, (open) => {
    if (open) {
      return
    }
    if (uploadFailurePending.value) {
      libraryAssetsStore.clearMutationError()
      uploadFailurePending.value = false
    }
  })

  function formatSizeBytes(sizeBytes: number): string {
    if (sizeBytes < 1024) {
      return t('assetLibrary.list.sizeBytes', { size: formatNumber(sizeBytes) })
    }
    const kib = sizeBytes / 1024
    if (kib < 1024) {
      return t('assetLibrary.list.sizeKiB', { size: formatNumber(Number(kib.toFixed(1))) })
    }
    return t('assetLibrary.list.sizeMiB', {
      size: formatNumber(Number((kib / 1024).toFixed(2))),
    })
  }

  function statusLabel(status: string): string {
    const key = `assetLibrary.status.${status}`
    return te(key) ? t(key) : status
  }

  function statusTagType(status: string): 'success' | 'info' {
    return status === 'ACTIVE' ? 'success' : 'info'
  }

  function classLabel(assetClass: string): string {
    const key = `assetLibrary.assetClass.${assetClass}`
    return te(key) ? t(key) : assetClass
  }

  async function handleUpload(payload: {
    groupCode: string
    assetKey: string
    assetClass: LibraryAssetClass
    file: File
  }): Promise<void> {
    uploadFailurePending.value = false
    try {
      await libraryAssetsStore.uploadAsset(payload)
      ElMessage.success(t('assetLibrary.upload.success'))
      uploadDialogOpen.value = false
      await reloadAssets()
    } catch {
      uploadFailurePending.value = true
    }
  }

  function clearUploadServerError() {
    libraryAssetsStore.clearMutationError()
    uploadFailurePending.value = false
  }

  async function confirmDisable(asset: LibraryAssetView): Promise<void> {
    if (!canDisable.value || asset.status !== 'ACTIVE') {
      return
    }
    const confirmed = await confirmAction({
      titleKey: 'assetLibrary.disable.confirmTitle',
      messageKey: 'assetLibrary.disable.confirmMessage',
      messageParams: { assetKey: asset.assetKey, groupCode: asset.groupCode },
      confirmButtonKey: 'assetLibrary.disable.confirm',
      type: 'warning',
    })
    if (!confirmed) {
      return
    }
    try {
      await libraryAssetsStore.disableAsset(asset.assetKey, asset.groupCode)
      ElMessage.success(t('assetLibrary.disable.success'))
      await reloadAssets()
    } catch {
      const key = libraryAssetsStore.lastMutationErrorMessageKey
      ElMessage.error(key && te(key) ? t(key) : t('assetLibrary.error.disable'))
    }
  }

  return {
    t,
    formatDateTime,
    formatSizeBytes,
    libraryAssetsStore,
    uploadDialogOpen,
    currentPage,
    allAssets,
    listHydrated,
    listGroupCode,
    showGroupFilter,
    canClearGroupFilter,
    groupCatalogLink,
    searchQuery,
    filters,
    activeSortKey,
    hasAnyActive,
    activeFilterChips,
    clearAll,
    removeFilterChip,
    catalogToolbarFilters,
    catalogSortOptions,
    showCatalogChrome,
    showListLoadError,
    canUpload,
    canDisable,
    uploadImageOrOtherAsset,
    uploadSealAsset,
    errorMessage,
    reloadAssets,
    handleUpload,
    clearUploadServerError,
    confirmDisable,
    statusLabel,
    statusTagType,
    classLabel,
  }
}
