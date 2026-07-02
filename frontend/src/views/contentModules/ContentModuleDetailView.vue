<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import AppDataTable from '@/components/common/AppDataTable.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import WorkspaceTabShell from '@/components/common/WorkspaceTabShell.vue'
import ContentModuleLifecycleImpactDialog from '@/components/contentModules/ContentModuleLifecycleImpactDialog.vue'
import ContentModuleStatusBadge from '@/components/contentModules/ContentModuleStatusBadge.vue'
import ContentModuleVersionDialog from '@/components/contentModules/ContentModuleVersionDialog.vue'
import ControlledStructuredContentEditor from '@/components/authoring/ControlledStructuredContentEditor.vue'
import {
  hasApprovedActiveVersion,
  hasApprovedStoppedVersion,
  latestDraftVersion,
  latestSubmittedVersion,
  resolveContentModuleActorId,
  resolveContentModuleApproverActorRole,
  resolveContentModuleAuthorActorRole,
  resolveContentModuleLifecycleActorRole,
} from '@/auth/contentModuleRoles'
import { useCapabilities } from '@/composables/useCapabilities'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { useContentModulesStore } from '@/stores/contentModules'
import { useSessionStore } from '@/stores/session'
import type {
  ContentModuleLifecycleOperation,
  ContentModuleVersion,
} from '@/types/contentModule'
import { DEFAULT_STRUCTURED_CONTENT_JSON, serializeStructuredContent } from '@/utils/structuredContentNodes'
import { normalizeStructuredContentJson } from '@/utils/structuredContentCompat'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CONTENT_MODULE_WORKSPACE_TAB_LABEL_KEYS,
  buildContentModuleWorkspaceQuery,
  resolveContentModuleWorkspaceTabFromQuery,
  type ContentModuleWorkspaceTab,
} from '@/views/contentModules/contentModuleWorkspaceTabs'

const { t, te } = useI18n()
const route = useRoute()
const router = useRouter()
const contentModulesStore = useContentModulesStore()
const sessionStore = useSessionStore()
const { formatDateTime } = useLocaleFormatters()
const { authorContentModules, decideContentModuleReviews, manageContentModuleLifecycle } =
  useCapabilities()

const loadFailed = ref(false)
const versionDialogOpen = ref(false)
const versionDialogMode = ref<'create' | 'edit'>('create')
const selectedVersion = ref<ContentModuleVersion | null>(null)
const impactDialogOpen = ref(false)
const pendingLifecycleOperation = ref<ContentModuleLifecycleOperation | null>(null)
const activeWorkspaceTab = ref<ContentModuleWorkspaceTab>(
  resolveContentModuleWorkspaceTabFromQuery(route.query),
)

const workspaceTabs = computed(() =>
  (['versions', 'content', 'lifecycle'] as const).map((name) => ({
    name,
    labelKey: CONTENT_MODULE_WORKSPACE_TAB_LABEL_KEYS[name],
  })),
)

watch(
  () => route.query.workspaceTab,
  () => {
    activeWorkspaceTab.value = resolveContentModuleWorkspaceTabFromQuery(route.query)
  },
)

watch(activeWorkspaceTab, (tab) => {
  if (resolveContentModuleWorkspaceTabFromQuery(route.query) === tab) {
    return
  }
  void router.replace({
    query: buildContentModuleWorkspaceQuery(route.query, tab),
  })
})

const moduleId = computed(() => String(route.params.moduleId ?? ''))
const detail = computed(() => contentModulesStore.selectedModule)
const versions = computed(() => detail.value?.versions ?? [])

const authorActorRole = computed(() =>
  resolveContentModuleAuthorActorRole(sessionStore.session?.roles ?? []),
)
const approverActorRole = computed(() =>
  resolveContentModuleApproverActorRole(sessionStore.session?.roles ?? []),
)
const lifecycleActorRole = computed(() =>
  resolveContentModuleLifecycleActorRole(sessionStore.session?.roles ?? []),
)

