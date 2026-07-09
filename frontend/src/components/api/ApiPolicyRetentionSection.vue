<script setup lang="ts">
import { inject } from 'vue'
import { useI18n } from 'vue-i18n'
import { API_POLICY_DOMAIN_EDITOR_KEY } from '@/components/api/apiPolicyDomainEditorContext'

defineProps<{
  canEdit: boolean
  submitting: boolean
  retentionSaveFeedback: 'success' | 'error' | null
  retentionDirty: boolean
  retentionPresetsValid: boolean
  canSaveRetention: boolean
  recordRetentionOptions: Array<{ value: number; label: string }>
  documentRetentionOptions: Array<{ value: number; label: string }>
  resolveErrorMessage: (fallbackKey: string) => string
}>()

const emit = defineEmits<{
  saveRetention: []
}>()

const { t } = useI18n()
const { forms } = inject(API_POLICY_DOMAIN_EDITOR_KEY)!
const { retentionForm } = forms
</script>

<template>
  <section id="policy-domain-INVOCATION_RETENTION" class="l1-section">
    <h3>{{ t('templates.policy.retention.title') }}</h3>
    <p class="field-hint">{{ t('templates.policy.retention.hint') }}</p>
    <el-alert
      v-if="retentionSaveFeedback === 'success'"
      type="success"
      :title="t('templates.policy.retention.inlineSaveSuccess')"
      show-icon
      :closable="false"
      class="retention-feedback"
      data-testid="retention-save-success"
    />
    <el-alert
      v-else-if="retentionSaveFeedback === 'error'"
      type="error"
      :title="resolveErrorMessage('templates.error.savePolicy')"
      show-icon
      :closable="false"
      class="retention-feedback"
      data-testid="retention-save-error"
    />
    <el-alert
      v-else-if="canEdit && retentionDirty && !retentionPresetsValid"
      type="warning"
      :title="t('templates.policy.retention.invalidPreset')"
      show-icon
      :closable="false"
      class="retention-feedback"
      data-testid="retention-invalid-preset"
    />
    <el-form label-position="top" class="inline-form retention-form">
      <el-form-item :label="t('templates.policy.retention.saveGeneratedDocuments')">
        <el-switch v-model="retentionForm.saveGeneratedDocuments" :disabled="!canEdit" />
      </el-form-item>
      <el-form-item :label="t('templates.policy.retention.recordDays')">
        <el-select
          v-model="retentionForm.invocationRecordRetentionDays"
          :disabled="!canEdit"
          class="retention-select"
        >
          <el-option
            v-for="option in recordRetentionOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item
        v-if="retentionForm.saveGeneratedDocuments"
        :label="t('templates.policy.retention.documentDays')"
      >
        <el-select
          v-model="retentionForm.documentRetentionDays"
          :disabled="!canEdit"
          class="retention-select"
        >
          <el-option
            v-for="option in documentRetentionOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
      </el-form-item>
      <div v-if="canEdit" class="action-row">
        <el-button
          type="primary"
          :loading="submitting"
          :disabled="!canSaveRetention"
          data-testid="retention-save-button"
          @click="emit('saveRetention')"
        >
          {{ t('templates.policy.retention.save') }}
        </el-button>
      </div>
    </el-form>
  </section>
</template>

<style scoped lang="scss">
@use './apiPolicyDomainEditor.shared.scss';
</style>
