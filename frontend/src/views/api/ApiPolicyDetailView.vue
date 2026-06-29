<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import ApiPolicyImpactPreviewPanel from '@/components/api/ApiPolicyImpactPreviewPanel.vue'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import { useConfirmAction } from '@/composables/useConfirmAction'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { rowSortMethod, useDataTableFilters } from '@/composables/useDataTableFilters'
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
const credentialSecretDialogVisible = ref(false)
const credentialSecretValue = ref('')
const credentialSecretExternalId = ref('')
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
const sortCredentialsByCreatedAt = rowSortMethod<ApiCredentialSummary>((row) => row.createdAt)
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

function openCredentialSecretDialog(externalId: string, secret: string) {
  credentialSecretExternalId.value = externalId
  credentialSecretValue.value = secret
  credentialSecretDialogVisible.value = true
}

async function handleCreateCredential() {
  try {
    const created = await templatesStore.createCredential(templateId.value)
    openCredentialSecretDialog(created.externalId, created.secret)
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
    openCredentialSecretDialog(externalId, rotated.secret)
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
    <header class="page-header">
      <div>
        <el-button link type="primary" @click="router.push('/api/policies')">
          {{ t('apiPolicy.detail.backToList') }}
        </el-button>
        <h1>{{ template?.name ?? t('apiPolicy.detail.title') }}</h1>
        <p>{{ t('apiPolicy.detail.description') }}</p>
      </div>
      <div v-if="policy" class="page-header__meta">
        <span class="meta-label">{{ t('apiPolicy.detail.policyVersion') }}</span>
        <el-tag type="info" effect="plain">v{{ policy.policyVersion }}</el-tag>
      </div>
    </header>

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
            <h2>{{ t(`apiPolicy.detail.domains.${activeDomain}`) }}</h2>
            <p class="field-hint">{{ t(`apiPolicy.detail.hints.${activeDomain}`) }}</p>

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
            <div class="action-row action-row--compact">
              <el-button :loading="templatesStore.submitting" @click="handleCreateCredential">
                {{ t('templates.policy.createCredential') }}
              </el-button>
            </div>
            <AppDataTable :data="paginatedCredentials" empty-text="">
              <template #empty>
                <el-empty :description="t('templates.policy.noCredentials')" />
              </template>
              <el-table-column prop="externalId" sortable>
                <template #header>
                  <TableColumnHeader
                    :label="t('templates.policy.credentialExternalId')"
                    v-model="credentialColumnFilters.externalId"
                  />
                </template>
              </el-table-column>
              <el-table-column prop="status" sortable>
                <template #header>
                  <TableColumnHeader
                    :label="t('templates.policy.credentialStatus')"
                    v-model="credentialColumnFilters.status"
                    filter-type="select"
                    :options="credentialStatusFilterOptions"
                  />
                </template>
              </el-table-column>
              <el-table-column sortable :sort-method="sortCredentialsByCreatedAt" min-width="180">
                <template #header>
                  <TableColumnHeader
                    :label="t('templates.policy.credentialCreatedAt')"
                    v-model="credentialColumnFilters.createdAt"
                  />
                </template>
                <template #default="{ row }">
                  {{ formatDateTime(row.createdAt) }}
                </template>
              </el-table-column>
              <el-table-column :label="t('templates.policy.credentialActions')" min-width="200">
                <template #default="{ row }">
                  <el-button
                    v-if="row.status === 'ACTIVE'"
                    link
                    type="primary"
                    @click="handleRotateCredential(row.credentialId, row.externalId)"
                  >
                    {{ t('templates.policy.rotateCredential') }}
                  </el-button>
                  <el-button
                    v-if="row.status === 'ACTIVE'"
                    link
                    type="danger"
                    @click="handleRevokeCredential(row.credentialId)"
                  >
                    {{ t('templates.policy.revokeCredential') }}
                  </el-button>
                </template>
              </el-table-column>
            </AppDataTable>
            <AppTablePagination
              v-model:current-page="credentialsCurrentPage"
              :page-size="CLIENT_TABLE_PAGE_SIZE"
              :total="totalCredentialRows"
            />
          </el-card>
        </div>
      </div>
    </template>

    <el-dialog
      v-model="credentialSecretDialogVisible"
      :title="t('templates.policy.credentialSecretDialogTitle')"
      width="32rem"
    >
      <p>{{ t('templates.policy.credentialSecretHint') }}</p>
      <p>
        <strong>{{ credentialSecretExternalId }}</strong>
      </p>
      <el-input :model-value="credentialSecretValue" readonly />
    </el-dialog>
  </AppPageLayout>
</template>

<style scoped lang="scss">
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1.5rem;
  margin-bottom: 1.5rem;

  h1 {
    margin: 0.25rem 0;
    font-size: 1.75rem;
  }

  p {
    margin: 0;
    color: var(--text-muted);
  }
}

.page-header__meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.meta-label {
  color: var(--text-muted);
  font-size: 0.875rem;
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
  background: var(--surface-color, #fff);

  :deep(.el-menu) {
    border-right: none;
  }
}

.section-card {
  margin-bottom: 1.5rem;

  h2 {
    margin: 0 0 0.5rem;
    font-size: 1.25rem;
  }
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
  background: var(--surface-muted, #fafbfc);
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

.action-row--compact {
  margin-top: 0;
  margin-bottom: 1rem;
}
</style>
