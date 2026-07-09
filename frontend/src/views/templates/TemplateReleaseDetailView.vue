<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import WorkspaceTabShell from '@/components/common/WorkspaceTabShell.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import BatchTestHistoryPanel from '@/components/template/BatchTestHistoryPanel.vue'
import TemplateLifecycleAuditTimeline from '@/components/templates/TemplateLifecycleAuditTimeline.vue'
import TemplateStatusBadge from '@/components/templates/TemplateStatusBadge.vue'
import ReleaseSectionTable from '@/components/templates/ReleaseSectionTable.vue'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { useCapabilities } from '@/composables/useCapabilities'
import { useTemplatePanelDataStore } from '@/stores/templatePanelData'
import {
  templateDevVersionPath,
  templatePackageHubPath,
} from '@/routing/routeKeys'
import type { TemplateDetail } from '@/types/template'
import { versionLineDisplayLabel } from '@/utils/templateVersionLine'
import TemplateDetailOverviewTab from '@/views/templates/detail/TemplateDetailOverviewTab.vue'
import {
  TEMPLATE_RELEASE_WORKSPACE_TAB_LABEL_KEYS,
  buildTemplateReleaseWorkspaceQuery,
  resolveTemplateReleaseWorkspaceTabFromQuery,
  type TemplateReleaseWorkspaceTab,
} from '@/views/templates/templateReleaseWorkspaceTabs'

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
</script>

<template>
  <AppPageLayout>
    <PageHeader
      show-back
      :back-label="t('templates.releaseDetail.backToHub')"
      :title="releaseDetail ? lineLabel : t('templates.releaseDetail.loadingTitle')"
      :description="releaseDetail ? t('templates.releaseDetail.readOnlyHint') : undefined"
      @back="backToHub"
    >
      <template v-if="releaseDetail" #actions>
        <TemplateStatusBadge
          :status="releaseDetail.lifecycleStatus"
          :approval-sub-state="releaseDetail.approvalSubState"
        />
        <el-button
          v-if="authorTemplates"
          type="primary"
          :loading="cloning"
          @click="handleClone"
        >
          {{ t('templates.versionLines.clone') }}
        </el-button>
      </template>
    </PageHeader>

    <LoadErrorPanel
      v-if="loadFailed"
      message-key="templates.releaseDetail.loadError"
      @retry="loadReleaseDetail"
    />

    <el-skeleton v-else-if="loading" :rows="8" animated />

    <EmptyStatePanel
      v-else-if="!releaseDetail"
      title-key="templates.releaseDetail.notFoundTitle"
      description-key="templates.releaseDetail.notFoundDescription"
    />

    <WorkspaceTabShell
      v-else
      v-model="activeWorkspaceTab"
      :tabs="workspaceTabs"
    >
      <template #basics>
        <TemplateDetailOverviewTab
          :template="releaseDetail"
          :format-date-time="formatDateTime"
        />
      </template>

      <template #testing>
        <p class="read-only-hint">{{ t('templates.releaseDetail.testing.readOnlySummary') }}</p>
        <BatchTestHistoryPanel :template-id="templateId" />
      </template>

      <template #approval>
        <el-card shadow="never" class="summary-card">
          <p class="read-only-hint">{{ t('templates.releaseDetail.approval.readOnlySummary') }}</p>
          <dl class="summary-grid">
            <div>
              <dt>{{ t('templates.releaseDetail.approval.lifecycleStatus') }}</dt>
              <dd>
                <TemplateStatusBadge
                  :status="releaseDetail.lifecycleStatus"
                  :approval-sub-state="releaseDetail.approvalSubState"
                />
              </dd>
            </div>
            <div>
              <dt>{{ t('templates.releaseDetail.approval.approvalSubState') }}</dt>
              <dd>{{ approvalSubStateLabel }}</dd>
            </div>
          </dl>
        </el-card>
        <TemplateLifecycleAuditTimeline :template-id="templateId" />
      </template>

      <template #variables>
        <ReleaseSectionTable
          :title="t('templates.releaseDetail.variablesTitle')"
          :columns="variableColumns"
          :data="releaseDetail.variables"
          :empty-text="t('templates.releaseDetail.noVariables')"
        />
      </template>

      <template #bindings>
        <ReleaseSectionTable
          :title="t('templates.releaseDetail.bindingsTitle')"
          :columns="bindingColumns"
          :data="releaseDetail.bindings"
          :empty-text="t('templates.releaseDetail.noBindings')"
        />
      </template>

      <template #rules>
        <ReleaseSectionTable
          :title="t('templates.releaseDetail.rulesTitle')"
          :columns="ruleColumns"
          :data="releaseDetail.rules"
          :empty-text="t('templates.releaseDetail.noRules')"
        />
      </template>
    </WorkspaceTabShell>
  </AppPageLayout>
</template>

<style scoped lang="scss">
.summary-card {
  margin-bottom: 1rem;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1rem;
  margin: 0;

  dt {
    margin: 0;
    font-size: 0.85rem;
    color: var(--text-muted);
  }

  dd {
    margin: 0.25rem 0 0;
    font-weight: 500;
  }
}

.read-only-hint {
  margin: 0 0 1rem;
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}
</style>