const canSubmitReview = computed(
  () => authorContentModules.value && Boolean(latestDraftVersion(versions.value)),
)
const canApproveReview = computed(
  () => decideContentModuleReviews.value && Boolean(latestSubmittedVersion(versions.value)),
)
const canCreateVersion = computed(() => authorContentModules.value)
const canEditDraft = computed(
  () => authorContentModules.value && Boolean(latestDraftVersion(versions.value)),
)
const canStop = computed(
  () => manageContentModuleLifecycle.value && hasApprovedActiveVersion(versions.value),
)
const canRecover = computed(
  () => manageContentModuleLifecycle.value && hasApprovedStoppedVersion(versions.value),
)
const canDeprecate = canRecover

const previewVersion = computed(() => {
  const draft = latestDraftVersion(versions.value)
  if (draft) {
    return draft as ContentModuleVersion
  }
  return (
    versions.value.find(
      (version) =>
        version.reviewState === 'APPROVED' && (version.lifecycleState ?? 'ACTIVE') === 'ACTIVE',
    ) ?? null
  )
})

const previewContentJson = computed(() => {
  const version = previewVersion.value
  if (!version?.contentStructureJson) {
    return DEFAULT_STRUCTURED_CONTENT_JSON
  }
  return serializeStructuredContent(normalizeStructuredContentJson(version.contentStructureJson))
})

const previewVersionLabel = computed(() => {
  if (!previewVersion.value) {
    return ''
  }
  return t('contentModules.detail.contentPreviewVersion', {
    version: previewVersion.value.semanticVersion,
    state: previewVersion.value.reviewState,
  })
})

const errorMessage = computed(() => {
  const key = contentModulesStore.lastErrorMessageKey
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('contentModules.error.loadDetail')
})

const lifecycleOperationLabelKey = computed(() => {
  switch (pendingLifecycleOperation.value) {
    case 'STOP_USE':
      return 'contentModules.lifecycle.stopDescription'
    case 'RECOVER':
      return 'contentModules.lifecycle.recoverDescription'
    case 'DEPRECATE':
      return 'contentModules.lifecycle.deprecateDescription'
    default:
      return 'contentModules.lifecycle.impactTitle'
  }
})

onMounted(async () => {
  await reloadPage()
})

onUnmounted(() => {
  contentModulesStore.clearSelected()
})

async function reloadPage() {
  loadFailed.value = false
  try {
    await contentModulesStore.fetchModule(moduleId.value)
  } catch {
    loadFailed.value = true
  }
}

function goBackToList() {
  router.push('/content-modules')
}

function openCreateVersionDialog() {
  versionDialogMode.value = 'create'
  selectedVersion.value = null
  versionDialogOpen.value = true
}

function openEditDraftDialog() {
  const draft = latestDraftVersion(versions.value)
  if (!draft) {
    return
  }
  versionDialogMode.value = 'edit'
  selectedVersion.value = draft as ContentModuleVersion
  versionDialogOpen.value = true
}

async function handleSubmitReview() {
  const actorRole = authorActorRole.value
  if (!actorRole) {
    return
  }
  try {
    const result = await ElMessageBox.prompt(
      t('contentModules.review.changeDescriptionPrompt'),
      t('contentModules.review.submitTitle'),
      {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        inputValidator: (value) =>
          value.trim().length > 0 ? true : t('contentModules.review.changeDescriptionRequired'),
      },
    )
    await contentModulesStore.transitionReview(moduleId.value, {
      operation: 'SUBMIT_FOR_REVIEW',
      actorRole,
      actorId: resolveContentModuleActorId(sessionStore.session),
      changeDescription: result.value.trim(),
    })
    ElMessage.success(t('contentModules.review.submitSuccess'))
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    ElMessage.error(errorMessage.value || t('contentModules.error.reviewTransition'))
  }
}

