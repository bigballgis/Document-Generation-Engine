<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { useTemplatePanelDataStore } from '@/stores/templatePanelData'
import ContextHelpTrigger from '@/components/common/ContextHelpTrigger.vue'
import TemplateVariableTreePanel from '@/components/templates/TemplateVariableTreePanel.vue'
import TemplateAuthoringBindingsPanel from '@/components/templates/TemplateAuthoringBindingsPanel.vue'
import TemplateClauseAuthoringPanel from '@/components/templates/TemplateClauseAuthoringPanel.vue'
import {
  resolveDesignSubTabFromQuery,
  buildDevWorkspaceQuery,
} from '@/views/templates/templateDevWorkspaceTabs'
import {
  templateAuthoringSubTabLabelKey,
  type TemplateAuthoringSubTab,
} from '@/views/templates/templateAuthoringSubTabs'
import type {
  AnchorBinding,
  CompositionRule,
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
const panelDataStore = useTemplatePanelDataStore()

const activeSubTab = ref<TemplateAuthoringSubTab>(resolveDesignSubTabFromQuery(route.query))
const entry = computed(() => panelDataStore.getEntry(props.templateId))
const contentModuleReferences = computed(() =>
  props.groupCode ? entry.value.contentModuleReferences : [],
)

watch(
  () => [route.query.designTab, route.query.authoringTab],
  () => {
    activeSubTab.value = resolveDesignSubTabFromQuery(route.query)
  },
)

watch(activeSubTab, (tab) => {
  if (resolveDesignSubTabFromQuery(route.query) === tab) {
    return
  }
  void router.replace({
    query: buildDevWorkspaceQuery(route.query, 'design', tab),
  })
})

watch(
  () => props.coverageRefreshToken,
  () => {
    void loadContentModuleReferences()
  },
)

function handleUpdated() {
  emit('updated')
}

async function loadContentModuleReferences() {
  if (!props.groupCode) {
    return
  }
  try {
    await panelDataStore.fetchContentModuleReferences(props.templateId)
  } catch {
    panelDataStore.invalidateContentModuleReferenceDomains(props.templateId)
  }
}

onMounted(() => {
  void loadContentModuleReferences()
})
</script>

<template>
  <el-card shadow="never" class="section-card">
    <div class="section-card__heading">
      <h2>{{ t('templates.devWorkspace.design.title') }}</h2>
      <ContextHelpTrigger
        :title="t('templates.authoring.helpTitle')"
        :content="t('templates.authoring.helpContent')"
      />
    </div>

    <el-tabs v-model="activeSubTab" class="design-sub-tabs">
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

.design-sub-tabs {
  margin-top: 0.25rem;
}
</style>
