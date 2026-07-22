import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useAbortableCatalogLoader } from '@/composables/useAbortableCatalogLoader'
import { useCapabilities } from '@/composables/useCapabilities'
import { useConfirmAction } from '@/composables/useConfirmAction'
import { useEntityLinkTargets } from '@/composables/useEntityLinkTargets'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { ROUTE_KEYS } from '@/routing/routeKeys'
import { useLegalHoldsStore } from '@/stores/legalHolds'
import { useSessionStore } from '@/stores/session'
import type { LegalHoldStatus, LegalHoldView } from '@/types/legalHold'
import { formatUserDisplayLabel } from '@/utils/userDisplay'

export function useLegalHoldListView() {
  const { t } = useI18n()
  const { formatDateTime } = useLocaleFormatters()
  const legalHoldsStore = useLegalHoldsStore()
  const sessionStore = useSessionStore()
  const { manageLegalHold } = useCapabilities()
  const { confirmAction } = useConfirmAction()
  const { userCatalogLink } = useEntityLinkTargets()

  const createDialogOpen = ref(false)
  const statusFilter = ref<LegalHoldStatus | ''>('')

  const { reload: reloadHolds, signal: listAbortSignal } = useAbortableCatalogLoader(
    async (signal) => {
      await legalHoldsStore.fetchHolds(legalHoldsStore.listPage, legalHoldsStore.listSize, {
        status: statusFilter.value,
        signal,
      })
    },
  )

  const currentPage = computed({
    get: () => legalHoldsStore.listPage + 1,
    set: (page: number) => {
      void legalHoldsStore.fetchHolds(page - 1, legalHoldsStore.listSize, {
        status: statusFilter.value,
        signal: listAbortSignal.value,
      })
    },
  })

  const showListLoadError = computed(
    () => Boolean(legalHoldsStore.lastErrorMessageKey) && !legalHoldsStore.loadingList,
  )

  const canManage = computed(() => manageLegalHold.value)
  const canLinkTemplates = computed(() => sessionStore.canAccessRoute(ROUTE_KEYS.templateManagement))

  const statusOptions = computed(() => [
    { value: '', label: t('legalHold.filters.statusAll') },
    { value: 'ACTIVE', label: t('legalHold.status.ACTIVE') },
    { value: 'RELEASED', label: t('legalHold.status.RELEASED') },
  ])

  function statusLabel(status: LegalHoldStatus | undefined): string {
    if (!status) {
      return '—'
    }
    return t(`legalHold.status.${status}`)
  }

  function statusTagType(status: LegalHoldStatus | undefined): 'success' | 'info' {
    return status === 'ACTIVE' ? 'success' : 'info'
  }

  function scopeLabel(scopeType: LegalHoldView['scopeType'] | undefined): string {
    if (!scopeType) {
      return '—'
    }
    return t(`legalHold.scope.${scopeType}`)
  }

  function scopeSummary(row: LegalHoldView): string {
    if (row.scopeType === 'TEMPLATE_WINDOW') {
      const templateLabel = row.templateExternalId || row.templateId || '—'
      const from = row.effectiveFrom ? formatDateTime(row.effectiveFrom) : '—'
      const to = row.effectiveTo ? formatDateTime(row.effectiveTo) : t('legalHold.scope.openEnded')
      return t('legalHold.scope.templateWindowSummary', { template: templateLabel, from, to })
    }
    return t('legalHold.scope.invocationSetSummary', { count: row.invocationCount })
  }

  function templateLinkTo(row: LegalHoldView): string | undefined {
    if (!row.templateId || !canLinkTemplates.value) {
      return undefined
    }
    return `/templates/${row.templateId}`
  }

  function createdByLabel(row: LegalHoldView): string {
    return formatUserDisplayLabel(row.createdByUsername, row.createdByDisplayName)
  }

  function createdByLinkTo(row: LegalHoldView) {
    return userCatalogLink(row.createdByUsername)
  }

  async function applyStatusFilter() {
    await legalHoldsStore.fetchHolds(0, legalHoldsStore.listSize, {
      status: statusFilter.value,
      signal: listAbortSignal.value,
    })
  }

  async function confirmRelease(row: LegalHoldView) {
    const confirmed = await confirmAction({
      titleKey: 'legalHold.release.confirmTitle',
      messageKey: 'legalHold.release.confirmMessage',
      messageParams: { holdExternalId: row.holdExternalId },
      confirmButtonKey: 'legalHold.release.confirmButton',
      type: 'warning',
    })
    if (!confirmed) {
      return
    }
    await legalHoldsStore.releaseHold(row.id)
    await reloadHolds()
  }

  async function handleCreated() {
    createDialogOpen.value = false
    await legalHoldsStore.fetchHolds(0, legalHoldsStore.listSize, {
      status: statusFilter.value,
      signal: listAbortSignal.value,
    })
  }

  watch(statusFilter, () => {
    void applyStatusFilter()
  })

  onMounted(() => {
    void reloadHolds()
  })

  return {
    t,
    formatDateTime,
    legalHoldsStore,
    createDialogOpen,
    statusFilter,
    statusOptions,
    currentPage,
    showListLoadError,
    canManage,
    reloadHolds,
    statusLabel,
    statusTagType,
    scopeLabel,
    scopeSummary,
    templateLinkTo,
    createdByLabel,
    createdByLinkTo,
    confirmRelease,
    handleCreated,
  }
}
