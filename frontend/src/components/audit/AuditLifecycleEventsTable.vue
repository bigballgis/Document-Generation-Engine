<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import type { RouteLocationRaw } from 'vue-router'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import EntityLinkCell from '@/components/common/EntityLinkCell.vue'
import type { LifecycleAuditEvent, ManagementAuditEvent } from '@/types/audit'
import type { AuditActorDisplayFields } from '@/utils/auditEntityDisplay'

defineProps<{
  loading: boolean
  rows: LifecycleAuditEvent[]
  currentPage: number
  pageSize: number
  total: number
  formatActor: (event: AuditActorDisplayFields) => string
  formatEventType: (eventType?: string) => string
  formatDate: (value: string) => string
  formatLifecycleState: (state?: string) => string
  resolveTemplateCell: (
    event: Pick<ManagementAuditEvent, 'templateId' | 'templateDisplayName' | 'templateExternalId'>,
  ) => { label: string; subtitle?: string; to?: RouteLocationRaw }
  sortByActor: (a: LifecycleAuditEvent, b: LifecycleAuditEvent) => number
  sortByEventType: (a: LifecycleAuditEvent, b: LifecycleAuditEvent) => number
  sortByTemplate: (a: LifecycleAuditEvent, b: LifecycleAuditEvent) => number
  sortFromState: (a: LifecycleAuditEvent, b: LifecycleAuditEvent) => number
  sortToState: (a: LifecycleAuditEvent, b: LifecycleAuditEvent) => number
  sortByEventAt: (a: LifecycleAuditEvent, b: LifecycleAuditEvent) => number
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
        <el-empty :description="t('audit.empty.lifecycle')" />
      </template>
      <el-table-column
        prop="actorId"
        sortable
        :sort-method="sortByActor"
        min-width="140"
        :label="t('audit.columns.actorSummary')"
      >
        <template #default="{ row }: { row: LifecycleAuditEvent }">
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
        <template #default="{ row }: { row: LifecycleAuditEvent }">
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
        <template #default="{ row }: { row: LifecycleAuditEvent }">
          <EntityLinkCell v-bind="resolveTemplateCell(row)" />
        </template>
      </el-table-column>
      <el-table-column
        sortable
        :sort-method="sortFromState"
        width="140"
        :label="t('audit.columns.fromState')"
      >
        <template #default="{ row }: { row: LifecycleAuditEvent }">
          {{ formatLifecycleState(row.fromState) }}
        </template>
      </el-table-column>
      <el-table-column
        sortable
        :sort-method="sortToState"
        width="140"
        :label="t('audit.columns.toState')"
      >
        <template #default="{ row }: { row: LifecycleAuditEvent }">
          {{ formatLifecycleState(row.toState) }}
        </template>
      </el-table-column>
      <el-table-column
        prop="summary"
        sortable
        min-width="200"
        :label="t('audit.columns.summary')"
      />
      <el-table-column
        sortable
        :sort-method="sortByEventAt"
        min-width="180"
        :label="t('audit.columns.eventAt')"
      >
        <template #default="{ row }: { row: LifecycleAuditEvent }">
          {{ formatDate(row.eventAt) }}
        </template>
      </el-table-column>
    </AppDataTable>
    <AppTablePagination
      :current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      @update:current-page="emit('update:currentPage', $event)"
    />
  </template>
</template>
