<script setup lang="ts">
import { computed, provide, ref, toRef, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { API_POLICY_DOMAIN_EDITOR_KEY } from '@/components/api/apiPolicyDomainEditorContext'
import ApiPolicyAdGroupsDefaultRouteSections from '@/components/api/ApiPolicyAdGroupsDefaultRouteSections.vue'
import ApiPolicyAdvancedSettingsCard from '@/components/api/ApiPolicyAdvancedSettingsCard.vue'
import ApiPolicyDomainConsolePanel from '@/components/api/ApiPolicyDomainConsolePanel.vue'
import ApiPolicyRetentionSection from '@/components/api/ApiPolicyRetentionSection.vue'
import { useApiPolicyDomainAnchor } from '@/composables/useApiPolicyDomainAnchor'
import { useApiPolicyDomainEditorActions } from '@/composables/useApiPolicyDomainEditorActions'
import { useApiPolicyDomainForms } from '@/composables/useApiPolicyDomainForms'
import { useConfirmAction } from '@/composables/useConfirmAction'
import { useApiPolicyStore } from '@/stores/apiPolicy'
import type { ApiPolicyDomain } from '@/types/apiPolicyDomain'
import type { ApiPolicy } from '@/types/template'

const props = withDefaults(
  defineProps<{
    templateId: string
    apiPolicy: ApiPolicy | null
    canEdit?: boolean
    submitting?: boolean
    variant: 'tab-sections' | 'domain-console'
    activeDomain?: ApiPolicyDomain
    initialDomainAnchor?: ApiPolicyDomain | null
    policyOutputFormatOptions?: string[]
    policyOutputModeOptions?: string[]
  }>(),
  {
    canEdit: false,
    submitting: false,
    initialDomainAnchor: null,
    policyOutputFormatOptions: () => ['DOCX', 'PDF'],
    policyOutputModeOptions: () => [
      'SYNC_STREAM',
      'SYNC_DOWNLOAD_URL',
      'ASYNC_TASK',
      'ASYNC_CALLBACK',
      'INLINE',
    ],
  },
)

const emit = defineEmits<{
  formEdited: []
}>()

const { t } = useI18n()
const { confirmAction } = useConfirmAction()
const apiPolicyStore = useApiPolicyStore()
const rollbackTargetVersion = ref<number | null>(null)

const apiPolicyRef = toRef(props, 'apiPolicy')
const templateIdRef = toRef(props, 'templateId')
const canEditRef = computed(() => props.canEdit)
const initialDomainAnchorRef = toRef(props, 'initialDomainAnchor')
const canRollback = computed(
  () => props.canEdit && (props.apiPolicy?.policyVersion ?? 0) > 1,
)

const {
  adGroupsForm,
  defaultRouteForm,
  retentionForm,
  outputForm,
  batchForm,
  encryptionForm,
  allowedAdGroupsText,
  syncFormsFromPolicy,
  domainCandidate,
  currentSummary,
} = useApiPolicyDomainForms(apiPolicyRef)

const { advancedExpanded } = useApiPolicyDomainAnchor(initialDomainAnchorRef)

const {
  retentionSaveFeedback,
  lastImpactPreview,
  saveBlockedByImpact,
  recordRetentionOptions,
  documentRetentionOptions,
  retentionDirty,
  retentionPresetsValid,
  canSaveRetention,
  resolveErrorMessage,
  saveAdGroupsDomain,
  saveDefaultRouteDomain,
  saveRetentionDomain,
  saveOutputDomain,
  saveBatchDomain,
  saveEncryptionDomain,
  clearRetentionSaveFeedback,
  clearLastImpactPreview,
} = useApiPolicyDomainEditorActions(templateIdRef, apiPolicyRef, canEditRef, {
  adGroupsForm,
  defaultRouteForm,
  retentionForm,
  outputForm,
  batchForm,
  encryptionForm,
})

provide(API_POLICY_DOMAIN_EDITOR_KEY, {
  apiPolicy: apiPolicyRef,
  forms: {
    adGroupsForm,
    defaultRouteForm,
    retentionForm,
    outputForm,
    batchForm,
    encryptionForm,
  },
  allowedAdGroupsText,
  currentSummary,
})

watch(
  () => [
    adGroupsForm.allowedAdGroups,
    outputForm.outputFormats,
    outputForm.outputModes,
    batchForm.batchEnabled,
    batchForm.syncMaxItems,
    batchForm.asyncMaxItems,
    encryptionForm.docxEncryptionEnabled,
    encryptionForm.pdfEncryptionEnabled,
    defaultRouteForm.defaultRouteReleaseVersion,
    retentionForm.saveGeneratedDocuments,
    retentionForm.invocationRecordRetentionDays,
    retentionForm.documentRetentionDays,
  ],
  () => {
    clearRetentionSaveFeedback()
    clearLastImpactPreview()
    emit('formEdited')
  },
)

async function handleRollback() {
  const target = rollbackTargetVersion.value
  if (!target || !canRollback.value) {
    return
  }
  const confirmed = await confirmAction({
    titleKey: 'apiPolicy.detail.impact.rollbackTitle',
    messageKey: 'apiPolicy.detail.impact.rollbackHint',
    type: 'warning',
  })
  if (!confirmed) {
    return
  }
  try {
    await apiPolicyStore.previewPolicyRollback(props.templateId, target)
    await apiPolicyStore.rollbackPolicy(props.templateId, target, true)
    await apiPolicyStore.fetchPolicy(props.templateId)
    syncFormsFromPolicy()
    ElMessage.success(t('apiPolicy.detail.impact.rollbackSuccess'))
  } catch {
    ElMessage.error(resolveErrorMessage('templates.error.savePolicy'))
  }
}

defineExpose({
  adGroupsForm,
  defaultRouteForm,
  retentionForm,
  outputForm,
  batchForm,
  encryptionForm,
  syncFormsFromPolicy,
  domainCandidate,
  currentSummary,
})
</script>

<template>
  <template v-if="variant === 'tab-sections'">
    <ApiPolicyAdGroupsDefaultRouteSections
      :can-edit="canEdit"
      :submitting="submitting"
      :save-blocked-by-impact="saveBlockedByImpact"
      :last-impact-preview="lastImpactPreview"
      @save-ad-groups="saveAdGroupsDomain"
      @save-default-route="saveDefaultRouteDomain"
    />
    <section v-if="canRollback && apiPolicy" class="policy-rollback" data-testid="api-policy-rollback">
      <h3>{{ t('apiPolicy.detail.impact.rollbackTitle') }}</h3>
      <p class="field-hint">{{ t('apiPolicy.detail.impact.rollbackHint') }}</p>
      <el-form label-position="top" inline>
        <el-form-item :label="t('apiPolicy.detail.impact.rollbackVersion')">
          <el-input-number
            v-model="rollbackTargetVersion"
            :min="1"
            :max="Math.max(1, apiPolicy.policyVersion - 1)"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="warning"
            :loading="submitting"
            :disabled="!rollbackTargetVersion"
            @click="handleRollback"
          >
            {{ t('apiPolicy.detail.impact.rollbackAction') }}
          </el-button>
        </el-form-item>
      </el-form>
    </section>
    <ApiPolicyRetentionSection
      :can-edit="canEdit"
      :submitting="submitting"
      :retention-save-feedback="retentionSaveFeedback"
      :retention-dirty="retentionDirty"
      :retention-presets-valid="retentionPresetsValid"
      :can-save-retention="canSaveRetention"
      :record-retention-options="recordRetentionOptions"
      :document-retention-options="documentRetentionOptions"
      :resolve-error-message="resolveErrorMessage"
      @save-retention="saveRetentionDomain"
    />
  </template>

  <ApiPolicyDomainConsolePanel
    v-else-if="variant === 'domain-console' && activeDomain"
    :active-domain="activeDomain"
    :policy-output-format-options="policyOutputFormatOptions"
    :policy-output-mode-options="policyOutputModeOptions"
    variant="domain-console"
  >
    <template #console-actions>
      <slot name="console-actions" />
    </template>
  </ApiPolicyDomainConsolePanel>

  <ApiPolicyAdvancedSettingsCard
    v-if="variant === 'tab-sections' && apiPolicy"
    :api-policy="apiPolicy"
    :can-edit="canEdit"
    :submitting="submitting"
    :save-blocked-by-impact="saveBlockedByImpact"
    v-model:advanced-expanded="advancedExpanded"
    :policy-output-format-options="policyOutputFormatOptions"
    :policy-output-mode-options="policyOutputModeOptions"
    @save-output="saveOutputDomain"
    @save-batch="saveBatchDomain"
    @save-encryption="saveEncryptionDomain"
  />
</template>
