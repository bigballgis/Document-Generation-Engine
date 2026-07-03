<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import TemplateStatusBadge from '@/components/templates/TemplateStatusBadge.vue'
import { useActivatableTableRow } from '@/composables/useActivatableTableRow'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { SERVER_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import * as templatesApi from '@/api/templates'
import { useTemplatesStore } from '@/stores/templates'
import {
  templateDevVersionPath,
  templateReleaseDetailPath,
} from '@/routing/routeKeys'
import type { LifecycleGovernanceAction, TemplateVersionLineSummary } from '@/types/template'
import { isInFlightVersionLine, versionLineDisplayLabel } from '@/utils/templateVersionLine'

const props = defineProps<{
  templateId: string
  canClone?: boolean
  canManageVersions?: boolean
}>()

const emit = defineEmits<{
  cloned: []
  changed: []
}>()

const { t, te } = useI18n()
const { formatDateTime } = useLocaleFormatters()
const router = useRouter()
const templatesStore = useTemplatesStore()

const loading = ref(false)
const loadError = ref(false)
const cloningReleaseVersion = ref<string | null>(null)
const abandoningDevVersionId = ref<string | null>(null)
const currentPage = ref(1)
const totalElements = ref(0)
const totalPages = ref(0)
const versionLines = ref<TemplateVersionLineSummary[]>([])

const pageSize = SERVER_TABLE_PAGE_SIZE

const hasInFlightLine = computed(() => versionLines.value.some(isInFlightVersionLine))

const latestPublishedLine = computed(() =>
  versionLines.value.find((row) => !isInFlightVersionLine(row) && row.releaseVersion),
)

const errorMessage = computed(() => {
  const key = templatesStore.lastErrorMessageKey
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('templates.error.loadDetail')
})

function lineLabel(row: TemplateVersionLineSummary): string {
  return versionLineDisplayLabel(t, row)
}

async function loadVersionLines() {
  loading.value = true
  loadError.value = false
  try {
    const page = await templatesApi.listTemplateVersionLines(
      props.templateId,
      currentPage.value - 1,
      pageSize,
    )
    versionLines.value = page.content
    totalElements.value = page.totalElements
    totalPages.value = page.totalPages
  } catch {
    loadError.value = true
    versionLines.value = []
    totalElements.value = 0
    totalPages.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadVersionLines()
})

watch(
  () => props.templateId,
  () => {
    currentPage.value = 1
    void loadVersionLines()
  },
)

watch(currentPage, () => {
  void loadVersionLines()
})

function openVersionLine(row: TemplateVersionLineSummary) {
  if (isInFlightVersionLine(row)) {
    router.push(templateDevVersionPath(props.templateId, row.devVersionId))
    return
  }
  if (row.releaseVersion) {
    router.push(templateReleaseDetailPath(props.templateId, row.releaseVersion))
  }
}

const { onRowClick } = useActivatableTableRow<TemplateVersionLineSummary>(openVersionLine)

const showPagination = computed(() => totalPages.value > 1)

const showCreateFromLatestRelease = computed(
  () => Boolean(props.canClone && !hasInFlightLine.value && latestPublishedLine.value?.cloneable !== false),
)

function canCloneRow(row: TemplateVersionLineSummary): boolean {
  return Boolean(
    props.canClone &&
      row.releaseVersion &&
      !isInFlightVersionLine(row) &&
      row.cloneable !== false &&
      !hasInFlightLine.value,
  )
}

function canAbandonRow(row: TemplateVersionLineSummary): boolean {
  return Boolean(props.canClone && isInFlightVersionLine(row))
}

function canDeactivateRow(row: TemplateVersionLineSummary): boolean {
  return Boolean(
    props.canManageVersions &&
      row.releaseVersion &&
      !isInFlightVersionLine(row) &&
      row.lifecycleStatus === 'PUBLISHED',
  )
}

function canRestoreRow(row: TemplateVersionLineSummary): boolean {
  return Boolean(
    props.canManageVersions &&
      row.releaseVersion &&
      !isInFlightVersionLine(row) &&
      row.lifecycleStatus === 'STOPPED',
  )
}

async function handleClone(row: TemplateVersionLineSummary) {
  if (!row.releaseVersion) {
    return
  }
  cloningReleaseVersion.value = row.releaseVersion
  try {
    const created = await templatesApi.cloneReleaseVersion(props.templateId, row.releaseVersion)
    ElMessage.success(t('templates.versionLines.cloneSuccess'))
    emit('cloned')
    router.push(templateDevVersionPath(props.templateId, created.devVersionId))
  } catch {
    ElMessage.error(t('templates.versionLines.cloneError'))
  } finally {
    cloningReleaseVersion.value = null
  }
}

async function handleCreateFromLatestRelease() {
  const row = latestPublishedLine.value
  if (!row?.releaseVersion) {
    return
  }
  await handleClone(row)
}

async function handleAbandon(row: TemplateVersionLineSummary) {
  try {
    await ElMessageBox.confirm(
      t('templates.versionLines.abandonConfirm'),
      t('templates.versionLines.abandon'),
      {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        type: 'warning',
      },
    )
  } catch {
    return
  }

  abandoningDevVersionId.value = row.devVersionId
  try {
    await templatesApi.abandonDevVersion(props.templateId, row.devVersionId)
    ElMessage.success(t('templates.versionLines.abandonSuccess'))
    emit('changed')
    await loadVersionLines()
  } catch {
    ElMessage.error(t('templates.versionLines.abandonError'))
  } finally {
    abandoningDevVersionId.value = null
  }
}

async function buildImpactPreviewMessage(
  action: LifecycleGovernanceAction,
  releaseVersion: string,
): Promise<string> {
  const preview = await templatesStore.fetchLifecycleImpactPreview(props.templateId, {
    action,
    releaseVersion,
  })
  const summary = te(preview.summaryMessageKey)
    ? t(preview.summaryMessageKey)
    : t(`templates.governance.impactSummary.${action}`)
  const callable = preview.callableReleaseVersions.length
    ? t('templates.governance.impactCallableVersions', {
        versions: preview.callableReleaseVersions.join(', '),
      })
    : t('templates.governance.impactNoCallableVersions')
  const defaultRoute = preview.defaultRouteReleaseVersion
    ? t('templates.governance.impactDefaultRoute', {
        version: preview.defaultRouteReleaseVersion,
      })
    : ''
  const routeImpact = preview.defaultRouteImpacted
    ? t('templates.governance.impactDefaultRouteAffected')
    : ''
  return [summary, callable, defaultRoute, routeImpact, t('templates.governance.impactConfirmPrompt')]
    .filter(Boolean)
    .join('\n\n')
}

async function handleVersionAction(
  releaseVersion: string,
  action: 'deactivate' | 'restore',
) {
  const previewAction: LifecycleGovernanceAction =
    action === 'deactivate' ? 'DEACTIVATE_VERSION' : 'RESTORE_VERSION'
  const reasonKey =
    action === 'deactivate'
      ? 'templates.versions.deactivateReasonPrompt'
      : 'templates.versions.restoreReasonPrompt'
  const titleKey =
    action === 'deactivate'
      ? 'templates.versions.deactivateTitle'
      : 'templates.versions.restoreTitle'
  const confirmTitleKey =
    action === 'deactivate'
      ? 'templates.versions.confirmDeactivateTitle'
      : 'templates.versions.confirmRestoreTitle'
  const confirmMessageKey =
    action === 'deactivate'
      ? 'templates.versions.confirmDeactivateMessage'
      : 'templates.versions.confirmRestoreMessage'
  const successKey =
    action === 'deactivate'
      ? 'templates.versions.deactivateSuccess'
      : 'templates.versions.restoreSuccess'

  let reason = ''
  try {
    const result = await ElMessageBox.prompt(t(reasonKey), t(titleKey), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      inputValidator: (value) =>
        value.trim().length > 0 ? true : t('templates.lifecycle.reasonRequired'),
    })
    reason = result.value.trim()
  } catch {
    return
  }

  try {
    const impactMessage = await buildImpactPreviewMessage(previewAction, releaseVersion)
    const confirmBody = [impactMessage, t(confirmMessageKey)].join('\n\n')
    await ElMessageBox.confirm(confirmBody, t(confirmTitleKey), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: 'warning',
    })
  } catch {
    return
  }

  const payload = { reason, confirmed: true }
  try {
    if (action === 'deactivate') {
      await templatesStore.deactivateTemplateVersion(props.templateId, releaseVersion, payload)
    } else {
      await templatesStore.restoreTemplateVersion(props.templateId, releaseVersion, payload)
    }
    ElMessage.success(t(successKey))
    emit('changed')
    await loadVersionLines()
  } catch {
    ElMessage.error(errorMessage.value || t('templates.error.lifecycle'))
  }
}

