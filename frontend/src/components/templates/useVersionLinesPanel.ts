import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useActivatableTableRow } from '@/composables/useActivatableTableRow'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { SERVER_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import { useTemplatePanelDataStore } from '@/stores/templatePanelData'
import { useTemplatesStore } from '@/stores/templates'
import {
  templateDevVersionPath,
  templateReleaseDetailPath,
} from '@/routing/routeKeys'
import type { LifecycleGovernanceAction, TemplateVersionLineSummary } from '@/types/template'
import { isInFlightVersionLine, versionLineDisplayLabel } from '@/utils/templateVersionLine'

export type VersionLinesPanelProps = {
  templateId: string
  canClone?: boolean
  canManageVersions?: boolean
}

export type VersionLinesPanelEmit = {
  (e: 'cloned'): void
  (e: 'changed'): void
}

export function useVersionLinesPanel(
  props: VersionLinesPanelProps,
  emit: VersionLinesPanelEmit,
) {
  const { t, te } = useI18n()
  const { formatDateTime } = useLocaleFormatters()
  const router = useRouter()
  const templatesStore = useTemplatesStore()
  const panelDataStore = useTemplatePanelDataStore()

  const loadError = ref(false)
  const cloningReleaseVersion = ref<string | null>(null)
  const abandoningDevVersionId = ref<string | null>(null)
  const currentPage = ref(1)

  const pageSize = SERVER_TABLE_PAGE_SIZE
  const entry = computed(() => panelDataStore.getEntry(props.templateId))
  const loading = computed(() => entry.value.loadingVersionLines)
  const versionLinesCache = computed(() => entry.value.versionLines)
  const versionLines = computed(() => versionLinesCache.value?.content ?? [])
  const totalElements = computed(() => versionLinesCache.value?.totalElements ?? 0)
  const totalPages = computed(() => versionLinesCache.value?.totalPages ?? 0)

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
    loadError.value = false
    try {
      await panelDataStore.fetchVersionLines(props.templateId, currentPage.value - 1, pageSize)
    } catch {
      loadError.value = true
      panelDataStore.invalidateVersionLineDomains(props.templateId)
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
      const created = await panelDataStore.cloneReleaseVersion(props.templateId, row.releaseVersion)
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
      await panelDataStore.abandonDevVersion(props.templateId, row.devVersionId)
      ElMessage.success(t('templates.versionLines.abandonSuccess'))
      emit('changed')
      panelDataStore.invalidateVersionLineDomains(props.templateId)
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
      panelDataStore.invalidateVersionLineDomains(props.templateId)
      await loadVersionLines()
    } catch {
      ElMessage.error(errorMessage.value || t('templates.error.lifecycle'))
    }
  }

  return {
    templatesStore,
    formatDateTime,
    loadError,
    cloningReleaseVersion,
    abandoningDevVersionId,
    currentPage,
    pageSize,
    loading,
    versionLines,
    totalElements,
    latestPublishedLine,
    lineLabel,
    loadVersionLines,
    openVersionLine,
    onRowClick,
    showPagination,
    showCreateFromLatestRelease,
    canCloneRow,
    canAbandonRow,
    canDeactivateRow,
    canRestoreRow,
    handleClone,
    handleCreateFromLatestRelease,
    handleAbandon,
    handleVersionAction,
    isInFlightVersionLine,
  }
}
