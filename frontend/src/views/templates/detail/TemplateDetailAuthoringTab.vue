<script setup lang="ts">
import { ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import TemplateVariableTreePanel from '@/components/templates/TemplateVariableTreePanel.vue'
import TemplateAuthoringBindingsPanel from '@/components/templates/TemplateAuthoringBindingsPanel.vue'
import TemplateRuleConfigurator from '@/components/templates/TemplateRuleConfigurator.vue'
import TemplateContentModuleReferencesPanel from '@/components/templates/TemplateContentModuleReferencesPanel.vue'
import TemplateTestDataSetPanel from '@/components/templates/TemplateTestDataSetPanel.vue'
import TemplateCoveragePanel from '@/components/templates/TemplateCoveragePanel.vue'
import TemplateChangeDiffPanel from '@/components/templates/TemplateChangeDiffPanel.vue'
import TemplatePreviewPanel from '@/components/templates/TemplatePreviewPanel.vue'
import {
  resolveTemplateAuthoringSubTab,
  templateAuthoringSubTabLabelKey,
  type TemplateAuthoringSubTab,
} from '@/views/templates/templateAuthoringSubTabs'
import type {
  AnchorBinding,
  CompositionRule,
  PreviewRecord,
  VariableSchema,
} from '@/types/template'

defineProps<{
  templateId: string
  variables: VariableSchema[]
  bindings: AnchorBinding[]
  rules: CompositionRule[] | null
  groupCode: string | null
  canEditContentModuleReferences: boolean
  coverageRefreshToken: number
  lastPreview: PreviewRecord | null
}>()

const emit = defineEmits<{
  updated: []
  selectedTestDataSet: [id: string | null]
  batchComplete: []
}>()

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const activeSubTab = ref<TemplateAuthoringSubTab>(resolveTemplateAuthoringSubTab(route.query.authoringTab))

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
</script>

<template>
  <el-card shadow="never" class="section-card">
    <h2>{{ t('templates.authoring.title') }}</h2>
    <el-tabs v-model="activeSubTab" class="authoring-sub-tabs">
      <el-tab-pane :label="t(templateAuthoringSubTabLabelKey('variables'))" name="variables">
        <TemplateVariableTreePanel
          :template-id="templateId"
          :variables="variables"
          @updated="emit('updated')"
        />
      </el-tab-pane>

      <el-tab-pane :label="t(templateAuthoringSubTabLabelKey('bindings'))" name="bindings">
        <TemplateAuthoringBindingsPanel
          :template-id="templateId"
          :variables="variables"
          :bindings="bindings"
          @updated="emit('updated')"
        />
      </el-tab-pane>

      <el-tab-pane :label="t(templateAuthoringSubTabLabelKey('rules'))" name="rules">
        <TemplateRuleConfigurator
          :template-id="templateId"
          :initial-rules="rules ?? []"
          @updated="emit('updated')"
        />
      </el-tab-pane>

      <el-tab-pane
        v-if="groupCode"
        :label="t(templateAuthoringSubTabLabelKey('contentModules'))"
        name="contentModules"
      >
        <TemplateContentModuleReferencesPanel
          :template-id="templateId"
          :group-code="groupCode"
          :editable="canEditContentModuleReferences"
          :refresh-token="coverageRefreshToken"
          @updated="emit('updated')"
        />
      </el-tab-pane>

      <el-tab-pane :label="t(templateAuthoringSubTabLabelKey('testPreview'))" name="testPreview">
        <h3>{{ t('templates.testDataSets.title') }}</h3>
        <TemplateTestDataSetPanel
          :template-id="templateId"
          @selected="emit('selectedTestDataSet', $event)"
          @batch-complete="emit('batchComplete')"
        />
        <TemplateCoveragePanel :template-id="templateId" :refresh-token="coverageRefreshToken" />
        <TemplateChangeDiffPanel :template-id="templateId" :refresh-token="coverageRefreshToken" />
        <h3>{{ t('templates.preview.title') }}</h3>
        <TemplatePreviewPanel
          :template-id="templateId"
          :bindings="bindings"
          :preview="lastPreview"
        />
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<style scoped lang="scss">
.section-card {
  margin-bottom: 1.5rem;

  h2 {
    margin: 0 0 1rem;
    font-size: 1.125rem;
  }

  h3 {
    margin: 1.5rem 0 0.75rem;
    font-size: 1rem;

    &:first-child {
      margin-top: 0;
    }
  }
}

.authoring-sub-tabs {
  margin-top: 0.25rem;
}
</style>