async function handleApproveReview() {
  const actorRole = approverActorRole.value
  if (!actorRole) {
    return
  }
  try {
    await ElMessageBox.confirm(
      t('contentModules.review.approveConfirm'),
      t('contentModules.review.approveTitle'),
      {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        type: 'success',
      },
    )
    await contentModulesStore.transitionReview(moduleId.value, {
      operation: 'APPROVE_REVIEW',
      actorRole,
      actorId: resolveContentModuleActorId(sessionStore.session),
    })
    ElMessage.success(t('contentModules.review.approveSuccess'))
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    ElMessage.error(errorMessage.value || t('contentModules.error.reviewTransition'))
  }
}

async function handleRejectReview() {
  const actorRole = approverActorRole.value
  if (!actorRole) {
    return
  }
  try {
    const result = await ElMessageBox.prompt(
      t('contentModules.review.rejectionReasonPrompt'),
      t('contentModules.review.rejectTitle'),
      {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        inputValidator: (value) =>
          value.trim().length > 0 ? true : t('contentModules.review.rejectionReasonRequired'),
      },
    )
    await contentModulesStore.transitionReview(moduleId.value, {
      operation: 'REJECT_REVIEW',
      actorRole,
      actorId: resolveContentModuleActorId(sessionStore.session),
      rejectionReason: result.value.trim(),
    })
    ElMessage.success(t('contentModules.review.rejectSuccess'))
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    ElMessage.error(errorMessage.value || t('contentModules.error.reviewTransition'))
  }
}

async function openLifecycleImpact(operation: ContentModuleLifecycleOperation) {
  pendingLifecycleOperation.value = operation
  impactDialogOpen.value = true
  try {
    await contentModulesStore.fetchLifecycleImpactPreview(moduleId.value)
  } catch {
    ElMessage.error(errorMessage.value || t('contentModules.error.loadImpactPreview'))
    impactDialogOpen.value = false
  }
}

async function confirmLifecycleOperation() {
  const operation = pendingLifecycleOperation.value
  const actorRole = lifecycleActorRole.value
  const impact = contentModulesStore.lifecycleImpactPreview
  if (!operation || !actorRole || !impact) {
    return
  }
  try {
    await contentModulesStore.applyLifecycleOperation(moduleId.value, {
      operationType: operation,
      actorRole,
      actorId: resolveContentModuleActorId(sessionStore.session),
      impactSummaryViewed: true,
      secondConfirmation: true,
      impactSummary:
        operation === 'RECOVER'
          ? undefined
          : impact,
    })
    impactDialogOpen.value = false
    pendingLifecycleOperation.value = null
    ElMessage.success(t(`contentModules.lifecycle.success.${operation}`))
  } catch {
    ElMessage.error(errorMessage.value || t('contentModules.error.lifecycle'))
  }
}

async function handleVersionSaved() {
  ElMessage.success(t('contentModules.version.saveSuccess'))
  await reloadPage()
}
</script>

