import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { useCapabilities } from '@/composables/useCapabilities'
import { useTemplatePanelDataStore } from '@/stores/templatePanelData'
import {
  templateDevVersionPath,
  templatePackageHubPath,
} from '@/routing/routeKeys'
import type { TemplateDetail } from '@/types/template'
import { versionLineDisplayLabel } from '@/utils/templateVersionLine'
import {
  TEMPLATE_RELEASE_WORKSPACE_TAB_LABEL_KEYS,
  buildTemplateReleaseWorkspaceQuery,
  resolveTemplateReleaseWorkspaceTabFromQuery,
  type TemplateReleaseWorkspaceTab,
} from '@/views/templates/templateReleaseWorkspaceTabs'

export function useTemplateReleaseDetailView() {
  const { t } = useI18n()
  const { formatDateTime } = useLocaleFormatters()
  const route = useRoute()
  const router = useRouter()
  const { authorTemplates } = useCapabilities()
  const panelDataStore = useTemplatePanelDataStore()

  const loading = ref(false)
  const loadFailed = ref(false)
  const cloning = ref(false)
  const releaseDetail = ref<TemplateDetail | null>(null)
  const selectedPreviewId = ref<string | null>(null)
  const activeWorkspaceTab = ref<TemplateReleaseWorkspaceTab>(
    resolveTemplateReleaseWorkspaceTabFromQuery(route.query),
  )

  const templateId = computed(() => String(route.params.templateId ?? ''))
  const releaseVersion = computed(() => String(route.params.releaseVersion ?? ''))

  const workspaceTabs = computed(() =>
    (['basics', 'testing', 'approval', 'variables', 'bindings', 'rules'] as const).map((name) => ({
      name,
      labelKey: TEMPLATE_RELEASE_WORKSPACE_TAB_LABEL_KEYS[name],
    })),
  )

  const lineLabel = computed(() => {
    if (!releaseDetail.value) {
      return ''
    }
    return versionLineDisplayLabel(t, {
      devVersionId: releaseDetail.value.devVersionId,
      devVersionNumber: releaseDetail.value.devVersionNumber,
      releaseVersion: releaseDetail.value.releaseVersion ?? releaseVersion.value,
      lifecycleStatus: releaseDetail.value.lifecycleStatus,
      lineKind: 'PUBLISHED',
      updatedAt: releaseDetail.value.updatedAt,
      updatedBy: releaseDetail.value.updatedBy ?? '',
      defaultRouteTarget: true,
      cloneable: true,
    })
  })

  const variableColumns = computed(() => [
    { prop: 'variableKey', label: t('templates.releaseDetail.columns.variableKey') },
    { prop: 'variableType', label: t('templates.releaseDetail.columns.variableType') },
    { prop: 'required', label: t('templates.releaseDetail.columns.required') },
  ])

  const bindingColumns = computed(() => [
    { prop: 'anchorId', label: t('templates.releaseDetail.columns.anchorId') },
    { prop: 'declaredContentType', label: t('templates.releaseDetail.columns.contentType') },
  ])

  const ruleColumns = computed(() => [
    { prop: 'ruleId', label: t('templates.releaseDetail.columns.ruleId') },
    { prop: 'targetAnchorId', label: t('templates.releaseDetail.columns.targetAnchor') },
  ])

  const approvalSubStateLabel = computed(() => {
    const subState = releaseDetail.value?.approvalSubState
    if (!subState) {
      return t('templates.releaseDetail.approval.noApprovalSubState')
    }
    if (subState === 'PENDING_SUBMIT') {
      return t('templates.status.approvalPendingSubmit')
    }
    return t('templates.status.approvalPendingDecision')
  })

  watch(
    () => route.query.workspaceTab,
    () => {
      activeWorkspaceTab.value = resolveTemplateReleaseWorkspaceTabFromQuery(route.query)
    },
  )

  watch(activeWorkspaceTab, (tab) => {
    if (resolveTemplateReleaseWorkspaceTabFromQuery(route.query) === tab) {
      return
    }
    void router.replace({
      query: buildTemplateReleaseWorkspaceQuery(route.query, tab),
    })
  })

  onMounted(async () => {
    await loadReleaseDetail()
  })

  async function loadReleaseDetail() {
    loading.value = true
    loadFailed.value = false
    try {
      releaseDetail.value = await panelDataStore.fetchReleaseVersionDetail(
        templateId.value,
        releaseVersion.value,
      )
    } catch {
      loadFailed.value = true
      releaseDetail.value = null
    } finally {
      loading.value = false
    }
  }

  function backToHub() {
    router.push(templatePackageHubPath(templateId.value))
  }

  async function handleClone() {
    if (!releaseVersion.value) {
      return
    }
    cloning.value = true
    try {
      const created = await panelDataStore.cloneReleaseVersion(templateId.value, releaseVersion.value)
      ElMessage.success(t('templates.versionLines.cloneSuccess'))
      router.push(templateDevVersionPath(templateId.value, created.devVersionId))
    } catch {
      ElMessage.error(t('templates.versionLines.cloneError'))
    } finally {
      cloning.value = false
    }
  }

  function handleOpenPreview(payload: { previewId: string }) {
    selectedPreviewId.value = payload.previewId
  }

  function handleOpenDataSet() {
    ElMessage.info(t('templates.releaseDetail.testing.openDataSetReadOnly'))
  }

  return {
    t,
    formatDateTime,
    authorTemplates,
    loading,
    loadFailed,
    cloning,
    releaseDetail,
    selectedPreviewId,
    activeWorkspaceTab,
    templateId,
    releaseVersion,
    workspaceTabs,
    lineLabel,
    variableColumns,
    bindingColumns,
    ruleColumns,
    approvalSubStateLabel,
    loadReleaseDetail,
    backToHub,
    handleClone,
    handleOpenPreview,
    handleOpenDataSet,
  }
}
