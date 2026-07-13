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
    <p class="decision-intro">{{ t('templates.lifecycle.decisionForm.approveIntro') }}</p>
    <el-form-item
      :label="t('templates.lifecycle.decisionForm.approvalRationale')"
      prop="commentSummary"
      required
    >
      <el-input
        v-model="form.commentSummary"
        type="textarea"
        :rows="3"
        maxlength="2048"
        show-word-limit
        :placeholder="t('templates.lifecycle.decisionForm.approvalRationalePlaceholder')"
      />
    </el-form-item>
    <el-form-item>
      <el-checkbox
        v-model="form.fidelityViewedConfirmed"
        data-testid="confirm-fidelity-viewed"
      >
        {{ t('templates.lifecycle.decisionForm.confirmFidelityViewed') }}
      </el-checkbox>
    </el-form-item>
    <el-form-item>
      <el-checkbox v-model="form.keyEvidenceConfirmed">
        {{ t('templates.lifecycle.decisionForm.confirmKeyEvidence') }}
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

