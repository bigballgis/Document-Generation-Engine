import { computed, onMounted, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useTemplateReleaseVersionActions } from '@/components/templates/useTemplateReleaseVersionActions'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { rowSortMethod, useDataTableFilters } from '@/composables/useDataTableFilters'
import { useCatalogPagination } from '@/composables/useCatalogPagination'
import { useLifecycleStatusFilterOptions } from '@/composables/useTableFilterOptions'
import { CLIENT_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import { useCapabilities } from '@/composables/useCapabilities'
import { useTemplatePanelDataStore } from '@/stores/templatePanelData'
import { useTemplatesStore } from '@/stores/templates'
import type {
  TemplateLifecycleStatus,
  TemplateReleaseVersion,
} from '@/types/template'
import { resolveUpdatedByDisplay } from '@/utils/userDisplay'

export interface UseTemplateReleaseVersionHistoryPanelOptions {
  templateId: Ref<string>
  templateLifecycleStatus: Ref<TemplateLifecycleStatus>
  onChanged: () => void
}

export function useTemplateReleaseVersionHistoryPanel(
  options: UseTemplateReleaseVersionHistoryPanelOptions,
) {
  const { t, te } = useI18n()
  const { formatDateTime } = useLocaleFormatters()
  const lifecycleStatusFilterOptions = useLifecycleStatusFilterOptions()
  const defaultRouteFilterOptions = computed(() => [
    { value: t('templates.versions.defaultRouteYes'), label: t('templates.versions.defaultRouteYes') },
    { value: t('templates.versions.defaultRouteNo'), label: t('templates.versions.defaultRouteNo') },
  ])
  const templatesStore = useTemplatesStore()
  const panelDataStore = useTemplatePanelDataStore()
  const { manageReleaseVersionState } = useCapabilities()

  const loadError = ref(false)
  const entry = computed(() => panelDataStore.getEntry(options.templateId.value))
  const loading = computed(() => entry.value.loadingReleaseVersions)
  const versions = computed(() => entry.value.releaseVersions)

  const versionsSource = computed(() => versions.value)
  const { filters: columnFilters, filteredRows: filteredVersions, hasActiveFilters, clearFilters } =
    useDataTableFilters(versionsSource, [
      { key: 'releaseVersion', getValue: (row) => row.releaseVersion },
      { key: 'devVersionNumber', getValue: (row) => String(row.devVersionNumber) },
      { key: 'status', getValue: (row) => row.lifecycleStatus, matchMode: 'exact' },
      {
        key: 'defaultRoute',
        getValue: (row) =>
          row.defaultRouteTarget ? t('templates.versions.defaultRouteYes') : t('templates.versions.defaultRouteNo'),
        matchMode: 'exact',
      },
      { key: 'updatedAt', getValue: (row) => formatDateTime(row.updatedAt) },
      { key: 'updatedBy', getValue: (row) => resolveUpdatedByDisplay(row.updatedBy, row.updatedByDisplayName) },
    ])
  const versionsCurrentPage = ref(1)
  const { paginatedRows: paginatedVersions, totalRows: totalVersionRows } = useCatalogPagination(
    filteredVersions,
    versionsCurrentPage,
    CLIENT_TABLE_PAGE_SIZE,
  )

  const canManageVersions = computed(
    () =>
      manageReleaseVersionState.value &&
      options.templateLifecycleStatus.value === 'PUBLISHED',
  )

  const showWorkflowHint = computed(() =>
    ['DRAFT', 'TESTING', 'APPROVAL', 'PENDING_RELEASE'].includes(options.templateLifecycleStatus.value),
  )

  const errorMessage = computed(() => {
    const key = templatesStore.lastErrorMessageKey
    if (!key) {
      return ''
    }
    return te(key) ? t(key) : t('templates.error.loadDetail')
  })

  async function loadVersions() {
    loadError.value = false
    try {
      await panelDataStore.fetchReleaseVersions(options.templateId.value)
    } catch {
      loadError.value = true
      panelDataStore.invalidateVersionLineDomains(options.templateId.value)
    }
  }

  onMounted(() => {
    void loadVersions()
  })

  watch(
    () => options.templateId.value,
    () => {
      void loadVersions()
    },
  )

  const { handleVersionAction } = useTemplateReleaseVersionActions({
    templateId: options.templateId,
    errorMessage,
    loadVersions,
    onChanged: options.onChanged,
  })

  const sortByDevVersion = rowSortMethod<TemplateReleaseVersion>((row) => row.devVersionNumber)
  const sortByLifecycleStatus = rowSortMethod<TemplateReleaseVersion>((row) => row.lifecycleStatus)
  const sortByUpdatedAt = rowSortMethod<TemplateReleaseVersion>((row) => row.updatedAt)

  return {
    t,
    formatDateTime,
    lifecycleStatusFilterOptions,
    defaultRouteFilterOptions,
    templatesStore,
    loadError,
    loading,
    columnFilters,
    hasActiveFilters,
    clearFilters,
    versionsCurrentPage,
    paginatedVersions,
    totalVersionRows,
    canManageVersions,
    showWorkflowHint,
    loadVersions,
    handleVersionAction,
    sortByDevVersion,
    sortByLifecycleStatus,
    sortByUpdatedAt,
    resolveUpdatedByDisplay,
    CLIENT_TABLE_PAGE_SIZE,
  }
}
