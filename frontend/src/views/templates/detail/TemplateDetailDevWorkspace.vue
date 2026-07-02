<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import WorkspaceTabShell from '@/components/common/WorkspaceTabShell.vue'
import LifecycleCommentDialog from '@/components/common/LifecycleCommentDialog.vue'
import TemplateDetailDesignTab from '@/views/templates/detail/TemplateDetailDesignTab.vue'
import TemplateDetailTestingTab from '@/views/templates/detail/TemplateDetailTestingTab.vue'
import TemplateDetailApprovalTab from '@/views/templates/detail/TemplateDetailApprovalTab.vue'
import {
  TEMPLATE_DEV_WORKSPACE_TABS,
  buildDevWorkspaceQuery,
  resolveTemplateDevWorkspaceTabFromQuery,
  templateDevWorkspaceTabLabelKey,
  type TemplateDevWorkspaceTab,
} from '@/views/templates/templateDevWorkspaceTabs'
import type { SemverBumpLevel } from '@/utils/semver'
import type { PublishGateDisplayItem } from '@/utils/templateLifecycleDecisionForm'
import type {
  AnchorBinding,
  BindingValidationResult,
  CompositionRule,
  PreviewRecord,
  TemplateLifecycleStatus,
  VariableSchema,
} from '@/types/template'

type PublishBumpOption = {
  level: SemverBumpLevel
  label: string
  version: string
}

type GovernanceAction = 'stop' | 'restore' | 'deprecate'

const props = defineProps<{
  templateId: string
  masterId: string
  variables: VariableSchema[]
  bindings: AnchorBinding[]
  rules: CompositionRule[] | null
  groupCode: string | null
  lifecycleStatus: TemplateLifecycleStatus
  canEditContentModuleReferences: boolean
  coverageRefreshToken: number
  lastPreview: PreviewRecord | null
  selectedPreviewId: string | null
  selectedTestDataSetId: string | null
  showDraftActions: boolean
  showTestingDecisionActions: boolean
  showSubmitForApproval: boolean
  showApprovalDecisionActions: boolean
  showPublishActions: boolean
  showTestGenerate: boolean
  showStopAction: boolean
  showRestoreAction: boolean
  showDeprecateAction: boolean
  showGovernanceSection: boolean
  publishGateItems: PublishGateDisplayItem[]
  loadingPublishGate: boolean
  publishBumpLevel: SemverBumpLevel
  publishVersionConflict: boolean
  publishGateReady: boolean
  publishBumpOptions: PublishBumpOption[]
  submitGateItems: PublishGateDisplayItem[]
  loadingSubmitGate: boolean
  submitGateReady: boolean
  submitGateLoadError: string | null
  bindingGateResult: BindingValidationResult | null
  publishGateLoadError: string | null
  submitting: boolean
  generatingPreview: boolean
  generatingPreviewId: string | null
  batchTesting: boolean
  openSubmitForTestDialog?: boolean
}>()

const emit = defineEmits<{
  updated: []
  'update:selectedTestDataSetId': [id: string | null]
  'update:selectedPreviewId': [previewId: string | null]
  'update:publishBumpLevel': [value: SemverBumpLevel]
  'update:openSubmitForTestDialog': [value: boolean]
  'test-generate': [testDataSetId: string | undefined]
  'test-generate-batch': []
  'submit-for-test': [comment: string]
  'test-decision': [decision: 'PASSED' | 'FAILED']
  submitForApproval: []
  approvalDecision: [decision: 'APPROVED' | 'REJECTED']
  publish: []
  governanceAction: [action: GovernanceAction]
  retryPublishGate: []
  retrySubmitGate: []
}>()

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const activeWorkspaceTab = ref<TemplateDevWorkspaceTab>(
  resolveTemplateDevWorkspaceTabFromQuery(route.query),
)
const testDataSetCount = ref(0)
const submitForTestDialogOpen = ref(false)

const workspaceTabs = computed(() =>
  TEMPLATE_DEV_WORKSPACE_TABS.map((name) => ({
    name,
    labelKey: templateDevWorkspaceTabLabelKey(name),
  })),
)

const canRunSelected = computed(
  () => props.showTestGenerate && Boolean(props.selectedTestDataSetId) && !props.generatingPreview,
)

watch(
  () => route.query,
  () => {
    const resolved = resolveTemplateDevWorkspaceTabFromQuery(route.query)
    if (activeWorkspaceTab.value !== resolved) {
      activeWorkspaceTab.value = resolved
    }
  },
  { deep: true },
)

watch(activeWorkspaceTab, (tab) => {
  if (resolveTemplateDevWorkspaceTabFromQuery(route.query) === tab) {
    return
  }
  void router.replace({ query: buildDevWorkspaceQuery(route.query, tab) })
})

watch(
  () => props.openSubmitForTestDialog,
  (requested) => {
    if (requested) {
      activeWorkspaceTab.value = 'testing'
      submitForTestDialogOpen.value = true
      emit('update:openSubmitForTestDialog', false)
    }
  },
)

function handleSubmitForTestConfirm(comment: string) {
  submitForTestDialogOpen.value = false
  emit('submit-for-test', comment)
}

function requestSubmitForTestDialog() {
  submitForTestDialogOpen.value = true
}
</script>