<template>
  <div class="content-module-detail-page">
    <header class="page-header">
      <div>
        <el-button link type="primary" @click="goBackToList">
          {{ t('contentModules.detail.backToList') }}
        </el-button>
        <h1>{{ detail?.name ?? t('contentModules.detail.loadingTitle') }}</h1>
        <p v-if="detail" class="meta">
          {{ detail.moduleCode }}
          · {{ t('contentModules.detail.groupLabel', { groupCode: detail.groupCode }) }}
        </p>
        <p v-if="detail?.description" class="description">{{ detail.description }}</p>
      </div>
    </header>

    <LoadErrorPanel
      v-if="loadFailed"
      :message-key="contentModulesStore.lastErrorMessageKey ?? 'contentModules.error.loadDetail'"
      @retry="reloadPage"
    />

    <el-skeleton v-else-if="contentModulesStore.loadingDetail" :rows="8" animated />

    <template v-else-if="detail">
      <WorkspaceTabShell v-model="activeWorkspaceTab" :tabs="workspaceTabs">
        <template #actions>
          <template v-if="activeWorkspaceTab === 'versions'">
            <el-button v-if="canEditDraft" @click="openEditDraftDialog">
              {{ t('contentModules.version.editDraft') }}
            </el-button>
            <el-button v-if="canCreateVersion" @click="openCreateVersionDialog">
              {{ t('contentModules.version.create') }}
            </el-button>
          </template>
          <template v-else-if="activeWorkspaceTab === 'content'">
            <el-button v-if="canEditDraft" @click="openEditDraftDialog">
              {{ t('contentModules.version.editDraft') }}
            </el-button>
          </template>
          <template v-else-if="activeWorkspaceTab === 'lifecycle'">
            <el-button v-if="canSubmitReview" type="primary" @click="handleSubmitReview">
              {{ t('contentModules.review.submit') }}
            </el-button>
            <template v-if="canApproveReview">
              <el-button type="success" @click="handleApproveReview">
                {{ t('contentModules.review.approve') }}
              </el-button>
              <el-button type="danger" @click="handleRejectReview">
                {{ t('contentModules.review.reject') }}
              </el-button>
            </template>
            <el-button v-if="canStop" type="warning" @click="openLifecycleImpact('STOP_USE')">
              {{ t('contentModules.lifecycle.stop') }}
            </el-button>
            <el-button v-if="canRecover" @click="openLifecycleImpact('RECOVER')">
              {{ t('contentModules.lifecycle.recover') }}
            </el-button>
            <el-button v-if="canDeprecate" type="danger" plain @click="openLifecycleImpact('DEPRECATE')">
              {{ t('contentModules.lifecycle.deprecate') }}
            </el-button>
          </template>
        </template>

        <template #versions>
          <AppDataTable v-if="versions.length > 0" :data="versions">
            <el-table-column prop="semanticVersion" :label="t('contentModules.detail.columns.version')" width="140" />
            <el-table-column :label="t('contentModules.detail.columns.status')" width="180">
              <template #default="{ row }">
                <ContentModuleStatusBadge
                  :review-state="row.reviewState"
                  :lifecycle-state="row.lifecycleState"
                />
              </template>
            </el-table-column>
            <el-table-column prop="changeDescription" :label="t('contentModules.detail.columns.changeDescription')" min-width="220" />
            <el-table-column :label="t('contentModules.detail.columns.updatedAt')" width="200">
              <template #default="{ row }">
                {{ formatDateTime(row.updatedAt) }}
              </template>
            </el-table-column>
          </AppDataTable>
          <EmptyStatePanel
            v-else
            title-key="contentModules.detail.noVersions"
            description-key="contentModules.detail.noVersionsDescription"
          />
        </template>

        <template #content>
          <template v-if="previewVersion">
            <p class="preview-meta">{{ previewVersionLabel }}</p>
            <ControlledStructuredContentEditor
              :model-value="previewContentJson"
              readonly
            />
          </template>
          <EmptyStatePanel
            v-else
            title-key="contentModules.detail.noVersions"
            description-key="contentModules.detail.noVersionsDescription"
          />
        </template>

        <template #lifecycle>
          <p class="lifecycle-hint">{{ t('contentModules.workspace.lifecycleHint') }}</p>
        </template>
      </WorkspaceTabShell>
    </template>

    <ContentModuleVersionDialog
      v-model="versionDialogOpen"
      :module-id="moduleId"
      :mode="versionDialogMode"
      :version="selectedVersion"
      @saved="handleVersionSaved"
    />

    <ContentModuleLifecycleImpactDialog
      v-model="impactDialogOpen"
      :loading="contentModulesStore.loadingImpactPreview"
      :impact="contentModulesStore.lifecycleImpactPreview"
      :operation-label-key="lifecycleOperationLabelKey"
      @confirm="confirmLifecycleOperation"
    />
  </div>
</template>

<style scoped lang="scss">
.content-module-detail-page {
  padding: 1.5rem 2rem 2rem;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.5rem;

  h1 {
    margin: 0.35rem 0 0.25rem;
    font-size: 1.5rem;
    font-weight: 650;
  }
}

.meta,
.description {
  margin: 0.15rem 0 0;
  color: var(--text-muted);
}

.lifecycle-hint,
.preview-meta {
  margin: 0 0 0.75rem;
  color: var(--text-muted);
  font-size: 0.875rem;
}
</style>
