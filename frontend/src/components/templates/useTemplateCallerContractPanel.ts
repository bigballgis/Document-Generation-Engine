import { computed, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useDataTableFilters } from '@/composables/useDataTableFilters'
import { useCatalogPagination } from '@/composables/useCatalogPagination'
import { useYesNoFilterOptions } from '@/composables/useTableFilterOptions'
import { CLIENT_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import { getCallerContract } from '@/api/contract'
import {
  ALLOWED_ENVIRONMENTS,
  ENVIRONMENT_LABEL_KEY,
  resolveRuntimeEnvironment,
  type RuntimeEnvironment,
} from '@/config/environments'
import type { CallerContract } from '@/types/contract'

export interface UseTemplateCallerContractPanelOptions {
  templateId: Ref<string>
  environment: Ref<RuntimeEnvironment | undefined>
  emitEnvironment: (environment: RuntimeEnvironment) => void
}

export function useTemplateCallerContractPanel(options: UseTemplateCallerContractPanelOptions) {
  const { t, te } = useI18n()
  const loading = ref(false)
  const errorMessageKey = ref<string | null>(null)
  const contract = ref<CallerContract | null>(null)

  const selectedEnvironment = ref<RuntimeEnvironment>(
    resolveRuntimeEnvironment(options.environment.value),
  )
  const environmentOptions = computed(() =>
    ALLOWED_ENVIRONMENTS.map((environment) => ({
      value: environment,
      label: t(ENVIRONMENT_LABEL_KEY[environment]),
    })),
  )
  const currentEnvironment = computed(() => selectedEnvironment.value)

  const versionComparison = computed(() => {
    if (!contract.value) {
      return []
    }
    const defaultVersion = contract.value.defaultRoute.currentTargetReleaseVersion
    return contract.value.callableVersions.map((version) => ({
      releaseVersion: version.releaseVersion,
      explicitVersionUrl: version.explicitVersionUrl,
      isDefaultRouteTarget: version.releaseVersion === defaultVersion,
    }))
  })

  const versionComparisonSource = computed(() => versionComparison.value)
  const { filters: versionColumnFilters, filteredRows: filteredVersionComparison } =
    useDataTableFilters(versionComparisonSource, [
      { key: 'releaseVersion', getValue: (row) => row.releaseVersion },
      { key: 'explicitVersionUrl', getValue: (row) => row.explicitVersionUrl },
      {
        key: 'defaultRoute',
        getValue: (row) => (row.isDefaultRouteTarget ? t('common.yes') : t('common.no')),
        matchMode: 'exact',
      },
    ])

  const versionComparisonCurrentPage = ref(1)
  const { paginatedRows: paginatedVersionComparison, totalRows: totalVersionComparisonRows } =
    useCatalogPagination(
      filteredVersionComparison,
      versionComparisonCurrentPage,
      CLIENT_TABLE_PAGE_SIZE,
    )

  const yesNoFilterOptions = useYesNoFilterOptions()

  const errorCodesSource = computed(() => contract.value?.errorCodes ?? [])
  const { filters: errorColumnFilters, filteredRows: filteredErrorCodes } = useDataTableFilters(
    errorCodesSource,
    [
      { key: 'code', getValue: (row) => row.code },
      { key: 'category', getValue: (row) => row.category },
      { key: 'message', getValue: (row) => row.message },
      {
        key: 'retryable',
        getValue: (row) => (row.retryable ? t('common.yes') : t('common.no')),
        matchMode: 'exact',
      },
    ],
  )

  const errorCodesCurrentPage = ref(1)
  const { paginatedRows: paginatedErrorCodes, totalRows: totalErrorCodeRows } = useCatalogPagination(
    filteredErrorCodes,
    errorCodesCurrentPage,
    CLIENT_TABLE_PAGE_SIZE,
  )

  async function loadContract() {
    loading.value = true
    errorMessageKey.value = null
    try {
      contract.value = await getCallerContract(options.templateId.value, currentEnvironment.value)
    } catch {
      errorMessageKey.value = 'templates.contract.error.load'
    } finally {
      loading.value = false
    }
  }

  watch(
    () => options.environment.value,
    (value) => {
      const resolved = resolveRuntimeEnvironment(value)
      if (selectedEnvironment.value !== resolved) {
        selectedEnvironment.value = resolved
      }
    },
    { immediate: true },
  )

  watch(selectedEnvironment, (value) => {
    options.emitEnvironment(value)
  })

  watch(
    [() => options.templateId.value, currentEnvironment],
    () => {
      void loadContract()
    },
    { immediate: true },
  )

  function errorMessage(key: string | null): string {
    if (!key) {
      return ''
    }
    return te(key) ? t(key) : t('templates.contract.error.load')
  }

  return {
    t,
    CLIENT_TABLE_PAGE_SIZE,
    loading,
    errorMessageKey,
    contract,
    selectedEnvironment,
    environmentOptions,
    versionColumnFilters,
    paginatedVersionComparison,
    versionComparisonCurrentPage,
    totalVersionComparisonRows,
    yesNoFilterOptions,
    errorColumnFilters,
    paginatedErrorCodes,
    errorCodesCurrentPage,
    totalErrorCodeRows,
    errorMessage,
  }
}
