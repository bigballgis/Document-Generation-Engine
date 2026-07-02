<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import ApiPolicyImpactPreviewPanel from '@/components/api/ApiPolicyImpactPreviewPanel.vue'
import CredentialsPanel from '@/components/api/CredentialsPanel.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import { useConfirmAction } from '@/composables/useConfirmAction'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { useDataTableFilters } from '@/composables/useDataTableFilters'
import { useCatalogPagination } from '@/composables/useCatalogPagination'
import { useCredentialStatusFilterOptions } from '@/composables/useTableFilterOptions'
import { CLIENT_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import { apiPolicyDetailPath } from '@/routing/routeKeys'
import { useTemplatesStore } from '@/stores/templates'
import {
  API_POLICY_DOMAINS,
  buildUpsertPayloadForDomain,
  createDomainFormFromPolicy,
  type AdGroupsDomainForm,
  type ApiPolicyDomain,
  type BatchLimitsDomainForm,
  type DefaultRouteDomainForm,
  type EncryptionDomainForm,
  type OutputPolicyDomainForm,
} from '@/types/apiPolicyDomain'
import type { ApiCredentialSummary, ApiPolicyImpactPreview } from '@/types/template'

const { t, te } = useI18n()
const { formatDateTime } = useLocaleFormatters()
const route = useRoute()
const router = useRouter()
const templatesStore = useTemplatesStore()
const { confirmAction } = useConfirmAction()

const policyOutputFormatOptions = ['DOCX', 'PDF']
const policyOutputModeOptions = ['SYNC_STREAM', 'ASYNC_CALLBACK', 'INLINE']

const loadFailed = ref(false)
const previewLoading = ref(false)
const previewError = ref('')
const impactPreview = ref<ApiPolicyImpactPreview | null>(null)
const credentialsPanelRef = ref<InstanceType<typeof CredentialsPanel> | null>(null)
const credentialsCurrentPage = ref(1)

const templateId = computed(() => String(route.params.templateId ?? ''))

function resolveDomain(value: unknown): ApiPolicyDomain {
  const candidate = typeof value === 'string' ? value : ''
  if (API_POLICY_DOMAINS.includes(candidate as ApiPolicyDomain)) {
    return candidate as ApiPolicyDomain
  }
  return 'AD_GROUP_AUTHORIZATION'
}

const activeDomain = ref<ApiPolicyDomain>(resolveDomain(route.query.domain))

const adGroupsForm = reactive<AdGroupsDomainForm>({ allowedAdGroups: [] })
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
const defaultRouteForm = reactive<DefaultRouteDomainForm>({ defaultRouteReleaseVersion: '' })

function domainCandidate(domain: ApiPolicyDomain) {
  switch (domain) {
    case 'AD_GROUP_AUTHORIZATION':
      return adGroupsForm
    case 'OUTPUT_POLICY':
      return outputForm
    case 'BATCH_LIMIT':
      return batchForm
    case 'ENCRYPTION_CAPABILITY':
      return encryptionForm
    case 'DEFAULT_ROUTE_TARGET':
      return defaultRouteForm
    default:
      return adGroupsForm
  }
}

const template = computed(() => templatesStore.selectedTemplate)
const policy = computed(() => templatesStore.apiPolicy)

const errorMessage = computed(() => {
  const key = templatesStore.lastErrorMessageKey
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('templates.error.loadDetail')
})

const saveDisabled = computed(
  () =>
    templatesStore.submitting ||
    previewLoading.value ||
    !policy.value ||
    impactPreview.value?.blocking === true,
)

const credentialsSource = computed(() => templatesStore.credentials)
const { filters: credentialColumnFilters, filteredRows: filteredCredentials } = useDataTableFilters(
  credentialsSource,
  [
    { key: 'externalId', getValue: (row) => row.externalId },
    { key: 'status', getValue: (row) => row.status, matchMode: 'exact' },
    { key: 'createdAt', getValue: (row) => formatDateTime(row.createdAt) },
  ],
)
const { paginatedRows: paginatedCredentials, totalRows: totalCredentialRows } = useCatalogPagination(
  filteredCredentials,
  credentialsCurrentPage,
  CLIENT_TABLE_PAGE_SIZE,
)
const sortCredentialsByCreatedAt = (a: ApiCredentialSummary, b: ApiCredentialSummary) =>
  a.createdAt.localeCompare(b.createdAt)
const credentialStatusFilterOptions = useCredentialStatusFilterOptions()

function syncFormsFromPolicy() {
  if (!policy.value) {
    return
  }
  Object.assign(adGroupsForm, createDomainFormFromPolicy(policy.value, 'AD_GROUP_AUTHORIZATION'))
  Object.assign(outputForm, createDomainFormFromPolicy(policy.value, 'OUTPUT_POLICY'))
  Object.assign(batchForm, createDomainFormFromPolicy(policy.value, 'BATCH_LIMIT'))
  Object.assign(encryptionForm, createDomainFormFromPolicy(policy.value, 'ENCRYPTION_CAPABILITY'))
  Object.assign(defaultRouteForm, createDomainFormFromPolicy(policy.value, 'DEFAULT_ROUTE_TARGET'))
}

function selectDomain(domain: ApiPolicyDomain) {
  activeDomain.value = domain
  impactPreview.value = null
  previewError.value = ''
  router.replace(apiPolicyDetailPath(templateId.value, domain))
}

async function reloadPage() {
  loadFailed.value = false
  try {
    await templatesStore.fetchTemplate(templateId.value)
    await Promise.all([
      templatesStore.fetchApiPolicy(templateId.value),
      templatesStore.fetchCredentials(templateId.value),
    ])
    syncFormsFromPolicy()
  } catch {
    loadFailed.value = true
  }
}

onMounted(async () => {
  await reloadPage()
})

watch(
  () => policy.value,
  () => {
    syncFormsFromPolicy()
  },
)

watch(
  () => route.query.domain,
  (value) => {
    activeDomain.value = resolveDomain(value)
    impactPreview.value = null
    previewError.value = ''
  },
)

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
    activeDomain.value,
  ],
  () => {
    impactPreview.value = null
    previewError.value = ''
  },
)

