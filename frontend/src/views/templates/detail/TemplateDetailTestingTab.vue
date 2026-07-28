<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ContextHelpTrigger from '@/components/common/ContextHelpTrigger.vue'
import BatchTestHistoryPanel from '@/components/templates/BatchTestHistoryPanel.vue'
import TemplateTestDataSetPanel from '@/components/templates/TemplateTestDataSetPanel.vue'
import TemplateTestPreviewWorkflowPanel from '@/components/templates/TemplateTestPreviewWorkflowPanel.vue'
import TemplateCoveragePanel from '@/components/templates/TemplateCoveragePanel.vue'
import TemplateChangeDiffPanel from '@/components/templates/TemplateChangeDiffPanel.vue'
import TemplatePreviewPanel from '@/components/templates/TemplatePreviewPanel.vue'
import TemplatePreviewRunHistoryPanel from '@/components/templates/TemplatePreviewRunHistoryPanel.vue'
import {
  buildDevWorkspaceQuery,
  resolveTestingSubTabFromQuery,
} from '@/views/templates/templateDevWorkspaceTabs'
import {
  templateTestingSubTabLabelKey,
  type TemplateTestingSubTab,
} from '@/views/templates/templateTestingSubTabs'
import type { AnchorBinding, PreviewRecord, TemplateLifecycleStatus, VariableSchema } from '@/types/template'

const props = defineProps<{
  templateId: string
  variables: VariableSchema[]
  bindings: AnchorBinding[]
  coverageRefreshToken: number
  lastPreview: PreviewRecord | null
  selectedPreviewId: string | null
  selectedTestDataSetId: string | null
  lifecycleStatus: TemplateLifecycleStatus
  generatingPreviewId: string | null
}>()

const emit = defineEmits<{
  'update:selectedPreviewId': [previewId: string | null]
  'update:selectedTestDataSetId': [id: string | null]
  'loaded-data-set-count': [count: number]
}>()

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const activeSubTab = ref<TemplateTestingSubTab>(resolveTestingSubTabFromQuery(route.query))

watch(
  () => route.query.testingTab,
  () => {
    activeSubTab.value = resolveTestingSubTabFromQuery(route.query)
  },
)

watch(activeSubTab, (tab) => {
  if (resolveTestingSubTabFromQuery(route.query) === tab) {
    return
  }
  void router.replace({
    query: buildDevWorkspaceQuery(route.query, 'testing', tab),
  })
})

watch(
  () => props.selectedPreviewId,
  (previewId) => {
    if (previewId) {
      activeSubTab.value = 'previewRuns'
    }
  },
)

function handleOpenDataSet(payload: {
  dataSetExternalId: string
  testDataSetId: string | null
  matched: boolean
}) {
  activeSubTab.value = 'dataSets'
  void router.replace({
    query: buildDevWorkspaceQuery(route.query, 'testing', 'dataSets'),
  })
  if (payload.matched && payload.testDataSetId) {
    emit('update:selectedTestDataSetId', payload.testDataSetId)
    return
  }
  emit('update:selectedTestDataSetId', null)
  ElMessage.warning(
    t('templates.batchTestHistory.sampleResults.dataSetNotFound', {
      id: payload.dataSetExternalId,
    }),
  )
}

function handleOpenPreview(payload: { previewId: string }) {
  activeSubTab.value = 'previewRuns'
  void router.replace({
    query: buildDevWorkspaceQuery(route.query, 'testing', 'previewRuns'),
  })
  emit('update:selectedPreviewId', payload.previewId)
}
</script>

<template>
  <el-card shadow="never" class="section-card">
    <div class="section-card__heading">
      <h2>{{ t('templates.devWorkspace.testing.title') }}</h2>
      <ContextHelpTrigger
        :title="t('templates.testPreview.workflow.helpTitle')"
        :content="t('templates.testPreview.workflow.helpContent')"
      />
    </div>

    <el-tabs v-model="activeSubTab" class="testing-sub-tabs">
      <el-tab-pane :label="t(templateTestingSubTabLabelKey('dataSets'))" name="dataSets">
        <TemplateTestPreviewWorkflowPanel :lifecycle-status="lifecycleStatus" />

        <TemplateTestDataSetPanel
          :template-id="templateId"
          :variables="variables"
          :generating-preview-id="generatingPreviewId"
          :refresh-token="coverageRefreshToken"
          :selected-test-data-set-id="selectedTestDataSetId"
          @selected="emit('update:selectedTestDataSetId', $event)"
          @test-generate="emit('update:selectedTestDataSetId', $event)"
          @loaded="emit('loaded-data-set-count', $event)"
        />
      </el-tab-pane>

      <el-tab-pane :label="t(templateTestingSubTabLabelKey('batchRuns'))" name="batchRuns">
        <BatchTestHistoryPanel
          :template-id="templateId"
          :refresh-token="coverageRefreshToken"
          @open-data-set="handleOpenDataSet"
          @open-preview="handleOpenPreview"
        />
      </el-tab-pane>

      <el-tab-pane :label="t(templateTestingSubTabLabelKey('previewRuns'))" name="previewRuns">
        <TemplatePreviewRunHistoryPanel
          :template-id="templateId"
          :refresh-token="coverageRefreshToken"
          :selected-preview-id="selectedPreviewId"
          @selected="emit('update:selectedPreviewId', $event)"
        />

        <TemplatePreviewPanel
          v-if="lastPreview"
          class="preview-detail-panel"
          :template-id="templateId"
          :bindings="bindings"
          :preview="lastPreview"
        />
      </el-tab-pane>

      <el-tab-pane :label="t(templateTestingSubTabLabelKey('coverage'))" name="coverage">
        <TemplateCoveragePanel compact :template-id="templateId" :refresh-token="coverageRefreshToken" />
      </el-tab-pane>

      <el-tab-pane :label="t(templateTestingSubTabLabelKey('changeDiff'))" name="changeDiff">
        <TemplateChangeDiffPanel compact :template-id="templateId" :refresh-token="coverageRefreshToken" />
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<style scoped lang="scss">
.section-card {
  margin-bottom: 0;

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

.testing-sub-tabs {
  margin-top: 0.25rem;
}

.preview-detail-panel {
  margin-top: 1.25rem;
}

:deep(.batch-test-history) {
  margin-bottom: 1.25rem;
}
</style>
