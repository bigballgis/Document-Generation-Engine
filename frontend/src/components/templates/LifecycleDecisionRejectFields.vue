<script setup lang="ts">
/* eslint-disable vue/no-mutating-props -- reactive form/filter bag owned by parent */
import { useI18n } from 'vue-i18n'
import type { LifecycleDecisionFormState } from '@/components/templates/lifecycleDecisionDialogTypes'
import type { LifecycleDecisionDialogMode } from '@/components/templates/lifecycleDecisionDialogTypes'

defineProps<{
  form: LifecycleDecisionFormState
  mode: LifecycleDecisionDialogMode
  availableReasonCategories: string[]
  selectedReasonPrompt: string
}>()

const { t } = useI18n()
</script>

<template>
  <div>
    <el-form-item
      :label="t('templates.lifecycle.decisionForm.reasonCategory')"
      prop="reasonCategory"
      required
    >
      <el-select
        v-model="form.reasonCategory"
        filterable
        :placeholder="t('templates.lifecycle.decisionForm.reasonCategoryPlaceholder')"
        style="width: 100%"
      >
        <el-option
          v-for="category in availableReasonCategories"
          :key="category"
          :label="t(`templates.lifecycle.decisionForm.reasonCategories.${category}`)"
          :value="category"
        />
      </el-select>
    </el-form-item>
    <p v-if="selectedReasonPrompt" class="decision-intro">{{ selectedReasonPrompt }}</p>
    <el-form-item
      :label="t('templates.lifecycle.decisionForm.impactSummary')"
      prop="impactSummary"
      required
    >
      <el-input
        v-model="form.impactSummary"
        type="textarea"
        :rows="4"
        maxlength="2048"
        show-word-limit
        :placeholder="t('templates.lifecycle.decisionForm.impactSummaryPlaceholder')"
      />
    </el-form-item>
    <template v-if="mode === 'approval-reject' || mode === 'test-fail'">
      <p class="decision-intro">{{ t('templates.lifecycle.decisionForm.remediationIntro') }}</p>
      <el-form-item :label="t('templates.lifecycle.decisionForm.remediationTestRecordId')">
        <el-input v-model="form.remediationTestRecordId" maxlength="64" />
      </el-form-item>
      <el-form-item :label="t('templates.lifecycle.decisionForm.remediationChangeDiffRef')">
        <el-input v-model="form.remediationChangeDiffRef" maxlength="64" />
      </el-form-item>
      <el-form-item :label="t('templates.lifecycle.decisionForm.remediationChecklistCode')">
        <el-input v-model="form.remediationChecklistCode" maxlength="64" />
      </el-form-item>
    </template>
    <el-form-item :label="t('templates.lifecycle.decisionForm.optionalComment')">
      <el-input
        v-model="form.commentSummary"
        type="textarea"
        :rows="2"
        maxlength="2048"
        :placeholder="t('templates.lifecycle.commentPlaceholder')"
      />
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

