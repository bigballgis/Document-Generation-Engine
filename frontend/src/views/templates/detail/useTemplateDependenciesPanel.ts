import { computed, onMounted, ref, watch, type Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import {
  fetchReleaseVersionDetail,
  listTemplateContentModuleReferences,
  listTemplateVersionLines,
} from '@/api/templates'
import { getMaster, listMasterRevisionLines } from '@/api/masters'
import { useEntityLinkTargets } from '@/composables/useEntityLinkTargets'
import { SERVER_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import {
  templateDevVersionPath,
  templateReleaseDetailPath,
} from '@/routing/routeKeys'
import type {
  AnchorBinding,
  TemplateContentModuleReference,
  TemplateDetail,
  TemplateMasterPin,
  TemplateVersionLineSummary,
} from '@/types/template'
import type { MasterRevisionLineSummary } from '@/types/master'
import {
  selectPinReleaseVersion,
  truncateMasterFileHash,
} from '@/utils/templateDependencyPin'
import { isInFlightVersionLine, versionLineDisplayLabel } from '@/utils/templateVersionLine'

export type DependencyAnchorRow = {
  anchorId: string
  declaredContentType: string
  displayLabel: string | null
}

export function useTemplateDependenciesPanel(template: Ref<TemplateDetail>) {
  const { t, te } = useI18n()
  const router = useRouter()
  const { masterDetailLink, contentModuleDetailLink } = useEntityLinkTargets()

  const masterName = ref<string | null>(null)
  const masterNameLoading = ref(false)

  const versionLines = ref<TemplateVersionLineSummary[]>([])
  const versionLinesLoading = ref(false)
  const versionLinesError = ref(false)

  const masterPin = ref<TemplateMasterPin | null>(null)
  const pinReleaseVersion = ref<string | null>(null)
  const pinLoading = ref(false)
  const pinError = ref(false)

  const workingRevision = ref<MasterRevisionLineSummary | null>(null)
  const workingRevisionLoading = ref(false)
  const workingRevisionError = ref(false)

  const clauseReferences = ref<TemplateContentModuleReference[]>([])
  const clauseLoading = ref(false)
  const clauseError = ref(false)

  const bindings = computed((): AnchorBinding[] => template.value.bindings ?? [])

  const anchorRows = computed((): DependencyAnchorRow[] =>
    bindings.value.map((binding) => ({
      anchorId: binding.anchorId,
      declaredContentType: binding.declaredContentType,
      displayLabel: null,
    })),
  )

  const pinHashSummary = computed(() => {
    const hash = masterPin.value?.masterFileHash
    return hash ? truncateMasterFileHash(hash) : null
  })

  const showPinnedMaster = computed(() => Boolean(masterPin.value?.masterRevisionId))
  const showNotPinned = computed(() => !pinLoading.value && !pinError.value && !showPinnedMaster.value)

  function resolveErrorMessage(fallbackKey: string): string {
    return te(fallbackKey) ? t(fallbackKey) : t('templates.error.loadDetail')
  }

  async function loadMasterName() {
    masterNameLoading.value = true
    try {
      const master = await getMaster(template.value.masterId)
      masterName.value = master.name
    } catch {
      masterName.value = null
    } finally {
      masterNameLoading.value = false
    }
  }

  async function loadVersionLines() {
    versionLinesLoading.value = true
    versionLinesError.value = false
    try {
      const page = await listTemplateVersionLines(
        template.value.id,
        0,
        SERVER_TABLE_PAGE_SIZE,
      )
      versionLines.value = page.content
    } catch {
      versionLines.value = []
      versionLinesError.value = true
    } finally {
      versionLinesLoading.value = false
    }
  }

  async function loadMasterPin() {
    pinLoading.value = true
    pinError.value = false
    masterPin.value = null
    pinReleaseVersion.value = null
    try {
      const releaseVersion = selectPinReleaseVersion(versionLines.value)
      pinReleaseVersion.value = releaseVersion
      if (!releaseVersion) {
        return
      }
      const detail = await fetchReleaseVersionDetail(template.value.id, releaseVersion)
      masterPin.value = detail.masterPin ?? null
    } catch {
      masterPin.value = null
      pinError.value = true
    } finally {
      pinLoading.value = false
    }
  }

  async function loadWorkingRevision() {
    workingRevision.value = null
    workingRevisionError.value = false
    if (showPinnedMaster.value) {
      workingRevisionLoading.value = false
      return
    }
    workingRevisionLoading.value = true
    try {
      const page = await listMasterRevisionLines(template.value.masterId, 0, 20)
      workingRevision.value = page.content.find((row) => row.current) ?? null
    } catch {
      workingRevision.value = null
      workingRevisionError.value = true
    } finally {
      workingRevisionLoading.value = false
    }
  }

  async function loadClauseReferences() {
    clauseLoading.value = true
    clauseError.value = false
    try {
      clauseReferences.value = await listTemplateContentModuleReferences(template.value.id)
    } catch {
      clauseReferences.value = []
      clauseError.value = true
    } finally {
      clauseLoading.value = false
    }
  }

  async function loadAll() {
    await Promise.all([loadMasterName(), loadVersionLines(), loadClauseReferences()])
    await loadMasterPin()
    await loadWorkingRevision()
  }

  async function retryVersionLines() {
    await loadVersionLines()
    await loadMasterPin()
    await loadWorkingRevision()
  }

  async function retryPin() {
    await loadMasterPin()
    await loadWorkingRevision()
  }

  async function retryClauses() {
    await loadClauseReferences()
  }

  function lineLabel(row: TemplateVersionLineSummary): string {
    return versionLineDisplayLabel(t, row)
  }

  function openVersionLine(row: TemplateVersionLineSummary) {
    if (isInFlightVersionLine(row)) {
      void router.push(templateDevVersionPath(template.value.id, row.devVersionId))
      return
    }
    if (row.releaseVersion) {
      void router.push(templateReleaseDetailPath(template.value.id, row.releaseVersion))
    }
  }

  onMounted(() => {
    void loadAll()
  })

  watch(
    () => template.value.id,
    () => {
      void loadAll()
    },
  )

  return {
    t,
    masterDetailLink,
    contentModuleDetailLink,
    masterName,
    masterNameLoading,
    versionLines,
    versionLinesLoading,
    versionLinesError,
    masterPin,
    pinReleaseVersion,
    pinLoading,
    pinError,
    pinHashSummary,
    showPinnedMaster,
    showNotPinned,
    workingRevision,
    workingRevisionLoading,
    workingRevisionError,
    clauseReferences,
    clauseLoading,
    clauseError,
    anchorRows,
    resolveErrorMessage,
    loadAll,
    retryVersionLines,
    retryPin,
    retryClauses,
    lineLabel,
    openVersionLine,
  }
}
