import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useActivatableTableRow } from '@/composables/useActivatableTableRow'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { SERVER_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import { useTemplatePanelDataStore } from '@/stores/templatePanelData'
import { useTemplatesStore } from '@/stores/templates'
import { useVersionLinesActions } from '@/components/templates/useVersionLinesActions'
import {
  templateDevVersionPath,
  templateReleaseDetailPath,
} from '@/routing/routeKeys'
import type { TemplateVersionLineSummary } from '@/types/template'
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

  /**
   * Whole-collection in-flight signal: current page may omit IN_FLIGHT when paginated.
   * Published rows already carry `cloneable=false` when any in-flight exists (API `hasInFlight`).
   */
  const hasInFlightLine = computed(() => {
    if (versionLines.value.some(isInFlightVersionLine)) {
      return true
    }
    if (!props.canClone) {
      return false
    }
    return versionLines.value.some(
      (row) =>
        !isInFlightVersionLine(row) && Boolean(row.releaseVersion) && row.cloneable === false,
    )
  })

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
    () => Boolean(props.canClone && latestPublishedLine.value),
  )

  const createFromLatestReleaseDisabled = computed(() => hasInFlightLine.value)

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

  const { handleClone, handleCreateFromLatestRelease, handleAbandon, handleVersionAction } =
    useVersionLinesActions({
      templateId: () => props.templateId,
      latestPublishedLine,
      errorMessage,
      cloningReleaseVersion,
      abandoningDevVersionId,
      loadVersionLines,
      emitCloned: () => emit('cloned'),
      emitChanged: () => emit('changed'),
    })

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
    createFromLatestReleaseDisabled,
    hasInFlightLine,
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
