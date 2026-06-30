<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import TemplateAuthoringPanel from '@/components/templates/TemplateAuthoringPanel.vue'
import TemplateRuleConfigurator from '@/components/templates/TemplateRuleConfigurator.vue'
import TemplateContentModuleReferencesPanel from '@/components/templates/TemplateContentModuleReferencesPanel.vue'
import TemplateTestDataSetPanel from '@/components/templates/TemplateTestDataSetPanel.vue'
import TemplateCoveragePanel from '@/components/templates/TemplateCoveragePanel.vue'
import TemplateChangeDiffPanel from '@/components/templates/TemplateChangeDiffPanel.vue'
import TemplatePreviewPanel from '@/components/templates/TemplatePreviewPanel.vue'
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
</script>

<template>
  <el-card shadow="never" class="section-card">
    <h2>{{ t('templates.authoring.title') }}</h2>
    <TemplateAuthoringPanel
      :template-id="templateId"
      :variables="variables"
      :bindings="bindings"
      @updated="emit('updated')"
    />
    <TemplateRuleConfigurator
      :template-id="templateId"
      :initial-rules="rules ?? []"
      @updated="emit('updated')"
    />
    <TemplateContentModuleReferencesPanel
      v-if="groupCode"
      :template-id="templateId"
      :group-code="groupCode"
      :editable="canEditContentModuleReferences"
      :refresh-token="coverageRefreshToken"
      @updated="emit('updated')"
    />
    <h3>{{ t('templates.testDataSets.title') }}</h3>
    <TemplateTestDataSetPanel
      :template-id="templateId"
      @selected="emit('selectedTestDataSet', $event)"
      @batch-complete="emit('batchComplete')"
    />
    <TemplateCoveragePanel :template-id="templateId" :refresh-token="coverageRefreshToken" />
    <TemplateChangeDiffPanel :template-id="templateId" :refresh-token="coverageRefreshToken" />
  </el-card>

  <el-card shadow="never" class="section-card">
    <h2>{{ t('templates.preview.title') }}</h2>
    <TemplatePreviewPanel
      :template-id="templateId"
      :bindings="bindings"
      :preview="lastPreview"
    />
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
  }
}
</style>