async function runImpactPreview(): Promise<ApiPolicyImpactPreview | null> {
  if (!policy.value) {
    return null
  }
  previewLoading.value = true
  previewError.value = ''
  try {
    const payload = buildUpsertPayloadForDomain(
      policy.value,
      activeDomain.value,
      domainCandidate(activeDomain.value),
    )
    impactPreview.value = await templatesStore.previewApiPolicyImpact(templateId.value, payload)
    return impactPreview.value
  } catch {
    previewError.value = errorMessage.value || t('templates.error.previewPolicyImpact')
    impactPreview.value = null
    return null
  } finally {
    previewLoading.value = false
  }
}

async function handlePreviewClick() {
  await runImpactPreview()
}

async function handleSaveDomain() {
  const preview = (await runImpactPreview()) ?? impactPreview.value
  if (!preview) {
    return
  }
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
  } else {
    try {
      await ElMessageBox.confirm(
        t('apiPolicy.detail.impact.confirmSave'),
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
  }

  try {
    await templatesStore.saveApiPolicyDomain(
      templateId.value,
      activeDomain.value,
      domainCandidate(activeDomain.value),
      true,
    )
    syncFormsFromPolicy()
    impactPreview.value = null
    ElMessage.success(t('apiPolicy.detail.saveSuccess'))
  } catch {
    ElMessage.error(errorMessage.value || t('templates.error.savePolicy'))
  }
}

function revealCredentialSecret(externalId: string, secret: string) {
  credentialsPanelRef.value?.revealSecret(externalId, secret)
}

async function handleCreateCredential() {
  try {
    const created = await templatesStore.createCredential(templateId.value)
    revealCredentialSecret(created.externalId, created.secret)
    ElMessage.success(t('templates.policy.createCredentialSuccess'))
  } catch {
    ElMessage.error(errorMessage.value || t('templates.error.createCredential'))
  }
}

async function handleRotateCredential(credentialId: string, externalId: string) {
  const confirmed = await confirmAction({
    titleKey: 'templates.policy.confirmRotateTitle',
    messageKey: 'templates.policy.confirmRotateMessage',
    type: 'warning',
  })
  if (!confirmed) {
    return
  }
  try {
    const rotated = await templatesStore.rotateCredential(templateId.value, credentialId)
    revealCredentialSecret(externalId, rotated.secret)
    ElMessage.success(t('templates.policy.rotateCredentialSuccess'))
  } catch {
    ElMessage.error(errorMessage.value || t('templates.error.rotateCredential'))
  }
}

async function handleRevokeCredential(credentialId: string) {
  const confirmed = await confirmAction({
    titleKey: 'templates.policy.confirmRevokeTitle',
    messageKey: 'templates.policy.confirmRevokeMessage',
    type: 'warning',
  })
  if (!confirmed) {
    return
  }
  try {
    await templatesStore.revokeCredential(templateId.value, credentialId)
    ElMessage.success(t('templates.policy.revokeCredentialSuccess'))
  } catch {
    ElMessage.error(errorMessage.value || t('templates.error.revokeCredential'))
  }
}

function currentSummary(domain: ApiPolicyDomain): string {
  if (!policy.value) {
    return ''
  }
  switch (domain) {
    case 'AD_GROUP_AUTHORIZATION':
      return policy.value.allowedAdGroups.join(', ') || t('apiPolicy.detail.summary.empty')
    case 'OUTPUT_POLICY':
      return `${policy.value.outputFormats.join(', ')} / ${policy.value.outputModes.join(', ')}`
    case 'BATCH_LIMIT':
      return policy.value.batchEnabled
        ? t('apiPolicy.detail.summary.batchEnabled', {
            sync: policy.value.batchSyncMaxItems ?? policy.value.maxBatchSize,
            async: policy.value.batchAsyncMaxItems ?? 10_000,
          })
        : t('apiPolicy.detail.summary.batchDisabled')
    case 'ENCRYPTION_CAPABILITY':
      return [
        policy.value.docxEncryptionEnabled ? 'DOCX' : null,
        policy.value.pdfEncryptionEnabled ? 'PDF' : null,
      ]
        .filter(Boolean)
        .join(', ') || t('apiPolicy.detail.summary.encryptionNone')
    case 'DEFAULT_ROUTE_TARGET':
      return policy.value.defaultRouteReleaseVersion || t('apiPolicy.detail.summary.empty')
    default:
      return ''
  }
}
</script>

<template>
  <AppPageLayout>
    <PageHeader
      show-back
      :back-label="t('apiPolicy.detail.backToList')"
      :title="template?.name ?? t('apiPolicy.detail.title')"
      :description="t('apiPolicy.detail.description')"
      @back="router.push('/api/policies')"
    >
      <template v-if="policy" #meta>
        <span class="meta-label">{{ t('apiPolicy.detail.policyVersion') }}</span>
        <el-tag type="info" effect="plain">v{{ policy.policyVersion }}</el-tag>
      </template>
    </PageHeader>

    <LoadErrorPanel
      v-if="loadFailed"
      message-key="common.loadError"
      @retry="reloadPage"
    />

    <el-skeleton v-else-if="templatesStore.loadingPolicy" :rows="8" animated />

    <template v-else-if="policy">
      <div class="domain-layout">
        <nav class="domain-nav" :aria-label="t('apiPolicy.detail.domainNav')">
          <el-menu :default-active="activeDomain" @select="(key: string) => selectDomain(key as ApiPolicyDomain)">
            <el-menu-item v-for="domain in API_POLICY_DOMAINS" :key="domain" :index="domain">
              {{ t(`apiPolicy.detail.domains.${domain}`) }}
            </el-menu-item>
          </el-menu>
        </nav>

        <div class="domain-content">
          <el-card shadow="never" class="section-card">
            <p class="field-hint domain-hint">{{ t(`apiPolicy.detail.hints.${activeDomain}`) }}</p>

            <div class="current-summary">
              <span class="summary-label">{{ t('apiPolicy.detail.currentSummary') }}</span>
              <span>{{ currentSummary(activeDomain) }}</span>
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
                  <AppSearchSelect
                    v-model="outputForm.outputFormats"
                    multiple
                    filterable
                    allow-create
                  >
                    <el-option
                      v-for="format in policyOutputFormatOptions"
                      :key="format"
                      :label="format"
                      :value="format"
                    />
                  </AppSearchSelect>
                </el-form-item>
                <el-form-item :label="t('templates.policy.outputModes')">
                  <AppSearchSelect
                    v-model="outputForm.outputModes"
                    multiple
                    filterable
                    allow-create
                  >
                    <el-option
                      v-for="mode in policyOutputModeOptions"
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
                  <el-input-number
                    v-model="batchForm.syncMaxItems"
                    :min="1"
                    :max="1000"
                  />
                </el-form-item>
                <el-form-item :label="t('apiPolicy.detail.fields.asyncMaxItems')">
                  <el-input-number
                    v-model="batchForm.asyncMaxItems"
                    :min="1"
                    :max="100000"
                  />
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

            <ApiPolicyImpactPreviewPanel
              :preview="impactPreview"
              :loading="previewLoading"
              :error-message="previewError"
            />

            <div class="action-row">
              <el-button :loading="previewLoading" @click="handlePreviewClick">
                {{ t('apiPolicy.detail.runPreview') }}
              </el-button>
              <el-button
                type="primary"
                :loading="templatesStore.submitting"
                :disabled="saveDisabled"
                @click="handleSaveDomain"
              >
                {{ t('apiPolicy.detail.saveDomain') }}
              </el-button>
            </div>
          </el-card>

          <el-card shadow="never" class="section-card">
            <h2>{{ t('templates.policy.credentialsTitle') }}</h2>
            <CredentialsPanel
              ref="credentialsPanelRef"
              v-model:credential-column-filters="credentialColumnFilters"
              v-model:current-page="credentialsCurrentPage"
              :credentials="paginatedCredentials"
              :credential-status-filter-options="credentialStatusFilterOptions"
              :page-size="CLIENT_TABLE_PAGE_SIZE"
              :total-rows="totalCredentialRows"
              :submitting="templatesStore.submitting"
              :format-date-time="formatDateTime"
              :sort-by-created-at="sortCredentialsByCreatedAt"
              @create="handleCreateCredential"
              @rotate="handleRotateCredential"
              @revoke="handleRevokeCredential"
            />
          </el-card>
        </div>
      </div>
    </template>
  </AppPageLayout>
</template>

<style scoped lang="scss">
.meta-label {
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

.domain-layout {
  display: grid;
  grid-template-columns: 15rem minmax(0, 1fr);
  gap: 1.5rem;
  align-items: start;
}

.domain-nav {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--surface-card);

  :deep(.el-menu) {
    border-right: none;
  }
}

.section-card {
  margin-bottom: 1.5rem;
}

.domain-hint {
  margin-top: 0;
}

.field-hint {
  margin: 0 0 1rem;
  color: var(--text-muted);
  font-size: 0.875rem;
}

.current-summary {
  display: flex;
  gap: 0.75rem;
  margin-bottom: 1rem;
  padding: 0.75rem 1rem;
  border-radius: var(--radius-md);
  background: var(--surface-muted);
  font-size: 0.875rem;
}

.summary-label {
  color: var(--text-muted);
  min-width: 7rem;
}

.domain-form {
  max-width: 40rem;
  margin-bottom: 1rem;
}

.action-row {
  display: flex;
  gap: 0.75rem;
  margin-top: 1rem;
}
</style>
