<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import * as templatesApi from '@/api/templates'
import ContextHelpTrigger from '@/components/common/ContextHelpTrigger.vue'
import TemplateVariableTreePanel from '@/components/templates/TemplateVariableTreePanel.vue'
import TemplateAuthoringBindingsPanel from '@/components/templates/TemplateAuthoringBindingsPanel.vue'
import TemplateClauseAuthoringPanel from '@/components/templates/TemplateClauseAuthoringPanel.vue'
import {
  resolveTemplateAuthoringSubTab,
  templateAuthoringSubTabLabelKey,
  type TemplateAuthoringSubTab,
} from '@/views/templates/templateAuthoringSubTabs'
import type {
  AnchorBinding,
  CompositionRule,
  TemplateContentModuleReference,
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
}>()

const emit = defineEmits<{
  updated: []
}>()

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const activeSubTab = ref<TemplateAuthoringSubTab>(resolveTemplateAuthoringSubTab(route.query.authoringTab))
const contentModuleReferences = ref<TemplateContentModuleReference[]>([])

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
</style>
