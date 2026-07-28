<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import TemplateStatusBadge from '@/components/templates/TemplateStatusBadge.vue'
import { useCapabilities } from '@/composables/useCapabilities'
import { apiPackageSettingsPath } from '@/routing/apiPackageSettings'
import type { TemplateVersionLineSummary } from '@/types/template'
import { isInFlightVersionLine } from '@/utils/templateVersionLine'
import { resolveUpdatedByDisplay } from '@/utils/userDisplay'

const currentPage = defineModel<number>('currentPage', { required: true })

const props = defineProps<{
  templateId?: string
  versionLines: TemplateVersionLineSummary[]
  showPagination: boolean
  pageSize: number
  totalElements: number
  cloningReleaseVersion: string | null
  abandoningDevVersionId: string | null
  submitting: boolean
  lineLabel: (row: TemplateVersionLineSummary) => string
  formatDateTime: (value: string) => string
  canCloneRow: (row: TemplateVersionLineSummary) => boolean
  canAbandonRow: (row: TemplateVersionLineSummary) => boolean
  canDeactivateRow: (row: TemplateVersionLineSummary) => boolean
  canRestoreRow: (row: TemplateVersionLineSummary) => boolean
  onRowClick: (row: TemplateVersionLineSummary, event: Event) => void
}>()

const router = useRouter()
const { manageApiPolicy } = useCapabilities()

function openApiPerspective(row: TemplateVersionLineSummary) {
  if (!props.templateId || !row.releaseVersion) {
    return
  }
  void router.push(
    apiPackageSettingsPath(props.templateId, {
      releaseVersion: row.releaseVersion,
      panel: 'routes',
    }),
  )
}

const emit = defineEmits<{
  open: [row: TemplateVersionLineSummary]
  abandon: [row: TemplateVersionLineSummary]
  deactivate: [releaseVersion: string]
  restore: [releaseVersion: string]
  clone: [row: TemplateVersionLineSummary]
}>()

const { t } = useI18n()
</script>

<template>
  <AppDataTable
    activatable
    :data="versionLines"
    @row-click="onRowClick"
  >
    <template #empty>
      <el-empty :description="t('templates.versionLines.empty')" />
    </template>
    <el-table-column min-width="200" :label="t('templates.versionLines.line')">
      <template #default="{ row }">
        <span>{{ lineLabel(row) }}</span>
        <el-tag
          v-if="isInFlightVersionLine(row)"
          size="small"
          type="success"
          class="line-tag"
        >
          {{ t('templates.versionLines.inFlightBadge') }}
        </el-tag>
        <el-tag v-else size="small" type="info" class="line-tag">
          {{ t('templates.versionLines.publishedBadge') }}
        </el-tag>
        <el-tag
          v-if="row.defaultRouteTarget"
          size="small"
          type="warning"
          class="line-tag"
        >
          {{ t('templates.versionLines.defaultRouteBadge') }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column min-width="140" :label="t('templates.versionLines.status')">
      <template #default="{ row }">
        <TemplateStatusBadge
          :status="row.lifecycleStatus"
          :approval-sub-state="row.approvalSubState"
        />
      </template>
    </el-table-column>
    <el-table-column
      v-if="templateId && manageApiPolicy"
      min-width="160"
      :label="t('templates.versionLines.apiPerspective')"
    >
      <template #default="{ row }">
        <template v-if="!isInFlightVersionLine(row) && row.releaseVersion">
          <span
            class="api-perspective"
            data-testid="version-line-api-perspective"
          >
            {{
              row.defaultRouteTarget
                ? t('templates.versionLines.apiPerspectiveDefault')
                : t('templates.versionLines.apiPerspectiveRouted')
            }}
          </span>
          <el-button
            link
            type="primary"
            data-testid="version-line-api-settings-link"
            @click.stop="openApiPerspective(row)"
          >
            {{ t('templates.versionLines.apiSettingsLink') }}
          </el-button>
        </template>
        <span v-else class="api-perspective-muted">—</span>
      </template>
    </el-table-column>
    <el-table-column
      prop="devVersionNumber"
      width="120"
      :label="t('templates.versionLines.devVersionNumber')"
    />
    <el-table-column min-width="170" :label="t('templates.versionLines.updatedAt')">
      <template #default="{ row }">
        {{ formatDateTime(row.updatedAt) }}
      </template>
    </el-table-column>
    <el-table-column min-width="120" :label="t('templates.versionLines.updatedBy')">
      <template #default="{ row }">
        {{ resolveUpdatedByDisplay(row.updatedBy, row.updatedByDisplayName) }}
      </template>
    </el-table-column>
    <el-table-column min-width="280" :label="t('templates.versionLines.actions')">
      <template #default="{ row }">
        <el-button link type="primary" @click.stop="emit('open', row)">
          {{ t('templates.versionLines.viewDetail') }}
        </el-button>
        <el-button
          v-if="canAbandonRow(row)"
          link
          type="danger"
          data-version-line-abandon
          :loading="abandoningDevVersionId === row.devVersionId"
          @click.stop="emit('abandon', row)"
        >
          {{ t('templates.versionLines.abandon') }}
        </el-button>
        <el-tooltip
          v-if="canDeactivateRow(row)"
          :disabled="!row.defaultRouteTarget"
          :content="t('templates.versionLines.deactivateDisabledTooltip')"
        >
          <el-button
            link
            type="warning"
            data-version-line-deactivate
            :disabled="row.defaultRouteTarget === true"
            :loading="submitting"
            @click.stop="row.releaseVersion && emit('deactivate', row.releaseVersion)"
          >
            {{ t('templates.versionLines.deactivate') }}
          </el-button>
        </el-tooltip>
        <el-button
          v-if="canRestoreRow(row)"
          link
          type="primary"
          data-version-line-restore
          :loading="submitting"
          @click.stop="row.releaseVersion && emit('restore', row.releaseVersion)"
        >
          {{ t('templates.versionLines.restore') }}
        </el-button>
        <el-button
          v-if="canCloneRow(row)"
          link
          type="primary"
          data-version-line-clone
          :loading="cloningReleaseVersion === row.releaseVersion"
          @click.stop="emit('clone', row)"
        >
          {{ t('templates.versionLines.clone') }}
        </el-button>
      </template>
    </el-table-column>
  </AppDataTable>

  <AppTablePagination
    v-if="showPagination"
    v-model:current-page="currentPage"
    :page-size="pageSize"
    :total="totalElements"
  />
</template>

<style scoped lang="scss">
.line-tag {
  margin-left: var(--space-2);
}

.api-perspective {
  display: block;
  font-size: var(--font-size-sm);
  color: var(--text-secondary);
}

.api-perspective-muted {
  color: var(--text-muted);
}
</style>
