<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import SectionPanelHeader from '@/components/common/SectionPanelHeader.vue'
import LifecycleCommentDialog from '@/components/common/LifecycleCommentDialog.vue'
import type { TemplateLifecycleStatus } from '@/types/template'

const props = withDefaults(
  defineProps<{
    lifecycleStatus: TemplateLifecycleStatus
    selectedTestDataSetId: string | null
    showDraftActions: boolean
    showTestingDecisionActions: boolean
    showTestGenerate: boolean
    submitting: boolean
    generatingPreview: boolean
    batchTesting: boolean
    hasDataSets: boolean
    openSubmitDialog?: boolean
    actionsHidden?: boolean
  }>(),
  {
    actionsHidden: false,
  },
)

const emit = defineEmits<{
  'update:openSubmitDialog': [value: boolean]
  'test-generate-selected': []
  'test-generate-batch': []
  'submit-for-test': [comment: string]
  'test-decision': [decision: 'PASSED' | 'FAILED']
}>()

const { t } = useI18n()

const submitDialogVisible = ref(false)

const statusLabelKey = computed(() => `templates.status.${props.lifecycleStatus}`)

const canRunSelected = computed(
  () => props.showTestGenerate && Boolean(props.selectedTestDataSetId) && !props.generatingPreview,
)

watch(
  () => props.openSubmitDialog,
  (requested) => {
    if (requested) {
      submitDialogVisible.value = true
      emit('update:openSubmitDialog', false)
    }
  },
)

function requestSubmitDialog() {
  submitDialogVisible.value = true
}

function confirmSubmit(comment: string) {
  submitDialogVisible.value = false
  emit('submit-for-test', comment)
}
</script>

<template>
  <section class="test-preview-workflow">
    <SectionPanelHeader
      :title="t('templates.testPreview.workflow.title')"
      :help-title="t('templates.testPreview.workflow.helpTitle')"
      :help-content="t('templates.testPreview.workflow.helpContent')"
    />

    <div class="test-preview-workflow__status-row">
      <span class="test-preview-workflow__status-label">{{ t('templates.testPreview.workflow.status') }}</span>
      <el-tag size="small" type="info">{{ t(statusLabelKey) }}</el-tag>
    </div>

    <div v-if="!actionsHidden" class="test-preview-workflow__actions">
      <el-button-group v-if="showTestGenerate">
        <el-button
          type="primary"
          :loading="generatingPreview"
          :disabled="!canRunSelected"
          @click="emit('test-generate-selected')"
        >
          {{ t('templates.testPreview.workflow.runSelected') }}
        </el-button>
        <el-button
          :loading="batchTesting"
          :disabled="!hasDataSets || generatingPreview"
          @click="emit('test-generate-batch')"
        >
          {{ t('templates.testPreview.workflow.runAll') }}
        </el-button>
      </el-button-group>

      <el-button
        v-if="showDraftActions"
        type="success"
        :loading="submitting"
        :disabled="!hasDataSets"
        @click="requestSubmitDialog"
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
    </div>

    <LifecycleCommentDialog
      v-model="submitDialogVisible"
      :title="t('templates.testPreview.workflow.submitDialogTitle')"
      :message="t('templates.testPreview.workflow.submitDialogMessage')"
      :confirm-label="t('templates.lifecycle.submitTest')"
      :loading="submitting"
      @confirm="confirmSubmit"
    />
  </section>
</template>

<style scoped lang="scss">
.test-preview-workflow {
  margin-bottom: 1rem;
  padding: 0.875rem 1rem;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--surface-muted, #fafbfc);

  &__status-row {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    margin-bottom: 0.625rem;
  }

  &__status-label {
    font-size: 0.875rem;
    color: var(--text-muted);
  }

  &__actions {
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem;
    align-items: center;
  }
}
</style>
