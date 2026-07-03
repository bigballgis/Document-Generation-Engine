<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import CredentialsPanel from '@/components/api/CredentialsPanel.vue'
import TemplateCallerContractPanel from '@/components/templates/TemplateCallerContractPanel.vue'
import TemplateRecentInvocationsPanel from '@/components/templates/TemplateRecentInvocationsPanel.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import {
  DOCUMENT_RETENTION_PRESETS,
  INVOCATION_RECORD_RETENTION_PRESETS,
  isBatchLimitsPlatformDefault,
  isEncryptionPlatformDefault,
  isOutputPolicyPlatformDefault,
} from '@/constants/apiPolicyPlatformDefaults'
import { DEFAULT_ENVIRONMENT, type RuntimeEnvironment } from '@/config/environments'
import { useCapabilities } from '@/composables/useCapabilities'
import {
  buildUpsertPayloadForDomain,
  createDomainFormFromPolicy,
  createRetentionFormFromPolicy,
  resolveBatchAsyncMaxItems,
  resolveBatchSyncMaxItems,
  type AdGroupsDomainForm,
  type BatchLimitsDomainForm,
  type DefaultRouteDomainForm,
  type EncryptionDomainForm,
  type OutputPolicyDomainForm,
} from '@/types/apiPolicyDomain'
import { useTemplatesStore } from '@/stores/templates'
import type { ApiCredentialSummary, ApiPolicy } from '@/types/template'

const props = defineProps<{
  templateId: string
  showPolicyPanel: boolean
  loadingPolicy: boolean
  apiPolicy: ApiPolicy | null
  policyLoadFailed: boolean
  policyLoadErrorKey: string | null
  paginatedCredentials: ApiCredentialSummary[]
  credentialStatusFilterOptions: Array<{ label: string; value: string }>
  pageSize: number
  totalCredentialRows: number
  submitting: boolean
  formatDateTime: (value: string) => string
  sortCredentialsByCreatedAt: (a: ApiCredentialSummary, b: ApiCredentialSummary) => number
}>()

const credentialColumnFilters = defineModel<Record<string, string>>('credentialColumnFilters', {
  required: true,
})

const credentialsCurrentPage = defineModel<number>('credentialsCurrentPage', { required: true })
const selectedContractEnvironment = defineModel<RuntimeEnvironment>('selectedContractEnvironment', {
  required: true,
})

const emit = defineEmits<{
  createCredential: []
  rotateCredential: [credentialId: string, externalId: string]
  revokeCredential: [credentialId: string]
  retryPolicyLoad: []
}>()

const { t, te } = useI18n()
const templatesStore = useTemplatesStore()
const { manageApiPolicy } = useCapabilities()

const credentialsPanelRef = ref<InstanceType<typeof CredentialsPanel> | null>(null)
const advancedExpanded = ref<string[]>([])

const policyOutputFormatOptions = ['DOCX', 'PDF']
const policyOutputModeOptions = ['SYNC_STREAM', 'SYNC_DOWNLOAD_URL', 'ASYNC_TASK', 'ASYNC_CALLBACK', 'INLINE']

const adGroupsForm = reactive<AdGroupsDomainForm>({ allowedAdGroups: [] })
const defaultRouteForm = reactive<DefaultRouteDomainForm>({ defaultRouteReleaseVersion: '' })
const retentionForm = reactive(createRetentionFormFromPolicy({
  templateId: '',
  policyVersion: 0,
  allowedAdGroups: [],
  defaultRouteReleaseVersion: '',
  outputFormats: [],
  outputModes: [],
  batchEnabled: false,
  maxBatchSize: 1,
  docxEncryptionEnabled: false,
  pdfEncryptionEnabled: false,
  saveGeneratedDocuments: true,
  invocationRecordRetentionDays: 90,
  documentRetentionDays: 30,
  updatedAt: '',
}))
const outputForm = reactive<OutputPolicyDomainForm>({ outputFormats: [], outputModes: [] })
const batchForm = reactive<BatchLimitsDomainForm>({
  batchEnabled: false,
  syncMaxItems: 100,
  asyncMaxItems: 10_000,
})
const encryptionForm = reactive<EncryptionDomainForm>({
  docxEncryptionEnabled: false,
  pdfEncryptionEnabled: false,
})

