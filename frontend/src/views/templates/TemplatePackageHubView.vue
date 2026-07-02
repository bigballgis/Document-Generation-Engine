<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import TemplateStatusBadge from '@/components/templates/TemplateStatusBadge.vue'
import TemplateWorkflowBanner from '@/components/templates/TemplateWorkflowBanner.vue'
import TemplateVersionLinesPanel from '@/components/templates/TemplateVersionLinesPanel.vue'
import TemplateExportActions from '@/components/templates/TemplateExportActions.vue'
import TemplateMetadataEditDialog from '@/components/templates/TemplateMetadataEditDialog.vue'
import TemplateAuthorJourneyBlock from '@/components/journey/TemplateAuthorJourneyBlock.vue'
import TemplateTesterJourneyBlock from '@/components/journey/TemplateTesterJourneyBlock.vue'
import TemplateApproverJourneyBlock from '@/components/journey/TemplateApproverJourneyBlock.vue'
import TemplateTeamLeadJourneyBlock from '@/components/journey/TemplateTeamLeadJourneyBlock.vue'
import TemplateDetailOverviewTab from '@/views/templates/detail/TemplateDetailOverviewTab.vue'
import TemplateDetailApiAccessTab from '@/views/templates/detail/TemplateDetailApiAccessTab.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import { DEFAULT_ENVIRONMENT, type RuntimeEnvironment } from '@/config/environments'
import { useCapabilities } from '@/composables/useCapabilities'
import { useConfirmAction } from '@/composables/useConfirmAction'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { rowSortMethod, useDataTableFilters } from '@/composables/useDataTableFilters'
import { useCatalogPagination } from '@/composables/useCatalogPagination'
import { useCredentialStatusFilterOptions } from '@/composables/useTableFilterOptions'
import { CLIENT_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import {
  ROUTE_PATH_BY_KEY,
  ROUTE_KEYS,
  apiPolicyDetailPath,
  templateDevVersionPath,
  templatePackageHubPath,
} from '@/routing/routeKeys'
import { useTemplatesStore } from '@/stores/templates'
import { useSessionStore } from '@/stores/session'
import { useCollaborationStore } from '@/stores/collaboration'
import { canViewCollaborationWorkItems } from '@/auth/roles'
import {
  isTemplateInRemediation,
  shouldShowTemplateAuthorJourney,
  type TemplateAuthorJourneyContext,
} from '@/utils/templateAuthorJourney'
import {
  shouldShowTemplateTesterJourney,
  type TemplateTesterJourneyContext,
} from '@/utils/templateTesterJourney'
import {
  shouldShowTemplateApproverJourney,
  type TemplateApproverJourneyContext,
} from '@/utils/templateApproverJourney'
import {
  shouldShowTemplateTeamLeadJourney,
  type TemplateTeamLeadJourneyContext,
} from '@/utils/templateTeamLeadJourney'
import { isTemplateExportEligible } from '@/utils/templateExportEligibility'
import { templateDetailTabLabelKey } from '@/views/templates/templateDetailTabs'
import type { TemplateDevWorkspaceTab } from '@/views/templates/templateDevWorkspaceTabs'
import { resolveTemplateDevWorkspaceTab } from '@/views/templates/templateDevWorkspaceTabs'
import type { TemplateJourneyWorkspaceQuery } from '@/utils/templateJourneyWorkspaceLink'
import type { ApiCredentialSummary, DeleteTemplatePayload } from '@/types/template'
import type { ComponentPublicInstance } from 'vue'

const HUB_SECONDARY_TABS = ['overview', 'lifecycle', 'apiAccess'] as const
type HubSecondaryTab = (typeof HUB_SECONDARY_TABS)[number]

const { t, te } = useI18n()
const { formatDateTime } = useLocaleFormatters()
const route = useRoute()
const router = useRouter()
const templatesStore = useTemplatesStore()
const sessionStore = useSessionStore()
const collaborationStore = useCollaborationStore()
const {
  authorTemplates,
  decideTests,
  decideApprovals,
  publishTemplates,
  reviewMasters,
  manageApiPolicy,
  deleteTemplates,
  exportTemplates,
  editTemplateMetadata,
  context,
} = useCapabilities()
const { confirmAction } = useConfirmAction()

const metadataEditOpen = ref(false)
const loadFailed = ref(false)
const policyLoadFailed = ref(false)
const credentialSecretDialogVisible = ref(false)
const credentialSecretValue = ref('')
const credentialSecretExternalId = ref('')
const selectedContractEnvironment = ref<RuntimeEnvironment>(DEFAULT_ENVIRONMENT)
const credentialsCurrentPage = ref(1)
const versionLinesPanelRef = ref<ComponentPublicInstance<{ reload: () => Promise<void> }> | null>(
  null,
)

const templateId = computed(() => String(route.params.templateId ?? ''))
const secondaryTab = ref<HubSecondaryTab | undefined>(undefined)

const template = computed(() => {
  const selected = templatesStore.selectedTemplate
  if (!selected || selected.id !== templateId.value) {
    return null
  }
  return selected
})

const showDetailSkeleton = computed(
  () =>
    templatesStore.loadingDetail ||
    (templatesStore.selectedTemplate !== null && templatesStore.selectedTemplate.id !== templateId.value),
)

const errorMessage = computed(() => {
  const key = templatesStore.lastErrorMessageKey
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('templates.error.loadDetail')
})

const credentialsSource = computed(() => templatesStore.credentials)
const { filters: credentialColumnFilters, filteredRows: filteredCredentials } = useDataTableFilters(
  credentialsSource,
  [
    { key: 'externalId', getValue: (row) => row.externalId },
    { key: 'status', getValue: (row) => row.status, matchMode: 'exact' },
    { key: 'createdAt', getValue: (row) => formatDateTime(row.createdAt) },
  ],
)
const { paginatedRows: paginatedCredentials, totalRows: totalCredentialRows } = useCatalogPagination(
  filteredCredentials,
  credentialsCurrentPage,
  CLIENT_TABLE_PAGE_SIZE,
)
const sortCredentialsByCreatedAt = rowSortMethod<ApiCredentialSummary>((row) => row.createdAt)
const credentialStatusFilterOptions = useCredentialStatusFilterOptions()

const showMetadataEdit = computed(() => {
  const status = template.value?.lifecycleStatus
  if (!status || !editTemplateMetadata.value) {
    return false
  }
  return status !== 'PUBLISHED' && status !== 'STOPPED' && status !== 'DEPRECATED'
})
const showDeleteTemplateAction = computed(
  () => deleteTemplates.value && template.value?.lifecycleStatus !== 'DELETED',
)
const showExportActions = computed(
  () =>
    exportTemplates.value &&
    Boolean(template.value) &&
    isTemplateExportEligible(template.value!.lifecycleStatus),
)
const showPolicyPanel = computed(
  () => template.value?.lifecycleStatus === 'PUBLISHED' && manageApiPolicy.value,
)

const openRemediationTemplateIds = computed(() => {
  const ids = new Set<string>()
  for (const item of collaborationStore.workItems) {
    if (item.queue === 'REMEDIATION') {
      ids.add(item.templateId)
    }
  }
  return ids
})

const showAuthorJourney = computed(() => {
  if (
    !template.value ||
    !shouldShowTemplateAuthorJourney({
      authorTemplates: authorTemplates.value,
      roles: sessionStore.session?.roles ?? [],
    })
  ) {
    return false
  }
  const status = template.value.lifecycleStatus
  if (
    status === 'PENDING_RELEASE' &&
    publishTemplates.value &&
    shouldShowTemplateTeamLeadJourney({
      publishTemplates: publishTemplates.value,
      reviewMasters: reviewMasters.value,
    })
  ) {
    return false
  }
  return status !== 'STOPPED' && status !== 'DEPRECATED' && status !== 'DELETED'
})

const authorJourneyContext = computed((): TemplateAuthorJourneyContext | null => {
  if (!template.value) {
    return null
  }
  return {
    lifecycleStatus: template.value.lifecycleStatus,
    approvalSubState: template.value.approvalSubState,
    bindingsCount: template.value.bindings.length,
    hasSuccessfulTrialOutput: false,
    isRemediation: isTemplateInRemediation(template.value.id, openRemediationTemplateIds.value),
  }
})

const showTesterJourney = computed(
  () =>
    Boolean(
      template.value &&
        shouldShowTemplateTesterJourney({ decideTests: decideTests.value }) &&
        template.value.lifecycleStatus === 'TESTING',
    ),
)

const testerJourneyContext = computed((): TemplateTesterJourneyContext | null => {
  if (!template.value || template.value.lifecycleStatus !== 'TESTING') {
    return null
  }
  return {
    lifecycleStatus: 'TESTING',
    hasPreviewArtifact: false,
    fidelityViewedConfirmed: false,
    coverageViewedConfirmed: false,
    previewViewedConfirmed: false,
  }
})

const showApproverJourney = computed(
  () =>
    Boolean(
      template.value &&
        shouldShowTemplateApproverJourney({ decideApprovals: decideApprovals.value }) &&
        template.value.lifecycleStatus === 'APPROVAL' &&
        template.value.approvalSubState === 'PENDING_DECISION',
    ),
)

const approverJourneyContext = computed((): TemplateApproverJourneyContext | null => {
  if (
    !template.value ||
    template.value.lifecycleStatus !== 'APPROVAL' ||
    template.value.approvalSubState !== 'PENDING_DECISION'
  ) {
    return null
  }
  return {
    lifecycleStatus: 'APPROVAL',
    approvalSubState: 'PENDING_DECISION',
    submissionReviewedConfirmed: false,
    keyEvidenceViewedConfirmed: false,
  }
})

const showTeamLeadJourney = computed(
  () =>
    Boolean(
      template.value &&
        publishTemplates.value &&
        template.value.lifecycleStatus === 'PENDING_RELEASE' &&
        shouldShowTemplateTeamLeadJourney({
          publishTemplates: publishTemplates.value,
          reviewMasters: reviewMasters.value,
        }),
    ),
)

const teamLeadJourneyContext = computed((): TemplateTeamLeadJourneyContext | null => {
  if (!template.value || template.value.lifecycleStatus !== 'PENDING_RELEASE') {
    return null
  }
  return {
    lifecycleStatus: 'PENDING_RELEASE',
    goLiveRequestReviewedConfirmed: false,
    preReleaseChecksViewed: false,
    publishGateReady: false,
  }
})

function resolveSecondaryTab(value: unknown): HubSecondaryTab | undefined {
  if (typeof value === 'string' && (HUB_SECONDARY_TABS as readonly string[]).includes(value)) {
    return value as HubSecondaryTab
  }
  return undefined
}

function syncSecondaryTabFromRoute() {
  if (route.query.tab === 'authoring') {
    void redirectAuthoringDeepLink()
    return
  }
  if (route.query.tab === 'lifecycle' || route.query.focus === 'lifecycle') {
    void redirectLifecycleDeepLink()
    return
  }
  if (route.query.tab === 'releaseVersions') {
    secondaryTab.value = undefined
    void router.replace(templatePackageHubPath(templateId.value))
    return
  }
  secondaryTab.value = resolveSecondaryTab(route.query.tab)
}

async function redirectLifecycleDeepLink() {
  try {
    if (!template.value) {
      await templatesStore.fetchTemplate(templateId.value)
    }
    openDevEditor('approval')
  } catch {
    await router.replace(templatePackageHubPath(templateId.value))
  }
}

async function redirectAuthoringDeepLink() {
  try {
    if (!template.value) {
      await templatesStore.fetchTemplate(templateId.value)
    }
    const devVersionId = templatesStore.selectedTemplate?.devVersionId
    if (devVersionId) {
      await router.replace(templateDevVersionPath(templateId.value, devVersionId))
    }
  } catch {
    await router.replace(templatePackageHubPath(templateId.value))
  }
}

onMounted(async () => {
  syncSecondaryTabFromRoute()
  if (
    route.query.tab === 'authoring' ||
    route.query.tab === 'lifecycle' ||
    route.query.focus === 'lifecycle'
  ) {
    return
  }
  await Promise.all([loadTemplate(), loadAuthorRemediationWorkItems()])
})

onUnmounted(() => {
  templatesStore.clearSelected()
})

watch(
  () => templateId.value,
  () => {
    void loadTemplate()
  },
)

watch(
  () => route.query,
  () => {
    syncSecondaryTabFromRoute()
  },
  { deep: true },
)

watch(secondaryTab, (tab) => {
  const queryTab = resolveSecondaryTab(route.query.tab)
  if (queryTab === tab) {
    return
  }
  if (!tab) {
    const query = { ...route.query }
    delete query.tab
    delete query.focus
    void router.replace({ query })
    return
  }
  void router.replace({ query: { ...route.query, tab } })
})

async function loadAuthorRemediationWorkItems() {
  if (!authorTemplates.value || !canViewCollaborationWorkItems(context.value)) {
    return
  }
  try {
    await collaborationStore.fetchWorkItems({ queue: 'REMEDIATION' })
  } catch {
    /* degrade */
  }
}

async function loadTemplate() {
  loadFailed.value = false
  try {
    await templatesStore.fetchTemplate(templateId.value)
    if (showPolicyPanel.value) {
      await loadPolicyData()
    }
  } catch {
    loadFailed.value = true
  }
}

async function loadPolicyData() {
  policyLoadFailed.value = false
  try {
    await Promise.all([
      templatesStore.fetchApiPolicy(templateId.value),
      templatesStore.fetchCredentials(templateId.value),
    ])
  } catch {
    policyLoadFailed.value = true
  }
}

function backToList() {
  router.push(ROUTE_PATH_BY_KEY[ROUTE_KEYS.templateManagement])
}

function openDevEditor(
  workspaceTab: TemplateDevWorkspaceTab = 'design',
  extraQuery?: Record<string, string>,
) {
  const devVersionId = template.value?.devVersionId
  if (!devVersionId) {
    return
  }
  router.push(
    templateDevVersionPath(templateId.value, devVersionId, undefined, {
      workspaceTab,
      ...extraQuery,
    }),
  )
}

function handleJourneyOpenWorkspace(query: TemplateJourneyWorkspaceQuery) {
  const { workspaceTab, ...extraQuery } = query
  openDevEditor(resolveTemplateDevWorkspaceTab(workspaceTab), extraQuery)
}

function openLifecyclePanel() {
  openDevEditor('approval')
}

function openApiPolicyConsole() {
  router.push(apiPolicyDetailPath(templateId.value))
}

async function handleMetadataUpdate(payload: { name: string; description: string | null }) {
  try {
    await templatesStore.updateTemplateMetadata(templateId.value, payload)
    metadataEditOpen.value = false
    ElMessage.success(t('templates.metadata.success'))
  } catch {
    ElMessage.error(errorMessage.value || t('templates.error.updateMetadata'))
  }
}

async function handleDeleteTemplate() {
  let reason = ''
  try {
    const result = await ElMessageBox.prompt(
      t('templates.deleteAction.reasonPrompt'),
      t('templates.deleteAction.title'),
      {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        inputValidator: (value) =>
          value.trim().length > 0 ? true : t('templates.deleteAction.reasonRequired'),
      },
    )
    reason = result.value.trim()
  } catch {
    return
  }

  const confirmed = await confirmAction({
    titleKey: 'templates.deleteAction.confirmTitle',
    messageKey: 'templates.deleteAction.confirmMessage',
    type: 'warning',
  })
  if (!confirmed) {
    return
  }

  try {
    const payload: DeleteTemplatePayload = { reason }
    await templatesStore.deleteTemplate(templateId.value, payload)
    ElMessage.success(t('templates.deleteAction.success'))
    router.push(ROUTE_PATH_BY_KEY[ROUTE_KEYS.templateManagement])
  } catch {
    ElMessage.error(errorMessage.value || t('templates.error.delete'))
  }
}

function openCredentialSecretDialog(externalId: string, secret: string) {
  credentialSecretExternalId.value = externalId
  credentialSecretValue.value = secret
  credentialSecretDialogVisible.value = true
}

async function handleCreateCredential() {
  try {
    const created = await templatesStore.createCredential(templateId.value)
    openCredentialSecretDialog(created.externalId, created.secret)
    ElMessage.success(t('templates.policy.createCredentialSuccess'))
  } catch {
    ElMessage.error(errorMessage.value || t('templates.error.createCredential'))
  }
}

async function handleRotateCredential(credentialId: string, externalId: string) {
  const confirmed = await confirmAction({
    titleKey: 'templates.policy.confirmRotateTitle',
    messageKey: 'templates.policy.confirmRotateMessage',
    type: 'warning',
  })
  if (!confirmed) {
    return
  }
  try {
    const rotated = await templatesStore.rotateCredential(templateId.value, credentialId)
    openCredentialSecretDialog(externalId, rotated.secret)
    ElMessage.success(t('templates.policy.rotateCredentialSuccess'))
  } catch {
    ElMessage.error(errorMessage.value || t('templates.error.rotateCredential'))
  }
}

async function handleRevokeCredential(credentialId: string) {
  const confirmed = await confirmAction({
    titleKey: 'templates.policy.confirmRevokeTitle',
    messageKey: 'templates.policy.confirmRevokeMessage',
    type: 'warning',
  })
  if (!confirmed) {
    return
  }
  try {
    await templatesStore.revokeCredential(templateId.value, credentialId)
    ElMessage.success(t('templates.policy.revokeCredentialSuccess'))
  } catch {
    ElMessage.error(errorMessage.value || t('templates.error.revokeCredential'))
  }
}

async function handleVersionLinesChanged() {
  await loadTemplate()
  await versionLinesPanelRef.value?.reload()
}

const displayedCredentialSecret = computed(() => {
  if (templatesStore.lastCreatedCredential?.secret) {
    return templatesStore.lastCreatedCredential.secret
  }
  return templatesStore.lastRotatedCredential?.secret ?? ''
})
</script>

<template>
  <div class="template-package-hub-page">
    <header class="page-header">
      <el-button link type="primary" @click="backToList">
        {{ t('templates.packageHub.backToList') }}
      </el-button>
      <div v-if="template" class="header-content">
        <div class="header-title-block">
          <el-tooltip :content="template.name" placement="top">
            <h1 class="template-name">{{ template.name }}</h1>
          </el-tooltip>
          <p>{{ t('templates.packageHub.groupLabel', { groupCode: template.groupCode }) }}</p>
          <p class="external-id">{{ t('templates.packageHub.externalIdLabel', { externalId: template.externalId }) }}</p>
        </div>
        <div class="header-actions">
          <TemplateExportActions
            v-if="showExportActions"
            :template-id="templateId"
            :external-id="template.externalId"
          />
          <el-button
            v-if="showDeleteTemplateAction"
            type="danger"
            plain
            :loading="templatesStore.submitting"
            @click="handleDeleteTemplate"
          >
            {{ t('templates.deleteAction.button') }}
          </el-button>
          <el-button v-if="showMetadataEdit" @click="metadataEditOpen = true">
            {{ t('templates.metadata.edit') }}
          </el-button>
          <TemplateStatusBadge
            :status="template.lifecycleStatus"
            :approval-sub-state="template.approvalSubState"
          />
        </div>
      </div>
      <h1 v-else>{{ t('templates.packageHub.loadingTitle') }}</h1>
    </header>

    <LoadErrorPanel
      v-if="loadFailed"
      :message-key="templatesStore.lastErrorMessageKey ?? 'templates.error.loadDetail'"
      @retry="loadTemplate"
    />

    <el-skeleton v-else-if="showDetailSkeleton" :rows="8" animated />

    <EmptyStatePanel
      v-else-if="!template"
      title-key="templates.empty.notFoundTitle"
      description-key="templates.empty.notFoundDescription"
    />

    <template v-else-if="template">
      <TemplateAuthorJourneyBlock
        v-if="showAuthorJourney && authorJourneyContext"
        :journey-context="authorJourneyContext"
        :template-id="templateId"
        :can-write="authorTemplates"
        :show-primary-cta="false"
        enable-workspace-link
        @open-workspace="handleJourneyOpenWorkspace"
      />

      <TemplateTesterJourneyBlock
        v-if="showTesterJourney && testerJourneyContext"
        :journey-context="testerJourneyContext"
        :can-decide="decideTests"
        :show-primary-cta="false"
        enable-workspace-link
        @open-workspace="handleJourneyOpenWorkspace"
      />

      <TemplateApproverJourneyBlock
        v-if="showApproverJourney && approverJourneyContext"
        :journey-context="approverJourneyContext"
        :can-decide="decideApprovals"
        :show-primary-cta="false"
        enable-workspace-link
        @open-workspace="handleJourneyOpenWorkspace"
      />

      <TemplateTeamLeadJourneyBlock
        v-if="showTeamLeadJourney && teamLeadJourneyContext"
        :journey-context="teamLeadJourneyContext"
        :can-publish="publishTemplates"
        :show-primary-cta="false"
        enable-workspace-link
        @open-workspace="handleJourneyOpenWorkspace"
      />

      <TemplateWorkflowBanner :template="template" @open-lifecycle="openLifecyclePanel" />

      <TemplateVersionLinesPanel
        ref="versionLinesPanelRef"
        :template-id="templateId"
        :can-clone="authorTemplates"
        @cloned="handleVersionLinesChanged"
      />

      <el-tabs
        :model-value="secondaryTab"
        class="secondary-tabs"
        @tab-change="(name: string | number) => (secondaryTab = String(name) as HubSecondaryTab)"
      >
        <el-tab-pane :label="t(templateDetailTabLabelKey('overview'))" name="overview">
          <TemplateDetailOverviewTab :template="template" :format-date-time="formatDateTime" />
        </el-tab-pane>

        <el-tab-pane :label="t(templateDetailTabLabelKey('lifecycle'))" name="lifecycle">
          <el-card shadow="never" class="lifecycle-link-card">
            <p>{{ t('templates.packageHub.lifecycleHint') }}</p>
            <el-button type="primary" @click="openLifecyclePanel">
              {{ t('templates.packageHub.openDevWorkflow') }}
            </el-button>
          </el-card>
        </el-tab-pane>

        <el-tab-pane
          v-if="showPolicyPanel"
          :label="t(templateDetailTabLabelKey('apiAccess'))"
          name="apiAccess"
        >
          <TemplateDetailApiAccessTab
            v-model:credential-column-filters="credentialColumnFilters"
            v-model:credentials-current-page="credentialsCurrentPage"
            v-model:selected-contract-environment="selectedContractEnvironment"
            :template-id="templateId"
            :show-policy-panel="showPolicyPanel"
            :loading-policy="templatesStore.loadingPolicy"
            :api-policy="templatesStore.apiPolicy"
            :policy-load-failed="policyLoadFailed"
            :policy-load-error-key="templatesStore.lastErrorMessageKey"
            :paginated-credentials="paginatedCredentials"
            :credential-status-filter-options="credentialStatusFilterOptions"
            :page-size="CLIENT_TABLE_PAGE_SIZE"
            :total-credential-rows="totalCredentialRows"
            :submitting="templatesStore.submitting"
            :format-date-time="formatDateTime"
            :sort-credentials-by-created-at="sortCredentialsByCreatedAt"
            @open-api-policy-console="openApiPolicyConsole"
            @create-credential="handleCreateCredential"
            @rotate-credential="handleRotateCredential"
            @revoke-credential="handleRevokeCredential"
            @retry-policy-load="loadPolicyData"
          />
        </el-tab-pane>
      </el-tabs>
    </template>

    <TemplateMetadataEditDialog
      v-if="template"
      v-model="metadataEditOpen"
      :initial-name="template.name"
      :initial-description="template.description"
      :loading="templatesStore.submitting"
      @submit="handleMetadataUpdate"
    />

    <el-dialog
      v-model="credentialSecretDialogVisible"
      :title="t('templates.policy.credentialSecretDialogTitle')"
      width="480px"
      :close-on-click-modal="false"
    >
      <p>{{ t('templates.policy.credentialSecretHint') }}</p>
      <p>{{ t('templates.policy.credentialExternalId') }}: {{ credentialSecretExternalId }}</p>
      <el-input
        :model-value="displayedCredentialSecret || credentialSecretValue"
        readonly
        type="textarea"
        :rows="3"
      />
      <template #footer>
        <el-button type="primary" @click="credentialSecretDialogVisible = false">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.template-package-hub-page {
  min-height: 100vh;
  padding: 2rem;
  background: var(--surface-bg);
}

.page-header {
  margin-bottom: 1.5rem;
}

.header-content {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-top: 0.5rem;
}

.header-title-block {
  min-width: 0;
  flex: 1 1 auto;
}

.template-name {
  margin: 0 0 0.25rem;
  font-size: 1.75rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}

.header-title-block p,
.external-id {
  margin: 0;
  color: var(--text-muted);
}

.external-id {
  margin-top: 0.35rem;
}

.header-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
}

.secondary-tabs {
  margin-top: 1rem;
}

.lifecycle-link-card {
  p {
    margin: 0 0 1rem;
    color: var(--text-muted);
  }
}
</style>
