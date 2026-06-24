<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import AppDataTable from '@/components/common/AppDataTable.vue'
import MasterStatusBadge from '@/components/masters/MasterStatusBadge.vue'
import type { MasterDocumentDetail } from '@/types/master'

const props = defineProps<{
  master: MasterDocumentDetail
}>()

const { t } = useI18n()

const revisionRows = computed(() => [
  {
    lineLabel: t('masters.detail.revisionLines.currentLine'),
    status: props.master.status,
    originalFilename: props.master.originalFilename,
    anchorCount: props.master.anchors.length,
    updatedAt: props.master.updatedAt,
    updatedBy: props.master.updatedBy,
  },
])
</script>

<template>
  <el-card shadow="never" class="revision-lines-card">
    <template #header>
      <div class="card-header">
        <span>{{ t('masters.detail.revisionLinesTitle') }}</span>
        <p class="card-hint">{{ t('masters.detail.revisionLinesHint') }}</p>
      </div>
    </template>
    <AppDataTable :data="revisionRows">
      <el-table-column prop="lineLabel" min-width="160" :label="t('masters.detail.revisionLines.line')" />
      <el-table-column min-width="140" :label="t('masters.detail.revisionLines.status')">
        <template #default="{ row }">
          <MasterStatusBadge :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column
        prop="originalFilename"
        min-width="180"
        :label="t('masters.detail.revisionLines.sourceFile')"
      />
      <el-table-column
        prop="anchorCount"
        width="100"
        :label="t('masters.detail.revisionLines.anchors')"
      />
      <el-table-column min-width="170" :label="t('masters.detail.revisionLines.updatedAt')">
        <template #default="{ row }">
          {{ new Date(row.updatedAt).toLocaleString() }}
        </template>
      </el-table-column>
      <el-table-column prop="updatedBy" min-width="120" :label="t('masters.detail.revisionLines.updatedBy')" />
    </AppDataTable>
  </el-card>
</template>

<style scoped lang="scss">
.revision-lines-card {
  margin-bottom: 1rem;
}

.card-header {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.card-hint {
  margin: 0;
  font-size: 0.875rem;
  color: var(--text-muted);
}
</style>
