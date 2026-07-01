<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import * as templatesApi from '@/api/templates'
import ContextHelpTrigger from '@/components/common/ContextHelpTrigger.vue'
import TemplateVariableTreePanel from '@/components/templates/TemplateVariableTreePanel.vue'
import TemplateAuthoringBindingsPanel from '@/components/templates/TemplateAuthoringBindingsPanel.vue'
import TemplateClauseAuthoringPanel from '@/components/templates/TemplateClauseAuthoringPanel.vue'
import TemplateTestDataSetPanel from '@/components/templates/TemplateTestDataSetPanel.vue'
import TemplateTestPreviewWorkflowPanel from '@/components/templates/TemplateTestPreviewWorkflowPanel.vue'
import TemplateCoveragePanel from '@/components/templates/TemplateCoveragePanel.vue'
import TemplateChangeDiffPanel from '@/components/templates/TemplateChangeDiffPanel.vue'
import TemplatePreviewPanel from '@/components/templates/TemplatePreviewPanel.vue'
import TemplatePreviewRunHistoryPanel from '@/components/templates/TemplatePreviewRunHistoryPanel.vue'
import {
  resolveTemplateAuthoringSubTab,
  templateAuthoringSubTabLabelKey,
  type TemplateAuthoringSubTab,
} from '@/views/templates/templateAuthoringSubTabs'
import type {
  AnchorBinding,
  CompositionRule,
  PreviewRecord,
  TemplateContentModuleReference,
  TemplateLifecycleStatus,
  VariableSchema,
} from '@/types/template'

const props = defineProps<{
  templateId: string
  masterId: string
  variables: VariableSchema[]
  bindings: AnchorBinding[]
  rules: CompositionRule[] | null
  groupCode: string | null
  canEditContentModuleReferences: boolean
  coverageRefreshToken: number
  lastPreview: PreviewRecord | null
  selectedPreviewId: string | null
  lifecycleStatus: TemplateLifecycleStatus
  selectedTestDataSetId: string | null
  showDraftActions: boolean
  showTestingDecisionActions: boolean
  showTestGenerate: boolean
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
  'update:openSubmitForTestDialog': [value: boolean]
  'test-generate': [testDataSetId: string | undefined]
  'test-generate-batch': []
  'submit-for-test': [comment: string]
  'test-decision': [decision: 'PASSED' | 'FAILED']
  'batch-complete': []
}>()

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const activeSubTab = ref<TemplateAuthoringSubTab>(resolveTemplateAuthoringSubTab(route.query.authoringTab))
const contentModuleReferences = ref<TemplateContentModuleReference[]>([])
const testDataSetPanelRef = ref<{ reload: () => Promise<void> } | null>(null)
const testDataSetCount = ref(0)
const expandedPreviewPanels = ref<string[]>([])

watch(
  () => props.lastPreview?.previewId,
  (previewId) => {
    if (previewId && !expandedPreviewPanels.value.includes('previewDetail')) {
      expandedPreviewPanels.value = [...expandedPreviewPanels.value, 'previewDetail']
    }
  },
)

watch(
  () => route.query.authoringTab,
  (value) => {
    activeSubTab.value = resolveTemplateAuthoringSubTab(value)
  },
)

watch(activeSubTab, (tab) => {
  if (resolveTemplateAuthoringSubTab(route.query.authoringTab) === tab) {
    return
  }
  router.replace({ query: { ...route.query, authoringTab: tab } })
})

watch(
  () => props.coverageRefreshToken,
  () => {
    void loadContentModuleReferences()
  },
)

function handleReferencesLoaded(references: TemplateContentModuleReference[]) {
  contentModuleReferences.value = references
}

function handleUpdated() {
  emit('updated')
}

function handleSelected(testDataSetId: string | null) {
  emit('update:selectedTestDataSetId', testDataSetId)
}

function handleTestGenerateFromRow(testDataSetId: string) {
  emit('test-generate', testDataSetId)
}

async function loadContentModuleReferences() {
  if (!props.groupCode) {
    contentModuleReferences.value = []
    return
  }
  try {
    contentModuleReferences.value = await templatesApi.listTemplateContentModuleReferences(props.templateId)
  } catch {
    contentModuleReferences.value = []
  }
}

onMounted(() => {
  void loadContentModuleReferences()
})
</script>

