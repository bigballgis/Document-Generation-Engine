<script setup lang="ts">
/* eslint-disable vue/no-mutating-props -- reactive form/filter bag owned by parent */
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import TemplateStatusBadge from '@/components/templates/TemplateStatusBadge.vue'
import type { TemplateReleaseVersion } from '@/types/template'

defineProps<{
  paginatedVersions: TemplateReleaseVersion[]
  columnFilters: Record<string, string>
  lifecycleStatusFilterOptions: Array<{ label: string; value: string }>
  defaultRouteFilterOptions: Array<{ label: string; value: string }>
  canManageVersions: boolean
  submitting: boolean
  versionsCurrentPage: number
  pageSize: number
  totalVersionRows: number
  formatDateTime: (value: string) => string
  sortByDevVersion: (a: TemplateReleaseVersion, b: TemplateReleaseVersion) => number
  sortByLifecycleStatus: (a: TemplateReleaseVersion, b: TemplateReleaseVersion) => number
  sortByUpdatedAt: (a: TemplateReleaseVersion, b: TemplateReleaseVersion) => number
  resolveUpdatedByDisplay: (updatedBy: string, displayName: string | null | undefined) => string
  t: (key: string) => string
}>()

const emit = defineEmits<{
  'update:versionsCurrentPage': [page: number]
  versionAction: [releaseVersion: string, action: 'deactivate' | 'restore']
}>()
</script>

<template>
  <div>
    <AppDataTable :data="paginatedVersions">
      <template #empty>
        <el-empty :description="t('templates.versions.empty')" />
      </template>
      <el-table-column prop="releaseVersion" sortable min-width="140">
        <template #header>
          <TableColumnHeader
            :label="t('templates.versions.releaseVersion')"
            v-model="columnFilters.releaseVersion"
          />
        </template>
      </el-table-column>
      <el-table-column
        prop="devVersionNumber"
        sortable
        width="120"
        :sort-method="sortByDevVersion"
      >
        <template #header>
          <TableColumnHeader
            :label="t('templates.versions.devVersionNumber')"
            v-model="columnFilters.devVersionNumber"
          />
        </template>
      </el-table-column>
      <el-table-column sortable :sort-method="sortByLifecycleStatus" width="160">
        <template #header>
          <TableColumnHeader
            :label="t('templates.versions.status')"
            v-model="columnFilters.status"
            filter-type="select"
            :options="lifecycleStatusFilterOptions"
          />
        </template>
        <template #default="{ row }">
          <TemplateStatusBadge :status="row.lifecycleStatus" />
        </template>
      </el-table-column>
      <el-table-column width="140">
        <template #header>
          <TableColumnHeader
            :label="t('templates.versions.defaultRoute')"
            v-model="columnFilters.defaultRoute"
            filter-type="select"
            :options="defaultRouteFilterOptions"
          />
        </template>
        <template #default="{ row }">
          <el-tag v-if="row.defaultRouteTarget" type="success" size="small">
            {{ t('templates.versions.defaultRouteYes') }}
          </el-tag>
          <span v-else>{{ t('templates.versions.defaultRouteNo') }}</span>
        </template>
      </el-table-column>
      <el-table-column sortable min-width="120">
        <template #header>
          <TableColumnHeader
            :label="t('templates.versions.updatedBy')"
            v-model="columnFilters.updatedBy"
          />
        </template>
        <template #default="{ row }">
          {{ resolveUpdatedByDisplay(row.updatedBy, row.updatedByDisplayName) }}
        </template>
      </el-table-column>
      <el-table-column sortable :sort-method="sortByUpdatedAt" min-width="180">
        <template #header>
          <TableColumnHeader
            :label="t('templates.versions.updatedAt')"
            v-model="columnFilters.updatedAt"
          />
        </template>
        <template #default="{ row }">
          {{ formatDateTime(row.updatedAt) }}
        </template>
      </el-table-column>
      <el-table-column
        v-if="canManageVersions"
        :label="t('templates.versions.actions')"
        min-width="220"
      >
        <template #default="{ row }">
          <el-button
            v-if="row.lifecycleStatus === 'PUBLISHED'"
            link
            type="warning"
            :loading="submitting"
            @click="emit('versionAction', row.releaseVersion, 'deactivate')"
          >
            {{ t('templates.versions.deactivate') }}
          </el-button>
          <el-button
            v-if="row.lifecycleStatus === 'STOPPED'"
            link
            type="primary"
            :loading="submitting"
            @click="emit('versionAction', row.releaseVersion, 'restore')"
          >
            {{ t('templates.versions.restore') }}
          </el-button>
        </template>
      </el-table-column>
    </AppDataTable>
    <AppTablePagination
      :current-page="versionsCurrentPage"
      :page-size="pageSize"
      :total="totalVersionRows"
      @update:current-page="emit('update:versionsCurrentPage', $event)"
    />
  </div>
</template>

