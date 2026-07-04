<script setup lang="ts">
import { computed, ref, toRef, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import {
  DOCUMENT_RETENTION_PRESETS,
  INVOCATION_RECORD_RETENTION_PRESETS,
  isBatchLimitsPlatformDefault,
  isEncryptionPlatformDefault,
  isOutputPolicyPlatformDefault,
} from '@/constants/apiPolicyPlatformDefaults'
import { useApiPolicyDomainForms } from '@/composables/useApiPolicyDomainForms'
import { useApiPolicyStore } from '@/stores/apiPolicy'
import {
  buildUpsertPayloadForDomain,
  resolveBatchAsyncMaxItems,
  resolveBatchSyncMaxItems,
  type ApiPolicyDomain,
  type BatchLimitsDomainForm,
  type EncryptionDomainForm,
  type OutputPolicyDomainForm,
} from '@/types/apiPolicyDomain'
import type { ApiPolicy } from '@/types/template'

const props = withDefaults(
  defineProps<{
    templateId: string
    apiPolicy: ApiPolicy | null
    canEdit?: boolean
    submitting?: boolean
    variant: 'tab-sections' | 'domain-console'
    activeDomain?: ApiPolicyDomain
    policyOutputFormatOptions?: string[]
    policyOutputModeOptions?: string[]
  }>(),
  {
    canEdit: false,
    submitting: false,
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

const apiPolicyRef = toRef(props, 'apiPolicy')
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

const { t, te } = useI18n()
const apiPolicyStore = useApiPolicyStore()

const advancedExpanded = ref<string[]>([])

const outputUsesPlatformDefaults = computed(() =>
  isOutputPolicyPlatformDefault(outputForm.outputFormats, outputForm.outputModes),
)
const batchUsesPlatformDefaults = computed(() =>
  isBatchLimitsPlatformDefault(batchForm.batchEnabled, batchForm.syncMaxItems, batchForm.asyncMaxItems),
)
const encryptionUsesPlatformDefaults = computed(() =>
  isEncryptionPlatformDefault(encryptionForm.docxEncryptionEnabled, encryptionForm.pdfEncryptionEnabled),
)

const recordRetentionOptions = computed(() =>
  INVOCATION_RECORD_RETENTION_PRESETS.map((days) => ({
    value: days,
    label: t('templates.policy.retention.presetDays', { days }),
  })),
)

const documentRetentionOptions = computed(() =>
  DOCUMENT_RETENTION_PRESETS.map((days) => ({
    value: days,
    label: t('templates.policy.retention.presetDays', { days }),
  })),
)

const consoleOutputModeOptions = computed(() => {
  if (props.variant === 'domain-console') {
    return ['SYNC_STREAM', 'ASYNC_CALLBACK', 'INLINE']
  }
  return props.policyOutputModeOptions
})

function resolveErrorMessage(fallbackKey: string): string {
  const key = apiPolicyStore.entryFor(props.templateId).lastErrorMessageKey
  if (!key) {
    return t(fallbackKey)
  }
  return te(key) ? t(key) : t(fallbackKey)
}

async function confirmSave(messageKey: string): Promise<boolean> {
  try {
    await ElMessageBox.confirm(t(messageKey), t('apiPolicy.detail.impact.confirmTitle'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: 'warning',
    })
    return true
  } catch {
    return false
  }
}

async function saveAdGroupsDomain() {
  if (!props.canEdit) {
    return
  }
  if (!(await confirmSave('templates.policy.retention.confirmSaveAdGroups'))) {
    return
  }
  try {
    await apiPolicyStore.savePolicyDomain(props.templateId, 'AD_GROUP_AUTHORIZATION', adGroupsForm)
    ElMessage.success(t('apiPolicy.detail.saveSuccess'))
  } catch {
    ElMessage.error(resolveErrorMessage('templates.error.savePolicy'))
  }
}

async function saveDefaultRouteDomain() {
  if (!props.canEdit || !props.apiPolicy) {
    return
  }
  try {
    const payload = buildUpsertPayloadForDomain(
      props.apiPolicy,
      'DEFAULT_ROUTE_TARGET',
      defaultRouteForm,
    )
    const preview = await apiPolicyStore.previewImpact(props.templateId, payload)
    if (preview.blocking) {
      ElMessage.error(t('apiPolicy.detail.impact.blockedSave'))
      return
    }
    if (preview.warnings.length > 0) {
      const warningLines = preview.warnings.map((key) => (te(key) ? t(key) : key))
      try {
        await ElMessageBox.confirm(
          [t('apiPolicy.detail.impact.confirmWarningsIntro'), ...warningLines].join('\n\n'),
          t('apiPolicy.detail.impact.confirmTitle'),
          {
            confirmButtonText: t('common.confirm'),
            cancelButtonText: t('common.cancel'),
            type: 'warning',
          },
        )
      } catch {
        return
      }
    } else if (!(await confirmSave('templates.policy.retention.confirmSaveDefaultRoute'))) {
      return
    }
    await apiPolicyStore.savePolicyDomain(props.templateId, 'DEFAULT_ROUTE_TARGET', defaultRouteForm)
    ElMessage.success(t('apiPolicy.detail.saveSuccess'))
  } catch {
    ElMessage.error(resolveErrorMessage('templates.error.savePolicy'))
  }
}

async function saveRetentionDomain() {
  if (!props.canEdit) {
    return
  }
  if (!(await confirmSave('templates.policy.retention.confirmSaveRetention'))) {
    return
  }
  try {
    await apiPolicyStore.saveInvocationRetentionDomain(props.templateId, { ...retentionForm })
    ElMessage.success(t('templates.policy.retention.saveSuccess'))
  } catch {
    ElMessage.error(resolveErrorMessage('templates.error.savePolicy'))
  }
}

async function saveAdvancedDomain(
  domain: 'OUTPUT_POLICY' | 'BATCH_LIMIT' | 'ENCRYPTION_CAPABILITY',
  form: OutputPolicyDomainForm | BatchLimitsDomainForm | EncryptionDomainForm,
) {
  if (!props.canEdit) {
    return
  }
  if (!(await confirmSave('templates.policy.retention.confirmSaveAdvanced'))) {
    return
  }
  try {
    await apiPolicyStore.savePolicyDomain(props.templateId, domain, form as never)
    ElMessage.success(t('apiPolicy.detail.saveSuccess'))
  } catch {
    ElMessage.error(resolveErrorMessage('templates.error.savePolicy'))
  }
}

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
    emit('formEdited')
  },
)

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
    <section class="l1-section">
      <h3>{{ t('templates.policy.l1.adGroupsTitle') }}</h3>
      <p class="field-hint">{{ t('templates.policy.l1.adGroupsHint') }}</p>
      <dl class="policy-summary">
        <div>
          <dt>{{ t('templates.policy.allowedAdGroups') }}</dt>
          <dd>
            <el-tooltip
              v-if="allowedAdGroupsText"
              :content="allowedAdGroupsText"
              placement="top"
            >
              <span class="policy-value policy-value--truncate policy-ad-groups">
                {{ allowedAdGroupsText }}
              </span>
            </el-tooltip>
            <span v-else class="policy-empty">{{ t('apiPolicy.detail.summary.empty') }}</span>
          </dd>
        </div>
      </dl>
      <el-form v-if="canEdit" label-position="top" class="inline-form">
        <el-form-item :label="t('templates.policy.l1.editAdGroups')">
          <AppSearchSelect
            v-model="adGroupsForm.allowedAdGroups"
            multiple
            filterable
            allow-create
            default-first-option
            :placeholder="t('templates.policy.allowedAdGroupsPlaceholder')"
          />
        </el-form-item>
        <div class="action-row">
          <el-button type="primary" :loading="submitting" @click="saveAdGroupsDomain">
            {{ t('templates.policy.l1.saveAdGroups') }}
          </el-button>
        </div>
      </el-form>
    </section>

    <section class="l1-section">
      <h3>{{ t('templates.policy.l1.defaultRouteTitle') }}</h3>
      <dl class="policy-summary">
        <div>
          <dt>{{ t('templates.policy.defaultRouteReleaseVersion') }}</dt>
          <dd>
            {{ apiPolicy?.defaultRouteReleaseVersion || t('apiPolicy.detail.summary.empty') }}
          </dd>
        </div>
      </dl>
      <el-form v-if="canEdit" label-position="top" class="inline-form">
        <el-form-item :label="t('templates.policy.l1.editDefaultRoute')">
          <el-input v-model="defaultRouteForm.defaultRouteReleaseVersion" />
        </el-form-item>
        <div class="action-row">
          <el-button type="primary" :loading="submitting" @click="saveDefaultRouteDomain">
            {{ t('templates.policy.l1.saveDefaultRoute') }}
          </el-button>
        </div>
      </el-form>
    </section>

    <section class="l1-section">
      <h3>{{ t('templates.policy.retention.title') }}</h3>
      <p class="field-hint">{{ t('templates.policy.retention.hint') }}</p>
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
          <el-button type="primary" :loading="submitting" @click="saveRetentionDomain">
            {{ t('templates.policy.retention.save') }}
          </el-button>
        </div>
      </el-form>
    </section>
  </template>

  <template v-else-if="variant === 'domain-console' && activeDomain">
    <p v-if="activeDomain" class="field-hint domain-hint">
      {{ t(`apiPolicy.detail.hints.${activeDomain}`) }}
    </p>

    <div v-if="activeDomain" class="current-summary">
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

  <el-card
    v-if="variant === 'tab-sections' && apiPolicy"
    shadow="never"
    class="advanced-card"
  >
    <el-collapse v-model="advancedExpanded">
      <el-collapse-item name="advanced" :title="t('templates.policy.advanced.title')">
        <p class="field-hint">{{ t('templates.policy.advanced.hint') }}</p>

        <section class="advanced-block">
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
              <el-button
                :loading="submitting"
                @click="saveAdvancedDomain('OUTPUT_POLICY', outputForm)"
              >
                {{ t('templates.policy.advanced.saveOutput') }}
              </el-button>
            </div>
          </el-form>
          <p v-else class="readonly-summary">
            {{ apiPolicy.outputFormats.join(', ') }} / {{ apiPolicy.outputModes.join(', ') }}
          </p>
        </section>

        <section class="advanced-block">
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
              <el-button :loading="submitting" @click="saveAdvancedDomain('BATCH_LIMIT', batchForm)">
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

        <section class="advanced-block">
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
              <el-button
                :loading="submitting"
                @click="saveAdvancedDomain('ENCRYPTION_CAPABILITY', encryptionForm)"
              >
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
.l1-section {
  padding-bottom: var(--space-5);
  margin-bottom: var(--space-5);
  border-bottom: 1px solid var(--border-color);

  &:last-child {
    margin-bottom: 0;
    padding-bottom: 0;
    border-bottom: none;
  }

  h3 {
    margin: 0 0 var(--space-2);
    font-size: var(--font-size-md);
  }
}

.field-hint {
  margin: 0 0 var(--space-3);
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

.domain-hint {
  margin-top: 0;
}

.policy-summary {
  display: grid;
  gap: var(--space-3);
  margin: 0 0 var(--space-4);

  div {
    display: grid;
    grid-template-columns: 12rem minmax(0, 1fr);
    gap: var(--space-3);
    align-items: start;
  }

  dt {
    margin: 0;
    color: var(--text-muted);
    font-weight: 500;
  }

  dd {
    margin: 0;
    min-width: 0;
  }
}

.policy-value--truncate {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}

.policy-empty {
  color: var(--text-muted);
}

.inline-form {
  max-width: 40rem;
}

.retention-form {
  display: grid;
  gap: var(--space-2);
}

.retention-select {
  width: 100%;
  max-width: 16rem;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  align-items: center;
}

.advanced-card {
  margin-top: var(--space-6);
  margin-bottom: 0;
  border: none;
  padding: 0;

  :deep(.el-card__body) {
    padding: 0;
  }
}

.advanced-block {
  padding-bottom: var(--space-5);
  margin-bottom: var(--space-5);
  border-bottom: 1px solid var(--border-color);

  &:last-child {
    margin-bottom: 0;
    padding-bottom: 0;
    border-bottom: none;
  }

  h4 {
    margin: 0;
    font-size: var(--font-size-md);
  }
}

.advanced-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-3);
}

.readonly-summary {
  margin: 0;
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}

.current-summary {
  display: flex;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-md);
  background: var(--surface-muted);
  font-size: var(--font-size-sm);
}

.summary-label {
  color: var(--text-muted);
  min-width: 7rem;
}

.domain-form {
  max-width: 40rem;
  margin-bottom: var(--space-4);
}
</style>