const canEditPolicy = computed(() => manageApiPolicy.value && Boolean(props.apiPolicy))
const allowedAdGroupsText = computed(() => props.apiPolicy?.allowedAdGroups.join(', ') ?? '')

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

const outputUsesPlatformDefaults = computed(() =>
  isOutputPolicyPlatformDefault(outputForm.outputFormats, outputForm.outputModes),
)
const batchUsesPlatformDefaults = computed(() =>
  isBatchLimitsPlatformDefault(batchForm.batchEnabled, batchForm.syncMaxItems, batchForm.asyncMaxItems),
)
const encryptionUsesPlatformDefaults = computed(() =>
  isEncryptionPlatformDefault(encryptionForm.docxEncryptionEnabled, encryptionForm.pdfEncryptionEnabled),
)

function syncFormsFromPolicy() {
  if (!props.apiPolicy) {
    return
  }
  Object.assign(adGroupsForm, createDomainFormFromPolicy(props.apiPolicy, 'AD_GROUP_AUTHORIZATION'))
  Object.assign(defaultRouteForm, createDomainFormFromPolicy(props.apiPolicy, 'DEFAULT_ROUTE_TARGET'))
  Object.assign(retentionForm, createRetentionFormFromPolicy(props.apiPolicy))
  Object.assign(outputForm, createDomainFormFromPolicy(props.apiPolicy, 'OUTPUT_POLICY'))
  Object.assign(batchForm, createDomainFormFromPolicy(props.apiPolicy, 'BATCH_LIMIT'))
  Object.assign(encryptionForm, createDomainFormFromPolicy(props.apiPolicy, 'ENCRYPTION_CAPABILITY'))
}

watch(
  () => props.apiPolicy,
  () => {
    syncFormsFromPolicy()
  },
  { immediate: true },
)

function resolveErrorMessage(fallbackKey: string): string {
  const key = templatesStore.lastErrorMessageKey
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
  if (!canEditPolicy.value) {
    return
  }
  if (!(await confirmSave('templates.policy.retention.confirmSaveAdGroups'))) {
    return
  }
  try {
    await templatesStore.saveApiPolicyDomain(props.templateId, 'AD_GROUP_AUTHORIZATION', adGroupsForm)
    ElMessage.success(t('apiPolicy.detail.saveSuccess'))
  } catch {
    ElMessage.error(resolveErrorMessage('templates.error.savePolicy'))
  }
}

async function saveDefaultRouteDomain() {
  if (!canEditPolicy.value || !props.apiPolicy) {
    return
  }
  try {
    const payload = buildUpsertPayloadForDomain(
      props.apiPolicy,
      'DEFAULT_ROUTE_TARGET',
      defaultRouteForm,
    )
    const preview = await templatesStore.previewApiPolicyImpact(props.templateId, payload)
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
    await templatesStore.saveApiPolicyDomain(
      props.templateId,
      'DEFAULT_ROUTE_TARGET',
      defaultRouteForm,
    )
    ElMessage.success(t('apiPolicy.detail.saveSuccess'))
  } catch {
    ElMessage.error(resolveErrorMessage('templates.error.savePolicy'))
  }
}

async function saveRetentionDomain() {
  if (!canEditPolicy.value) {
    return
  }
  if (!(await confirmSave('templates.policy.retention.confirmSaveRetention'))) {
    return
  }
  try {
    await templatesStore.saveInvocationRetentionDomain(props.templateId, { ...retentionForm })
    ElMessage.success(t('templates.policy.retention.saveSuccess'))
  } catch {
    ElMessage.error(resolveErrorMessage('templates.error.savePolicy'))
  }
}

async function saveAdvancedDomain(
  domain: 'OUTPUT_POLICY' | 'BATCH_LIMIT' | 'ENCRYPTION_CAPABILITY',
  form: OutputPolicyDomainForm | BatchLimitsDomainForm | EncryptionDomainForm,
) {
  if (!canEditPolicy.value) {
    return
  }
  if (!(await confirmSave('templates.policy.retention.confirmSaveAdvanced'))) {
    return
  }
  try {
    await templatesStore.saveApiPolicyDomain(props.templateId, domain, form as never)
    ElMessage.success(t('apiPolicy.detail.saveSuccess'))
  } catch {
    ElMessage.error(resolveErrorMessage('templates.error.savePolicy'))
  }
}

