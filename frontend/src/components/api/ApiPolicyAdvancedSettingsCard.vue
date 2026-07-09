<script setup lang="ts">
import { computed, inject } from 'vue'
import { useI18n } from 'vue-i18n'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import { API_POLICY_DOMAIN_EDITOR_KEY } from '@/components/api/apiPolicyDomainEditorContext'
import {
  isBatchLimitsPlatformDefault,
  isEncryptionPlatformDefault,
  isOutputPolicyPlatformDefault,
} from '@/constants/apiPolicyPlatformDefaults'
import {
  resolveBatchAsyncMaxItems,
  resolveBatchSyncMaxItems,
} from '@/types/apiPolicyDomain'
import type { ApiPolicy } from '@/types/template'

const props = defineProps<{
  apiPolicy: ApiPolicy
  canEdit: boolean
  submitting: boolean
  advancedExpanded: string[]
  policyOutputFormatOptions: string[]
  policyOutputModeOptions: string[]
}>()

const emit = defineEmits<{
  'update:advancedExpanded': [value: string[]]
  saveOutput: []
  saveBatch: []
  saveEncryption: []
}>()

const { t } = useI18n()
const { forms } = inject(API_POLICY_DOMAIN_EDITOR_KEY)!
const { outputForm, batchForm, encryptionForm } = forms

const advancedExpandedModel = computed({
  get: () => props.advancedExpanded,
  set: (value: string[]) => emit('update:advancedExpanded', value),
})

const outputUsesPlatformDefaults = computed(() =>
  isOutputPolicyPlatformDefault(outputForm.outputFormats, outputForm.outputModes),
)
const batchUsesPlatformDefaults = computed(() =>
  isBatchLimitsPlatformDefault(
    batchForm.batchEnabled,
    batchForm.syncMaxItems,
    batchForm.asyncMaxItems,
  ),
)
const encryptionUsesPlatformDefaults = computed(() =>
  isEncryptionPlatformDefault(
    encryptionForm.docxEncryptionEnabled,
    encryptionForm.pdfEncryptionEnabled,
  ),
)
</script>

<template>
  <el-card shadow="never" class="advanced-card">
    <el-collapse v-model="advancedExpandedModel">
      <el-collapse-item name="advanced" :title="t('templates.policy.advanced.title')">
        <p class="field-hint">{{ t('templates.policy.advanced.hint') }}</p>

        <section id="policy-domain-OUTPUT_POLICY" class="advanced-block">
          <div class="advanced-header">
            <h4>{{ t('apiPolicy.detail.domains.OUTPUT_POLICY') }}</h4>
            <el-tag v-if="outputUsesPlatformDefaults" type="info" effect="plain">
              {{ t('templates.policy.platformDefaults') }}
            </el-tag>
          </div>
          <el-form v-if="canEdit" label-position="top" class="inline-form">
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
                  v-for="mode in policyOutputModeOptions"
                  :key="mode"
                  :label="mode"
                  :value="mode"
                />
              </AppSearchSelect>
            </el-form-item>
            <div class="action-row">
              <el-button :loading="submitting" @click="emit('saveOutput')">
                {{ t('templates.policy.advanced.saveOutput') }}
              </el-button>
            </div>
          </el-form>
          <p v-else class="readonly-summary">
            {{ apiPolicy.outputFormats.join(', ') }} / {{ apiPolicy.outputModes.join(', ') }}
          </p>
        </section>

        <section id="policy-domain-BATCH_LIMIT" class="advanced-block">
          <div class="advanced-header">
            <h4>{{ t('apiPolicy.detail.domains.BATCH_LIMIT') }}</h4>
            <el-tag v-if="batchUsesPlatformDefaults" type="info" effect="plain">
              {{ t('templates.policy.platformDefaults') }}
            </el-tag>
          </div>
          <el-form v-if="canEdit" label-position="top" class="inline-form">
            <el-form-item :label="t('templates.policy.batchEnabled')">
              <el-switch v-model="batchForm.batchEnabled" />
            </el-form-item>
            <el-form-item :label="t('apiPolicy.detail.fields.syncMaxItems')">
              <el-input-number v-model="batchForm.syncMaxItems" :min="1" :max="1000" />
            </el-form-item>
            <el-form-item :label="t('apiPolicy.detail.fields.asyncMaxItems')">
              <el-input-number v-model="batchForm.asyncMaxItems" :min="1" :max="100000" />
            </el-form-item>
            <div class="action-row">
              <el-button :loading="submitting" @click="emit('saveBatch')">
                {{ t('templates.policy.advanced.saveBatch') }}
              </el-button>
            </div>
          </el-form>
          <p v-else class="readonly-summary">
            {{
              apiPolicy.batchEnabled
                ? t('apiPolicy.detail.summary.batchEnabled', {
                    sync: resolveBatchSyncMaxItems(apiPolicy),
                    async: resolveBatchAsyncMaxItems(apiPolicy),
                  })
                : t('apiPolicy.detail.summary.batchDisabled')
            }}
          </p>
        </section>

        <section id="policy-domain-ENCRYPTION_CAPABILITY" class="advanced-block">
          <div class="advanced-header">
            <h4>{{ t('apiPolicy.detail.domains.ENCRYPTION_CAPABILITY') }}</h4>
            <el-tag v-if="encryptionUsesPlatformDefaults" type="info" effect="plain">
              {{ t('templates.policy.platformDefaults') }}
            </el-tag>
          </div>
          <el-form v-if="canEdit" label-position="top" class="inline-form">
            <el-form-item :label="t('templates.policy.docxEncryptionEnabled')">
              <el-switch v-model="encryptionForm.docxEncryptionEnabled" />
            </el-form-item>
            <el-form-item :label="t('templates.policy.pdfEncryptionEnabled')">
              <el-switch v-model="encryptionForm.pdfEncryptionEnabled" />
            </el-form-item>
            <div class="action-row">
              <el-button :loading="submitting" @click="emit('saveEncryption')">
                {{ t('templates.policy.advanced.saveEncryption') }}
              </el-button>
            </div>
          </el-form>
          <p v-else class="readonly-summary">
            {{
              [apiPolicy.docxEncryptionEnabled ? 'DOCX' : null, apiPolicy.pdfEncryptionEnabled ? 'PDF' : null]
                .filter(Boolean)
                .join(', ') || t('apiPolicy.detail.summary.encryptionNone')
            }}
          </p>
        </section>
      </el-collapse-item>
    </el-collapse>
  </el-card>
</template>

<style scoped lang="scss">
@use './apiPolicyDomainEditor.shared.scss';
</style>
