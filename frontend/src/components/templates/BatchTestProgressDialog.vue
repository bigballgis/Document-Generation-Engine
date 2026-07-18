<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { useBatchTestProgressStream } from '@/components/templates/useBatchTestProgressStream'

const props = defineProps<{
  modelValue: boolean
  templateId: string
  runId: string
  streamUrl: string
  dataSetCount: number
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  completed: []
}>()

const { t } = useI18n()

const {
  phase,
  completedCount,
  samples,
  currentSample,
  summarySuccessCount,
  summaryFailedCount,
  anchorCoveragePct,
  variableCoveragePct,
  sampleCoveragePct,
  gatePassed,
  errorMessage,
  progressPercent,
  progressStatus,
  handleClose,
} = useBatchTestProgressStream({
  modelValue: () => props.modelValue,
  streamUrl: () => props.streamUrl,
  dataSetCount: () => props.dataSetCount,
  onCompleted: () => emit('completed'),
  onClose: () => emit('update:modelValue', false),
})
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="t('templates.batchTest.progressTitle')"
    width="560px"
    :close-on-click-modal="false"
    :close-on-press-escape="phase !== 'running'"
    @update:model-value="handleClose"
  >
    <div class="batch-progress">
      <div class="batch-progress__counter">
        {{ t('templates.batchTest.completedCount', { done: completedCount, total: dataSetCount }) }}
      </div>

      <el-progress
        :percentage="progressPercent"
        :status="progressStatus"
        :striped="phase === 'running'"
        :striped-flow="phase === 'running'"
        class="batch-progress__bar"
      />

      <p
        v-if="phase === 'running' && currentSample"
        class="batch-progress__current"
      >
        {{
          t('templates.batchTest.currentSample', {
            externalId: currentSample.externalId,
            index: currentSample.index,
            total: currentSample.total,
          })
        }}
      </p>

      <div v-if="samples.length" class="batch-progress__results">
        <div
          v-for="s in samples"
          :key="s.sampleIndex"
          class="batch-progress__result-row"
          :class="s.success ? 'is-success' : 'is-failed'"
        >
          <el-tag :type="s.success ? 'success' : 'danger'" size="small">
            {{ s.success ? t('templates.batchTest.resultSuccess') : t('templates.batchTest.resultFailed') }}
          </el-tag>
          <span class="batch-progress__result-name">{{ s.dataSetExternalId }}</span>
          <span v-if="!s.success && s.errorDetail" class="batch-progress__result-error">
            {{ s.errorDetail }}
          </span>
        </div>
      </div>

      <div v-if="phase === 'completed'" class="batch-progress__summary">
        <el-divider />
        <h4 class="batch-progress__summary-title">{{ t('templates.batchTest.summaryTitle') }}</h4>
        <div class="batch-progress__summary-counts">
          <span class="is-success">{{ t('templates.batchTest.summarySuccess', { count: summarySuccessCount }) }}</span>
          <span v-if="summaryFailedCount > 0" class="is-failed">
            {{ t('templates.batchTest.summaryFailed', { count: summaryFailedCount }) }}
          </span>
        </div>
        <div class="batch-progress__summary-gate">
          <el-tag :type="gatePassed ? 'success' : 'warning'" size="small">
            {{ gatePassed ? t('templates.batchTest.readinessPassed') : t('templates.batchTest.readinessFailed') }}
          </el-tag>
        </div>
        <div v-if="anchorCoveragePct !== null" class="batch-progress__coverage">
          <span>{{ t('templates.batchTest.placeholderBindingCoverage', { pct: anchorCoveragePct }) }}</span>
          <span>{{ t('templates.batchTest.variableCoverage', { pct: variableCoveragePct }) }}</span>
          <span>{{ t('templates.batchTest.sampleCoverage', { pct: sampleCoveragePct }) }}</span>
        </div>
      </div>

      <el-alert
        v-if="phase === 'failed'"
        type="error"
        :title="errorMessage"
        :closable="false"
        show-icon
        class="batch-progress__error"
      />
    </div>

    <template #footer>
      <el-button @click="handleClose">
        {{ t('templates.previewProgress.closeButton') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss" src="./BatchTestProgressDialog.scss"></style>