function revealCredentialSecret(externalId: string, secret: string) {
  credentialsPanelRef.value?.revealSecret(externalId, secret)
}

defineExpose({ revealCredentialSecret })
</script>

<template>
  <el-card shadow="never" class="section-card">
    <div class="section-header">
      <h2>{{ t('templates.policy.title') }}</h2>
      <span v-if="apiPolicy" class="policy-version">
        {{ t('templates.policy.policyVersion') }} v{{ apiPolicy.policyVersion }}
      </span>
    </div>

    <el-skeleton v-if="loadingPolicy" :rows="6" animated />
    <LoadErrorPanel
      v-else-if="policyLoadFailed"
      :message-key="policyLoadErrorKey ?? 'templates.error.loadPolicy'"
      @retry="emit('retryPolicyLoad')"
    />
    <template v-else>
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
        <el-form v-if="canEditPolicy" label-position="top" class="inline-form">
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
              {{
                apiPolicy?.defaultRouteReleaseVersion || t('apiPolicy.detail.summary.empty')
              }}
            </dd>
          </div>
        </dl>
        <el-form v-if="canEditPolicy" label-position="top" class="inline-form">
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
            <el-switch
              v-model="retentionForm.saveGeneratedDocuments"
              :disabled="!canEditPolicy"
            />
          </el-form-item>
          <el-form-item :label="t('templates.policy.retention.recordDays')">
            <el-select
              v-model="retentionForm.invocationRecordRetentionDays"
              :disabled="!canEditPolicy"
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
              :disabled="!canEditPolicy"
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
          <div v-if="canEditPolicy" class="action-row">
            <el-button type="primary" :loading="submitting" @click="saveRetentionDomain">
              {{ t('templates.policy.retention.save') }}
            </el-button>
          </div>
        </el-form>
      </section>
    </template>
  </el-card>

  <el-card v-if="showPolicyPanel" shadow="never" class="section-card">
    <h2>{{ t('templates.contract.title') }}</h2>
    <TemplateCallerContractPanel
      :template-id="templateId"
      :environment="selectedContractEnvironment ?? DEFAULT_ENVIRONMENT"
      @update:environment="selectedContractEnvironment = $event"
    />
  </el-card>

  <el-card shadow="never" class="section-card">
    <h2>{{ t('templates.policy.credentialsTitle') }}</h2>
    <CredentialsPanel
      ref="credentialsPanelRef"
      v-model:credential-column-filters="credentialColumnFilters"
      v-model:current-page="credentialsCurrentPage"
      :credentials="paginatedCredentials"
      :credential-status-filter-options="credentialStatusFilterOptions"
      :page-size="pageSize"
      :total-rows="totalCredentialRows"
      :submitting="submitting"
      :format-date-time="formatDateTime"
      :sort-by-created-at="sortCredentialsByCreatedAt"
      @create="emit('createCredential')"
      @rotate="(credentialId, externalId) => emit('rotateCredential', credentialId, externalId)"
      @revoke="(credentialId) => emit('revokeCredential', credentialId)"
    />
  </el-card>

  <TemplateRecentInvocationsPanel :template-id="templateId" />

  <el-card v-if="apiPolicy" shadow="never" class="section-card">
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
          <el-form v-if="canEditPolicy" label-position="top" class="inline-form">
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
          <el-form v-if="canEditPolicy" label-position="top" class="inline-form">
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
              <el-button
                :loading="submitting"
                @click="saveAdvancedDomain('BATCH_LIMIT', batchForm)"
              >
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
          <el-form v-if="canEditPolicy" label-position="top" class="inline-form">
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
.section-card {
  margin-bottom: var(--space-6);

  h2 {
    margin: 0 0 var(--space-4);
    font-size: var(--font-size-lg);
  }
}

.section-header {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-3);
  margin-bottom: var(--space-4);

  h2 {
    margin: 0;
  }
}

.policy-version {
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

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
</style>