<template>
  <section id="dev-workspace" class="dev-workspace">
    <WorkspaceTabShell v-model="activeWorkspaceTab" :tabs="workspaceTabs">
      <template #actions>
        <template v-if="activeWorkspaceTab === 'testing'">
          <el-button-group v-if="showTestGenerate">
            <el-button
              type="primary"
              :loading="generatingPreview"
              :disabled="!canRunSelected"
              @click="emit('test-generate', selectedTestDataSetId ?? undefined)"
            >
              {{ t('templates.testPreview.workflow.runSelected') }}
            </el-button>
            <el-button
              :loading="batchTesting"
              :disabled="testDataSetCount === 0 || generatingPreview"
              @click="emit('test-generate-batch')"
            >
              {{ t('templates.testPreview.workflow.runAll') }}
            </el-button>
          </el-button-group>

          <el-button
            v-if="showDraftActions"
            type="success"
            :loading="submitting"
            :disabled="testDataSetCount === 0"
            @click="requestSubmitForTestDialog"
          >
            {{ t('templates.lifecycle.submitTest') }}
          </el-button>

          <template v-if="showTestingDecisionActions">
            <el-button type="success" :loading="submitting" @click="emit('test-decision', 'PASSED')">
              {{ t('templates.lifecycle.passTest') }}
            </el-button>
            <el-button type="danger" :loading="submitting" @click="emit('test-decision', 'FAILED')">
              {{ t('templates.lifecycle.failTest') }}
            </el-button>
          </template>
        </template>

        <template v-else-if="activeWorkspaceTab === 'approval'">
          <el-button
            v-if="showSubmitForApproval"
            type="primary"
            :loading="submitting"
            :disabled="!submitGateReady || loadingSubmitGate || Boolean(submitGateLoadError)"
            @click="emit('submitForApproval')"
          >
            {{ t('templates.lifecycle.submitApproval') }}
          </el-button>
          <template v-if="showApprovalDecisionActions">
            <el-button type="success" :loading="submitting" @click="emit('approvalDecision', 'APPROVED')">
              {{ t('templates.lifecycle.approve') }}
            </el-button>
            <el-button type="danger" :loading="submitting" @click="emit('approvalDecision', 'REJECTED')">
              {{ t('templates.lifecycle.reject') }}
            </el-button>
          </template>
          <el-button
            v-if="showPublishActions"
            type="primary"
            :loading="submitting"
            :disabled="!publishGateReady || loadingPublishGate"
            @click="emit('publish')"
          >
            {{ t('templates.lifecycle.publish') }}
          </el-button>
          <el-button
            v-if="showStopAction"
            type="warning"
            :loading="submitting"
            @click="emit('governanceAction', 'stop')"
          >
            {{ t('templates.governance.stop') }}
          </el-button>
          <el-button
            v-if="showRestoreAction"
            type="primary"
            :loading="submitting"
            @click="emit('governanceAction', 'restore')"
          >
            {{ t('templates.governance.restore') }}
          </el-button>
          <el-button
            v-if="showDeprecateAction"
            type="danger"
            :loading="submitting"
            @click="emit('governanceAction', 'deprecate')"
          >
            {{ t('templates.governance.deprecate') }}
          </el-button>
        </template>
      </template>

      <template #design>
        <TemplateDetailDesignTab
          :template-id="templateId"
          :master-id="masterId"
          :variables="variables"
          :bindings="bindings"
          :rules="rules"
          :group-code="groupCode"
          :can-edit-content-module-references="canEditContentModuleReferences"
          :coverage-refresh-token="coverageRefreshToken"
          @updated="emit('updated')"
        />
      </template>

      <template #testing>
        <TemplateDetailTestingTab
          :template-id="templateId"
          :bindings="bindings"
          :coverage-refresh-token="coverageRefreshToken"
          :last-preview="lastPreview"
          :selected-preview-id="selectedPreviewId"
          :lifecycle-status="lifecycleStatus"
          :generating-preview-id="generatingPreviewId"
          :selected-test-data-set-id="selectedTestDataSetId"
          @update:selected-preview-id="emit('update:selectedPreviewId', $event)"
          @update:selected-test-data-set-id="emit('update:selectedTestDataSetId', $event)"
          @loaded-data-set-count="testDataSetCount = $event"
        />
      </template>

      <template #approval>
        <TemplateDetailApprovalTab
          :template-id="templateId"
          :show-submit-for-approval="showSubmitForApproval"
          :show-publish-actions="showPublishActions"
          :show-governance-section="showGovernanceSection"
          :publish-gate-items="publishGateItems"
          :loading-publish-gate="loadingPublishGate"
          :publish-bump-level="publishBumpLevel"
          :publish-version-conflict="publishVersionConflict"
          :publish-bump-options="publishBumpOptions"
          :submit-gate-items="submitGateItems"
          :loading-submit-gate="loadingSubmitGate"
          :submit-gate-load-error="submitGateLoadError"
          :binding-gate-result="bindingGateResult"
          :publish-gate-load-error="publishGateLoadError"
          @update:publish-bump-level="emit('update:publishBumpLevel', $event)"
          @retry-publish-gate="emit('retryPublishGate')"
          @retry-submit-gate="emit('retrySubmitGate')"
        />
      </template>
    </WorkspaceTabShell>

    <LifecycleCommentDialog
      v-model="submitForTestDialogOpen"
      :title="t('templates.testPreview.workflow.submitDialogTitle')"
      :message="t('templates.testPreview.workflow.submitDialogMessage')"
      :confirm-label="t('templates.lifecycle.submitTest')"
      :loading="submitting"
      @confirm="handleSubmitForTestConfirm"
    />
  </section>
</template>

<style scoped lang="scss">
.dev-workspace {
  margin-top: 0.25rem;
}
</style>
