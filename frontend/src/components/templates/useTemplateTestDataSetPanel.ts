import { computed, onMounted, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { useCatalogPagination } from '@/composables/useCatalogPagination'
import { rowSortMethod, useDataTableFilters } from '@/composables/useDataTableFilters'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { CLIENT_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import { useTemplatePanelDataStore } from '@/stores/templatePanelData'
import type { TestDataSet, VariableSchema } from '@/types/template'
import { createTemplateTestDataSetPanelActions } from '@/components/templates/createTemplateTestDataSetPanelActions'

export interface UseTemplateTestDataSetPanelOptions {
  templateId: Ref<string> | (() => string)
  refreshToken: Ref<number | undefined> | (() => number | undefined)
  variables: Ref<VariableSchema[]> | (() => VariableSchema[])
  emitSelected: (testDataSetId: string | null) => void
  emitLoaded: (count: number) => void
}

function readValue<T>(source: Ref<T> | (() => T)): T {
  return typeof source === 'function' ? source() : source.value
}

export function useTemplateTestDataSetPanel(options: UseTemplateTestDataSetPanelOptions) {
  const { t } = useI18n()
  const { formatDateTime } = useLocaleFormatters()
  const panelDataStore = useTemplatePanelDataStore()
  const dataSets = computed(() => panelDataStore.getEntry(readValue(options.templateId)).testDataSets)
  const loading = computed(() => panelDataStore.getEntry(readValue(options.templateId)).loadingTestDataSets)
  const dataSetsSource = computed(() => dataSets.value)
  const { filters: columnFilters, filteredRows: filteredDataSets } = useDataTableFilters(
    dataSetsSource,
    [
      { key: 'name', getValue: (row) => row.name },
      { key: 'testDataSetId', getValue: (row) => row.testDataSetId },
      { key: 'updatedAt', getValue: (row) => formatDateTime(row.updatedAt) },
    ],
  )
  const dataSetsCurrentPage = ref(1)
  const { paginatedRows: paginatedDataSets, totalRows: totalDataSetRows } = useCatalogPagination(
    filteredDataSets,
    dataSetsCurrentPage,
    CLIENT_TABLE_PAGE_SIZE,
  )
  const sortByUpdatedAt = rowSortMethod<TestDataSet>((row) => row.updatedAt)
  const selectedId = ref<string | null>(null)

  const actions = createTemplateTestDataSetPanelActions({
    t,
    panelDataStore,
    templateId: () => readValue(options.templateId),
    variables: () => readValue(options.variables),
    selectedId,
    emitSelected: options.emitSelected,
  })

  async function loadDataSets() {
    try {
      await panelDataStore.fetchTestDataSets(readValue(options.templateId))
      options.emitLoaded(dataSets.value.length)
    } catch {
      ElMessage.error(t('templates.testDataSets.error.load'))
    }
  }

  watch(
    () => readValue(options.refreshToken),
    () => {
      void loadDataSets()
    },
  )

  function handlePreviewRetry() {
    const row = dataSets.value.find((dataSet) => dataSet.testDataSetId === selectedId.value)
    if (row) {
      void actions.handleRunPreview(row)
    }
  }

  onMounted(() => {
    void loadDataSets()
  })

  function rowClassName({ row }: { row: TestDataSet }): string {
    return row.testDataSetId === selectedId.value ? 'is-selected-row' : ''
  }

  return {
    t,
    formatDateTime,
    CLIENT_TABLE_PAGE_SIZE,
    loading,
    columnFilters,
    paginatedDataSets,
    totalDataSetRows,
    dataSetsCurrentPage,
    sortByUpdatedAt,
    loadDataSets,
    dataSets,
    handlePreviewRetry,
    rowClassName,
    ...actions,
  }
}
