<script setup lang="ts">
/* eslint-disable vue/no-mutating-props -- reactive form/filter bag owned by parent */
import { useI18n } from 'vue-i18n'
import type { LifecycleDecisionFormState } from '@/components/templates/lifecycleDecisionDialogTypes'

defineProps<{
  form: LifecycleDecisionFormState
}>()

const { t } = useI18n()
</script>

<template>
  <div>
    <p class="decision-intro">{{ t('templates.lifecycle.decisionForm.passTestIntro') }}</p>
    <el-form-item>
      <el-checkbox
        v-model="form.fidelityViewedConfirmed"
        data-testid="confirm-fidelity-viewed"
      >
        {{ t('templates.lifecycle.decisionForm.confirmFidelityViewed') }}
      </el-checkbox>
    </el-form-item>
    <el-form-item>
      <el-checkbox v-model="form.coverageViewedConfirmed">
        {{ t('templates.lifecycle.decisionForm.confirmCoverageViewed') }}
      </el-checkbox>
    </el-form-item>
    <el-form-item>
      <el-checkbox v-model="form.previewViewedConfirmed">
        {{ t('templates.lifecycle.decisionForm.confirmPreviewViewed') }}
      </el-checkbox>
    </el-form-item>
  </div>
</template>

<style scoped lang="scss">
.decision-intro {
  margin: 0 0 0.75rem;
  color: var(--text-muted);
  font-size: 0.875rem;
}
</style>