defineExpose({
  reload: loadVersionLines,
})
</script>

<template>
  <el-card shadow="never" class="version-lines-card">
    <template #header>
      <div class="card-header">
        <div class="card-header-row">
          <span>{{ t('templates.versionLines.title') }}</span>
          <el-button
            v-if="showCreateFromLatestRelease"
            type="primary"
            data-version-line-create-from-latest
            :loading="cloningReleaseVersion === latestPublishedLine?.releaseVersion"
            @click="handleCreateFromLatestRelease"
          >
            {{ t('templates.versionLines.createFromLatestRelease') }}
          </el-button>
        </div>
        <p class="card-hint">{{ t('templates.versionLines.hint') }}</p>
      </div>
    </template>

    <LoadErrorPanel
      v-if="loadError"
      message-key="templates.versionLines.loadError"
      @retry="loadVersionLines"
    />

    <el-skeleton v-else-if="loading" :rows="4" animated />

    <template v-else>
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
          prop="devVersionNumber"
          width="120"
          :label="t('templates.versionLines.devVersionNumber')"
        />
        <el-table-column min-width="170" :label="t('templates.versionLines.updatedAt')">
          <template #default="{ row }">
            {{ formatDateTime(row.updatedAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="updatedBy" min-width="120" :label="t('templates.versionLines.updatedBy')" />
        <el-table-column min-width="280" :label="t('templates.versionLines.actions')">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="openVersionLine(row)">
              {{ t('templates.versionLines.viewDetail') }}
            </el-button>
            <el-button
              v-if="canAbandonRow(row)"
              link
              type="danger"
              data-version-line-abandon
              :loading="abandoningDevVersionId === row.devVersionId"
              @click.stop="handleAbandon(row)"
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
                :loading="templatesStore.submitting"
                @click.stop="row.releaseVersion && handleVersionAction(row.releaseVersion, 'deactivate')"
              >
                {{ t('templates.versionLines.deactivate') }}
              </el-button>
            </el-tooltip>
            <el-button
              v-if="canRestoreRow(row)"
              link
              type="primary"
              data-version-line-restore
              :loading="templatesStore.submitting"
              @click.stop="row.releaseVersion && handleVersionAction(row.releaseVersion, 'restore')"
            >
              {{ t('templates.versionLines.restore') }}
            </el-button>
            <el-button
              v-if="canCloneRow(row)"
              link
              type="primary"
              data-version-line-clone
              :loading="cloningReleaseVersion === row.releaseVersion"
              @click.stop="handleClone(row)"
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
  </el-card>
</template>

<style scoped lang="scss">
.version-lines-card {
  margin-bottom: var(--space-4);
}

.card-header {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.card-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

.card-hint {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}

.line-tag {
  margin-left: var(--space-2);
}
</style>