<template>
  <el-card shadow="never" class="section-card">
    <div class="section-card__heading">
      <h2>{{ t('templates.authoring.title') }}</h2>
      <ContextHelpTrigger
        :title="t('templates.authoring.helpTitle')"
        :content="t('templates.authoring.helpContent')"
      />
    </div>

    <el-tabs v-model="activeSubTab" class="authoring-sub-tabs">
      <el-tab-pane :label="t(templateAuthoringSubTabLabelKey('variables'))" name="variables">
        <TemplateVariableTreePanel
          :template-id="templateId"
          :variables="variables"
          @updated="handleUpdated"
        />
      </el-tab-pane>

      <el-tab-pane
        v-if="groupCode"
        :label="t(templateAuthoringSubTabLabelKey('contentModules'))"
        name="contentModules"
      >
        <TemplateClauseAuthoringPanel
          :template-id="templateId"
          :group-code="groupCode"
          :editable="canEditContentModuleReferences"
          :refresh-token="coverageRefreshToken"
          @updated="handleUpdated"
          @references-loaded="handleReferencesLoaded"
        />
      </el-tab-pane>

      <el-tab-pane :label="t(templateAuthoringSubTabLabelKey('bindings'))" name="bindings">
        <TemplateAuthoringBindingsPanel
          :template-id="templateId"
          :master-id="masterId"
          :variables="variables"
          :bindings="bindings"
          :rules="rules"
          :content-module-references="contentModuleReferences"
          @updated="handleUpdated"
        />
      </el-tab-pane>

      <el-tab-pane :label="t(templateAuthoringSubTabLabelKey('testPreview'))" name="testPreview">
        <TemplateTestPreviewWorkflowPanel
          :lifecycle-status="lifecycleStatus"
          :selected-test-data-set-id="selectedTestDataSetId"
          :show-draft-actions="showDraftActions"
          :show-testing-decision-actions="showTestingDecisionActions"
          :show-test-generate="showTestGenerate"
          :submitting="submitting"
          :generating-preview="generatingPreview"
          :batch-testing="batchTesting"
          :has-data-sets="testDataSetCount > 0"
          :open-submit-dialog="openSubmitForTestDialog"
          @update:open-submit-dialog="emit('update:openSubmitForTestDialog', $event)"
          @test-generate-selected="emit('test-generate', selectedTestDataSetId ?? undefined)"
          @test-generate-batch="emit('test-generate-batch')"
          @submit-for-test="emit('submit-for-test', $event)"
          @test-decision="emit('test-decision', $event)"
        />

        <TemplateTestDataSetPanel
          ref="testDataSetPanelRef"
          :template-id="templateId"
          :generating-preview-id="generatingPreviewId"
          :refresh-token="coverageRefreshToken"
          @selected="handleSelected"
          @test-generate="handleTestGenerateFromRow"
          @loaded="testDataSetCount = $event"
        />

        <TemplatePreviewRunHistoryPanel
          :template-id="templateId"
          :refresh-token="coverageRefreshToken"
          :selected-preview-id="selectedPreviewId"
          @selected="emit('update:selectedPreviewId', $event)"
        />

        <el-collapse v-model="expandedPreviewPanels" class="test-preview-collapse">
          <el-collapse-item name="coverage">
            <template #title>
              <span class="collapse-title">
                {{ t('templates.coverage.title') }}
                <ContextHelpTrigger
                  :title="t('templates.coverage.helpTitle')"
                  :content="t('templates.coverage.helpContent')"
                  @click.stop
                />
              </span>
            </template>
            <TemplateCoveragePanel
              compact
              :template-id="templateId"
              :refresh-token="coverageRefreshToken"
            />
          </el-collapse-item>
          <el-collapse-item name="changeDiff">
            <template #title>
              <span class="collapse-title">
                {{ t('templates.changeDiff.title') }}
                <ContextHelpTrigger
                  :title="t('templates.changeDiff.helpTitle')"
                  :content="t('templates.changeDiff.helpContent')"
                  @click.stop
                />
              </span>
            </template>
            <TemplateChangeDiffPanel
              compact
              :template-id="templateId"
              :refresh-token="coverageRefreshToken"
            />
          </el-collapse-item>
          <el-collapse-item name="previewDetail">
            <template #title>
              <span class="collapse-title">
                {{ t('templates.preview.detailTitle') }}
                <ContextHelpTrigger
                  :title="t('templates.preview.helpTitle')"
                  :content="t('templates.preview.helpContent')"
                  @click.stop
                />
              </span>
            </template>
            <TemplatePreviewPanel
              compact
              :template-id="templateId"
              :bindings="bindings"
              :preview="lastPreview"
            />
          </el-collapse-item>
        </el-collapse>
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<style scoped lang="scss">
.section-card {
  margin-bottom: 1.5rem;

  &__heading {
    display: flex;
    align-items: center;
    gap: 0.25rem;
    margin-bottom: 1rem;
  }

  h2 {
    margin: 0;
    font-size: 1.125rem;
  }
}

.authoring-sub-tabs {
  margin-top: 0.25rem;
}

.test-preview-collapse {
  margin-top: 1.25rem;
  border: none;

  :deep(.el-collapse-item__header) {
    font-weight: 650;
    font-size: 0.9375rem;
  }

  :deep(.el-collapse-item__wrap) {
    border-bottom: none;
  }
}

.collapse-title {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
}
</style>
