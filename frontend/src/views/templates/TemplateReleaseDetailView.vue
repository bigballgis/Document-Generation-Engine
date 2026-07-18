<script setup lang="ts">
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import WorkspaceTabShell from '@/components/common/WorkspaceTabShell.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import BatchTestHistoryPanel from '@/components/templates/BatchTestHistoryPanel.vue'
import TemplateLifecycleAuditTimeline from '@/components/templates/TemplateLifecycleAuditTimeline.vue'
import PublishGateReadOnlyPanel from '@/components/templates/PublishGateReadOnlyPanel.vue'
import TemplateStatusBadge from '@/components/templates/TemplateStatusBadge.vue'
import ReleaseSectionTable from '@/components/templates/ReleaseSectionTable.vue'
import TemplateDetailOverviewTab from '@/views/templates/detail/TemplateDetailOverviewTab.vue'
import { useTemplateReleaseDetailView } from '@/views/templates/useTemplateReleaseDetailView'

const {
  t,
  formatDateTime,
  authorTemplates,
  loading,
  loadFailed,
  cloning,
  releaseDetail,
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
} = useTemplateReleaseDetailView()
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
        <PublishGateReadOnlyPanel
          :template-id="templateId"
          :release-version="releaseVersion"
        />
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
