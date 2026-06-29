<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import { rowSortMethod, useDataTableFilters } from '@/composables/useDataTableFilters'
import { useCatalogPagination } from '@/composables/useCatalogPagination'
import { CLIENT_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import { useCollaborationStore } from '@/stores/collaboration'
import { formatCollaborationAgeSeconds } from '@/utils/collaborationWorkItems'
import type { CollaborationWorkItemSummary } from '@/types/collaboration'

const props = withDefaults(
  defineProps<{
    queue?: CollaborationWorkItemSummary['queue']
    groupCode?: string | null
    showQueueColumn?: boolean
    emptyMessageKey?: string
  }>(),
  {
    groupCode: null,
    showQueueColumn: false,
    emptyMessageKey: 'collaboration.workItems.empty',
  },
)

const { t } = useI18n()
const router = useRouter()
const collaborationStore = useCollaborationStore()

const currentPage = ref(1)
const selectedItem = ref<CollaborationWorkItemSummary | null>(null)

const visibleItems = computed(() => {
  if (!props.queue) {
    return collaborationStore.workItems
  }
  return collaborationStore.workItems.filter((item) => item.queue === props.queue)
})

const { filters: columnFilters, filteredRows: filteredItems } = useDataTableFilters(visibleItems, [
  { key: 'template', getValue: (row) => row.templateName },
  { key: 'group', getValue: (row) => row.groupCode },
  { key: 'submitter', getValue: (row) => row.submitterUserId },
  { key: 'summary', getValue: (row) => row.summaryText },
  {
    key: 'queue',
    getValue: (row) => (props.showQueueColumn ? t(`collaboration.workItem.queue.${row.queue}.label`) : ''),
  },
])

const { paginatedRows, totalRows } = useCatalogPagination(
  filteredItems,
  currentPage,
  CLIENT_TABLE_PAGE_SIZE,
)

const loading = computed(() => collaborationStore.loadingWorkItems)

async function loadItems() {
  await collaborationStore.fetchWorkItems({
    queue: props.queue,
    groupCode: props.groupCode ?? undefined,
  })
}

onMounted(() => {
  void loadItems()
})

watch(
  () => [props.queue, props.groupCode] as const,
  () => {
    currentPage.value = 1
    void loadItems()
  },
)

function openItem(item: CollaborationWorkItemSummary) {
  router.push(`/templates/${item.templateId}?tab=overview`)
}

function onCurrentChange(row: CollaborationWorkItemSummary | undefined) {
  selectedItem.value = row ?? null
}

function onTableKeydown(event: KeyboardEvent) {
  if (event.key === 'Enter' && selectedItem.value) {
    openItem(selectedItem.value)
  }
}

const sortByTemplate = rowSortMethod<CollaborationWorkItemSummary>((row) => row.templateName)
const sortByGroup = rowSortMethod<CollaborationWorkItemSummary>((row) => row.groupCode)
const sortBySubmitter = rowSortMethod<CollaborationWorkItemSummary>((row) => row.submitterUserId)
const sortByAge = rowSortMethod<CollaborationWorkItemSummary>((row) => row.ageSeconds)
</script>

<template>
  <div class="collaboration-work-item-panel">
    <LoadErrorPanel
      v-if="collaborationStore.workItemsErrorMessageKey"
      :message-key="collaborationStore.workItemsErrorMessageKey"
      @retry="loadItems"
    />

    <el-skeleton v-else-if="loading" :rows="5" animated />

    <el-empty
      v-else-if="filteredItems.length === 0"
      :description="t(emptyMessageKey)"
    />

    <div v-else class="table-wrap">
      <AppDataTable
        activatable
        :data="paginatedRows"
        class="work-items-table"
        highlight-current-row
        tabindex="0"
        default-sort="{ prop: 'templateName', order: 'ascending' }"
        @row-click="openItem"
        @current-change="onCurrentChange"
        @keydown="onTableKeydown"
      >
        <el-table-column sortable :sort-method="sortByTemplate" min-width="220">
          <template #header>
            <TableColumnHeader
              :label="t('collaboration.workItems.columns.template')"
              v-model="columnFilters.template"
            />
          </template>
          <template #default="{ row }">
            <strong>{{ row.templateName }}</strong>
          </template>
        </el-table-column>

        <el-table-column sortable :sort-method="sortByGroup" width="140">
          <template #header>
            <TableColumnHeader
              :label="t('collaboration.workItems.columns.group')"
              v-model="columnFilters.group"
            />
          </template>
          <template #default="{ row }">
            {{ row.groupCode }}
          </template>
        </el-table-column>

        <el-table-column sortable :sort-method="sortBySubmitter" width="160">
          <template #header>
            <TableColumnHeader
              :label="t('collaboration.workItems.columns.submitter')"
              v-model="columnFilters.submitter"
            />
          </template>
          <template #default="{ row }">
            {{ row.submitterUserId }}
          </template>
        </el-table-column>

        <el-table-column sortable :sort-method="sortByAge" width="120">
          <template #header>
            <TableColumnHeader :label="t('collaboration.workItems.columns.age')" />
          </template>
          <template #default="{ row }">
            {{ t('collaboration.workItems.ageValue', { value: formatCollaborationAgeSeconds(row.ageSeconds) }) }}
          </template>
        </el-table-column>

        <el-table-column v-if="showQueueColumn" min-width="160">
          <template #header>
            <TableColumnHeader
              :label="t('collaboration.workItems.columns.queue')"
              v-model="columnFilters.queue"
            />
          </template>
          <template #default="{ row }">
            {{ t(`collaboration.workItem.queue.${row.queue}.label`) }}
          </template>
        </el-table-column>

        <el-table-column min-width="260">
          <template #header>
            <TableColumnHeader
              :label="t('collaboration.workItems.columns.summary')"
              v-model="columnFilters.summary"
            />
          </template>
          <template #default="{ row }">
            {{ row.summaryText }}
          </template>
        </el-table-column>
      </AppDataTable>

      <AppTablePagination
        v-model:current-page="currentPage"
        :page-size="CLIENT_TABLE_PAGE_SIZE"
        :total="totalRows"
      />
    </div>
  </div>
</template>

<style scoped lang="scss">
.work-items-table {
  cursor: pointer;
}

.table-wrap {
  outline: none;
}
</style>
