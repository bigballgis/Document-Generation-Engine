import { computed, onMounted, reactive, ref } from 'vue'
import { listInvocations } from '@/api/apiPolicy'
import { listAllTemplates, listTemplates } from '@/api/templates'
import {
  attachPackageIdentity,
  computeOpsSummaryFromRows,
  CROSS_PACKAGE_INVOCATIONS_PER_PACKAGE,
  CROSS_PACKAGE_SAMPLE_CAP,
  filterRowsByRequestId,
  mergeAndSortCrossPackageRows,
  paginateRows,
  selectPackagesForComposition,
  type CrossPackageInvocationRow,
  type ExternalServicesOpsSummary,
} from '@/composables/externalServicesOpsCompose'
import { SERVER_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import type { ManagementInvocationFilters, TemplateSummary } from '@/types/template'

export type CrossPackageInvocationFilters = {
  status: string
  requestId: string
  templateId: string
  createdAfter: string
  createdBefore: string
}

const EMPTY_OPS: ExternalServicesOpsSummary = {
  sampledPackageCount: 0,
  sampledInvocationCount: 0,
  failedCount: 0,
  succeededCount: 0,
  failureRatePercent: null,
  compositionCapped: false,
}

function toApiFilters(filters: CrossPackageInvocationFilters): ManagementInvocationFilters {
  return {
    status: filters.status || undefined,
    requestId: filters.requestId || undefined,
    createdAfter: filters.createdAfter || undefined,
    createdBefore: filters.createdBefore || undefined,
  }
}

async function loadAuthorizedPackages(signal?: AbortSignal): Promise<{
  packages: TemplateSummary[]
  truncated: boolean
}> {
  const collected = await listAllTemplates({ signal, sort: 'updatedAt,desc' })
  return { packages: collected.content, truncated: collected.truncated }
}

async function fetchPackageInvocations(
  template: TemplateSummary,
  filters: ManagementInvocationFilters,
  page: number,
  size: number,
): Promise<{ rows: CrossPackageInvocationRow[]; totalElements: number }> {
  const pageView = await listInvocations(template.id, page, size, filters)
  return {
    rows: attachPackageIdentity(template, pageView.content),
    totalElements: pageView.totalElements,
  }
}

type PackageInvocationBatch = {
  ok: boolean
  rows: CrossPackageInvocationRow[]
}

async function fetchPackageInvocationBatch(
  template: TemplateSummary,
  filters: ManagementInvocationFilters,
  page: number,
  size: number,
): Promise<PackageInvocationBatch> {
  try {
    const result = await fetchPackageInvocations(template, filters, page, size)
    return { ok: true, rows: result.rows }
  } catch {
    return { ok: false, rows: [] }
  }
}

function assertCompositionFetchHonesty(batches: PackageInvocationBatch[]): void {
  const successCount = batches.filter((batch) => batch.ok).length
  const errorCount = batches.length - successCount
  if (successCount === 0 && errorCount > 0) {
    throw new Error('CROSS_PACKAGE_INVOCATION_FETCH_FAILED')
  }
}

/** Raw status enum values — views translate via `apiPolicy.invocationsPage.statusLabels.*`. */
export const CROSS_PACKAGE_INVOCATION_STATUS_VALUES = [
  'SUCCEEDED',
  'FAILED',
  'PARTIAL_SUCCEEDED',
  'PROCESSING',
  'ACCEPTED',
  'EXPIRED',
  'CANCELLED',
] as const

export function useCrossPackageInvocations(options: { autoLoad?: boolean } = {}) {
  const autoLoad = options.autoLoad !== false
  const pageSize = SERVER_TABLE_PAGE_SIZE
  const currentPage = ref(1)
  const totalElements = ref(0)
  const loading = ref(false)
  const loadFailed = ref(false)
  const compositionLimited = ref(false)
  const packageCatalogTruncated = ref(false)
  const rows = ref<CrossPackageInvocationRow[]>([])
  const packageOptions = ref<TemplateSummary[]>([])
  const opsSummary = ref<ExternalServicesOpsSummary>({ ...EMPTY_OPS })

  const filterDraft = reactive<CrossPackageInvocationFilters>({
    status: '',
    requestId: '',
    templateId: '',
    createdAfter: '',
    createdBefore: '',
  })

  const appliedFilters = reactive<CrossPackageInvocationFilters>({
    status: '',
    requestId: '',
    templateId: '',
    createdAfter: '',
    createdBefore: '',
  })

  const drawerVisible = ref(false)
  const selectedRow = ref<CrossPackageInvocationRow | null>(null)

  const statusFilterOptions = computed(() =>
    CROSS_PACKAGE_INVOCATION_STATUS_VALUES.map((value) => ({
      label: value,
      value,
    })),
  )

  const uiPage = computed({
    get: () => currentPage.value,
    set: (page: number) => {
      currentPage.value = page
      void loadInvocations()
    },
  })

  const selectedTemplateId = computed(() => appliedFilters.templateId.trim())

  async function ensurePackageOptions() {
    if (packageOptions.value.length > 0) {
      return
    }
    const collected = await loadAuthorizedPackages()
    packageOptions.value = collected.packages
    packageCatalogTruncated.value = collected.truncated
  }

  async function loadComposedMode() {
    await ensurePackageOptions()
    const { selected, compositionCapped } = selectPackagesForComposition(
      packageOptions.value,
      CROSS_PACKAGE_SAMPLE_CAP,
    )
    compositionLimited.value = compositionCapped || packageCatalogTruncated.value
    const apiFilters = toApiFilters(appliedFilters)
    const batches = await Promise.all(
      selected.map((template) =>
        fetchPackageInvocationBatch(
          template,
          apiFilters,
          0,
          CROSS_PACKAGE_INVOCATIONS_PER_PACKAGE,
        ),
      ),
    )
    assertCompositionFetchHonesty(batches)
    let merged = mergeAndSortCrossPackageRows(batches.flatMap((batch) => batch.rows))
    merged = filterRowsByRequestId(merged, appliedFilters.requestId)
    opsSummary.value = computeOpsSummaryFromRows(merged, selected.length, compositionLimited.value)
    const page = paginateRows(merged, currentPage.value - 1, pageSize)
    rows.value = page.content
    totalElements.value = page.totalElements
  }

  async function loadSinglePackageMode() {
    compositionLimited.value = false
    const templateId = selectedTemplateId.value
    let template = packageOptions.value.find((item) => item.id === templateId)
    if (!template) {
      await ensurePackageOptions()
      template = packageOptions.value.find((item) => item.id === templateId)
    }
    if (!template) {
      // Fallback identity when package not in first catalog pages.
      const page = await listTemplates(0, 1, { search: templateId })
      template = page.content.find((item) => item.id === templateId) ?? page.content[0]
    }
    if (!template) {
      rows.value = []
      totalElements.value = 0
      opsSummary.value = { ...EMPTY_OPS }
      return
    }
    const result = await fetchPackageInvocations(
      template,
      toApiFilters(appliedFilters),
      currentPage.value - 1,
      pageSize,
    )
    rows.value = result.rows
    totalElements.value = result.totalElements
    opsSummary.value = computeOpsSummaryFromRows(result.rows, 1, false)
  }

  async function loadInvocations() {
    loading.value = true
    loadFailed.value = false
    try {
      if (selectedTemplateId.value) {
        await loadSinglePackageMode()
      } else {
        await loadComposedMode()
      }
    } catch {
      rows.value = []
      totalElements.value = 0
      opsSummary.value = { ...EMPTY_OPS }
      loadFailed.value = true
    } finally {
      loading.value = false
    }
  }

  async function loadOpsSummaryOnly() {
    loading.value = true
    loadFailed.value = false
    try {
      await ensurePackageOptions()
      const { selected, compositionCapped } = selectPackagesForComposition(
        packageOptions.value,
        CROSS_PACKAGE_SAMPLE_CAP,
      )
      compositionLimited.value = compositionCapped || packageCatalogTruncated.value
      if (selected.length === 0) {
        opsSummary.value = { ...EMPTY_OPS }
        return
      }
      const batches = await Promise.all(
        selected.map((template) =>
          fetchPackageInvocationBatch(template, {}, 0, CROSS_PACKAGE_INVOCATIONS_PER_PACKAGE),
        ),
      )
      assertCompositionFetchHonesty(batches)
      const merged = mergeAndSortCrossPackageRows(batches.flatMap((batch) => batch.rows))
      opsSummary.value = computeOpsSummaryFromRows(
        merged,
        selected.length,
        compositionLimited.value,
      )
    } catch {
      opsSummary.value = { ...EMPTY_OPS }
      loadFailed.value = true
    } finally {
      loading.value = false
    }
  }

  function applyFilters() {
    appliedFilters.status = filterDraft.status
    appliedFilters.requestId = filterDraft.requestId
    appliedFilters.templateId = filterDraft.templateId
    appliedFilters.createdAfter = filterDraft.createdAfter
    appliedFilters.createdBefore = filterDraft.createdBefore
    currentPage.value = 1
    void loadInvocations()
  }

  function clearFilters() {
    filterDraft.status = ''
    filterDraft.requestId = ''
    filterDraft.templateId = ''
    filterDraft.createdAfter = ''
    filterDraft.createdBefore = ''
    appliedFilters.status = ''
    appliedFilters.requestId = ''
    appliedFilters.templateId = ''
    appliedFilters.createdAfter = ''
    appliedFilters.createdBefore = ''
    currentPage.value = 1
    void loadInvocations()
  }

  function openDetail(row: CrossPackageInvocationRow) {
    selectedRow.value = row
    drawerVisible.value = true
  }

  function seedTemplateFilter(templateId: string | null | undefined) {
    if (!templateId?.trim()) {
      return
    }
    filterDraft.templateId = templateId.trim()
    appliedFilters.templateId = templateId.trim()
  }

  if (autoLoad) {
    onMounted(() => {
      void loadInvocations()
    })
  }

  return {
    pageSize,
    currentPage,
    totalElements,
    loading,
    loadFailed,
    compositionLimited,
    packageCatalogTruncated,
    rows,
    packageOptions,
    opsSummary,
    filterDraft,
    appliedFilters,
    drawerVisible,
    selectedRow,
    statusFilterOptions,
    uiPage,
    loadInvocations,
    loadOpsSummaryOnly,
    applyFilters,
    clearFilters,
    openDetail,
    seedTemplateFilter,
    ensurePackageOptions,
  }
}
