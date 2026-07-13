<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { RouteLocationRaw } from 'vue-router'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import EntityLinkCell from '@/components/common/EntityLinkCell.vue'
import type { ManagementAuditEvent } from '@/types/audit'
import type { AuditActorDisplayFields } from '@/utils/auditEntityDisplay'

defineProps<{
  loading: boolean
  rows: ManagementAuditEvent[]
  currentPage: number
  pageSize: number
  total: number
  formatActor: (event: AuditActorDisplayFields) => string
  formatEventType: (eventType?: string) => string
  formatDate: (value: string) => string
  resolveTemplateCell: (
    event: Pick<ManagementAuditEvent, 'templateId' | 'templateDisplayName' | 'templateExternalId'>,
  ) => { label: string; subtitle?: string; to?: RouteLocationRaw }
  sortByActor: (a: ManagementAuditEvent, b: ManagementAuditEvent) => number
  sortByEventType: (a: ManagementAuditEvent, b: ManagementAuditEvent) => number
  sortByTemplate: (a: ManagementAuditEvent, b: ManagementAuditEvent) => number
  sortByEventAt: (a: ManagementAuditEvent, b: ManagementAuditEvent) => number
}>()

const emit = defineEmits<{
  'update:currentPage': [page: number]
}>()

const { t } = useI18n()
</script>

<template>
  <el-skeleton v-if="loading" :rows="6" animated />
  <template v-else>
    <AppDataTable :data="rows" empty-text="">
      <template #empty>
        <el-empty :description="t('audit.empty.management')" />
      </template>
      <el-table-column
        prop="actorSummary"
        sortable
        :sort-method="sortByActor"
        min-width="160"
        :label="t('audit.columns.actorSummary')"
      >
        <template #default="{ row }: { row: ManagementAuditEvent }">
          {{ formatActor(row) }}
        </template>
      </el-table-column>
      <el-table-column
        prop="eventType"
        sortable
        :sort-method="sortByEventType"
        min-width="180"
        :label="t('audit.columns.eventType')"
      >
        <template #default="{ row }: { row: ManagementAuditEvent }">
          {{ formatEventType(row.eventType) }}
        </template>
      </el-table-column>
      <el-table-column
        prop="templateId"
        sortable
        :sort-method="sortByTemplate"
        min-width="200"
        :label="t('audit.columns.templateId')"
      >
        <template #default="{ row }: { row: ManagementAuditEvent }">
          <EntityLinkCell v-bind="resolveTemplateCell(row)" />
        </template>
      </el-table-column>
      <el-table-column
        sortable
        :sort-method="sortByEventAt"
        min-width="180"
        :label="t('audit.columns.eventAt')"
      >
        <template #default="{ row }: { row: ManagementAuditEvent }">
          {{ formatDate(row.eventAt) }}
        </template>
      </el-table-column>
      <el-table-column
        prop="statusSummary"
        sortable
        min-width="160"
        :label="t('audit.columns.statusSummary')"
      />
    </AppDataTable>
    <AppTablePagination
      :current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      @update:current-page="emit('update:currentPage', $event)"
    />
  </template>
</template>
