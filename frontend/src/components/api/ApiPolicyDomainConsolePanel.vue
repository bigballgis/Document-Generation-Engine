<script setup lang="ts">
import { computed, inject } from 'vue'
import { useI18n } from 'vue-i18n'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import { API_POLICY_DOMAIN_EDITOR_KEY } from '@/components/api/apiPolicyDomainEditorContext'
import type { ApiPolicyDomain } from '@/types/apiPolicyDomain'

const props = defineProps<{
  activeDomain: ApiPolicyDomain
  policyOutputFormatOptions: string[]
  policyOutputModeOptions: string[]
  variant: 'tab-sections' | 'domain-console'
}>()

const { t } = useI18n()
const { forms, currentSummary } = inject(API_POLICY_DOMAIN_EDITOR_KEY)!
const { adGroupsForm, outputForm, batchForm, encryptionForm, defaultRouteForm } = forms

const consoleOutputModeOptions = computed(() => {
  if (props.variant === 'domain-console') {
    return ['SYNC_STREAM', 'ASYNC_CALLBACK', 'INLINE']
  }
  return props.policyOutputModeOptions
})
</script>

<template>
  <p class="field-hint domain-hint">
    {{ t(`apiPolicy.detail.hints.${activeDomain}`) }}
  </p>

  <div class="current-summary">
    <span class="summary-label">{{ t('apiPolicy.detail.currentSummary') }}</span>
    <span>{{ currentSummary(activeDomain, t) }}</span>
  </div>

  <el-form label-position="top" class="domain-form">
    <template v-if="activeDomain === 'AD_GROUP_AUTHORIZATION'">
      <el-form-item :label="t('templates.policy.allowedAdGroups')">
        <AppSearchSelect
          v-model="adGroupsForm.allowedAdGroups"
          multiple
          filterable
          allow-create
          default-first-option
          :placeholder="t('templates.policy.allowedAdGroupsPlaceholder')"
        />
      </el-form-item>
    </template>

    <template v-else-if="activeDomain === 'OUTPUT_POLICY'">
      <el-form-item :label="t('templates.policy.outputFormats')">
        <AppSearchSelect v-model="outputForm.outputFormats" multiple filterable allow-create>
          <el-option
            v-for="format in policyOutputFormatOptions"
            :key="format"
            :label="format"
            :value="format"
          />
        </AppSearchSelect>
      </el-form-item>
      <el-form-item :label="t('templates.policy.outputModes')">
        <AppSearchSelect v-model="outputForm.outputModes" multiple filterable allow-create>
          <el-option
            v-for="mode in consoleOutputModeOptions"
            :key="mode"
            :label="mode"
            :value="mode"
          />
        </AppSearchSelect>
      </el-form-item>
    </template>

    <template v-else-if="activeDomain === 'BATCH_LIMIT'">
      <el-form-item :label="t('templates.policy.batchEnabled')">
        <el-switch v-model="batchForm.batchEnabled" />
      </el-form-item>
      <el-form-item :label="t('apiPolicy.detail.fields.syncMaxItems')">
        <el-input-number v-model="batchForm.syncMaxItems" :min="1" :max="1000" />
      </el-form-item>
      <el-form-item :label="t('apiPolicy.detail.fields.asyncMaxItems')">
        <el-input-number v-model="batchForm.asyncMaxItems" :min="1" :max="100000" />
      </el-form-item>
    </template>

    <template v-else-if="activeDomain === 'ENCRYPTION_CAPABILITY'">
      <el-form-item :label="t('templates.policy.docxEncryptionEnabled')">
        <el-switch v-model="encryptionForm.docxEncryptionEnabled" />
      </el-form-item>
      <el-form-item :label="t('templates.policy.pdfEncryptionEnabled')">
        <el-switch v-model="encryptionForm.pdfEncryptionEnabled" />
      </el-form-item>
    </template>

    <template v-else-if="activeDomain === 'DEFAULT_ROUTE_TARGET'">
      <el-form-item :label="t('templates.policy.defaultRouteReleaseVersion')">
        <el-input v-model="defaultRouteForm.defaultRouteReleaseVersion" />
      </el-form-item>
    </template>
  </el-form>

  <slot name="console-actions" />
</template>

<style scoped lang="scss">
@use './apiPolicyDomainEditor.shared.scss';
</style>
