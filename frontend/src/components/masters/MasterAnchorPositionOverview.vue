<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import AppDataTable from '@/components/common/AppDataTable.vue'
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import {
  sortMasterAnchorsByDocumentSequence,
  toMasterAnchorDocumentPosition,
} from '@/utils/masterAnchorDocumentOrder'
import type { MasterAnchor } from '@/types/master'

const props = defineProps<{
  anchors: MasterAnchor[]
  canEditDisplayLabel: boolean
  columnFilters: Record<string, string>
}>()

const emit = defineEmits<{
  'update:columnFilters': [value: Record<string, string>]
  editDisplayLabel: [anchor: MasterAnchor]
}>()

const { t } = useI18n()

const selectedAnchorId = ref<string | null>(null)

const orderedAnchors = computed(() => sortMasterAnchorsByDocumentSequence(props.anchors))

const anchorColumnFilters = computed({
  get: () => props.columnFilters,
  set: (value: Record<string, string>) => emit('update:columnFilters', value),
})

function handleRowClick(row: MasterAnchor) {
  selectedAnchorId.value = row.anchorId
}

function rowClassName({ row }: { row: MasterAnchor }) {
  return row.anchorId === selectedAnchorId.value ? 'master-anchor-row--selected' : ''
}

function openEdit(row: MasterAnchor) {
  selectedAnchorId.value = row.anchorId
  emit('editDisplayLabel', row)
}
</script>

<template>
  <section
    class="master-anchor-position-overview"
    data-testid="master-anchor-position-overview"
  >
    <p class="overview-hint">{{ t('masters.revision.anchorOverviewHint') }}</p>
    <AppDataTable
      v-if="orderedAnchors.length > 0"
      :data="orderedAnchors"
      highlight-current-row
      :row-class-name="rowClassName"
      data-testid="master-anchor-position-list"
      @row-click="(row: MasterAnchor) => handleRowClick(row)"
    >
      <el-table-column
        :label="t('masters.revision.anchorPosition')"
        width="100"
        align="center"
      >
        <template #default="{ row }">
          <span
            class="position-cell"
            :data-testid="`master-anchor-position-${row.anchorId}`"
          >
            {{ toMasterAnchorDocumentPosition(row.documentSequence) }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="anchorId" min-width="160">
        <template #header>
          <TableColumnHeader
            :label="t('masters.revision.anchorId')"
            :model-value="anchorColumnFilters.anchorId ?? ''"
            @update:model-value="
              anchorColumnFilters = { ...anchorColumnFilters, anchorId: $event }
            "
          />
        </template>
      </el-table-column>
      <el-table-column prop="displayLabel" min-width="220">
        <template #header>
          <TableColumnHeader
            :label="t('masters.revision.anchorLabel')"
            :model-value="anchorColumnFilters.displayLabel ?? ''"
            @update:model-value="
              anchorColumnFilters = { ...anchorColumnFilters, displayLabel: $event }
            "
          />
        </template>
        <template #default="{ row }">
          <span :data-testid="`master-anchor-label-${row.anchorId}`">
            {{ row.displayLabel }}
          </span>
        </template>
      </el-table-column>
      <el-table-column
        v-if="canEditDisplayLabel"
        :label="t('masters.revision.actions')"
        width="120"
        align="right"
      >
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            :data-testid="`master-anchor-edit-label-${row.anchorId}`"
            @click.stop="openEdit(row)"
          >
            {{ t('masters.revision.editAnchorLabel') }}
          </el-button>
        </template>
      </el-table-column>
    </AppDataTable>
    <el-empty v-else :description="t('masters.revision.noAnchors')" />
  </section>
</template>

<style scoped lang="scss">
.overview-hint {
  margin: 0 0 var(--space-3);
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

.position-cell {
  font-variant-numeric: tabular-nums;
  font-weight: 600;
}

:deep(.master-anchor-row--selected > td) {
  background-color: var(--el-table-current-row-bg-color) !important;
}
</style>
