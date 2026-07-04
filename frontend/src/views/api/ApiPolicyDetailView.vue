<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import ApiPolicyDomainEditor from '@/components/api/ApiPolicyDomainEditor.vue'
import ApiPolicyImpactPreviewPanel from '@/components/api/ApiPolicyImpactPreviewPanel.vue'
import CredentialsPanel from '@/components/api/CredentialsPanel.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import { useConfirmAction } from '@/composables/useConfirmAction'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { useDataTableFilters } from '@/composables/useDataTableFilters'
import { useCatalogPagination } from '@/composables/useCatalogPagination'
import { useCredentialStatusFilterOptions } from '@/composables/useTableFilterOptions'
import { CLIENT_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import { apiPolicyDetailPath } from '@/routing/routeKeys'
import { useApiPolicyStore } from '@/stores/apiPolicy'
import { useTemplatesStore } from '@/stores/templates'
import {
  API_POLICY_DOMAINS,
  buildUpsertPayloadForDomain,
  type ApiPolicyDomain,
} from '@/types/apiPolicyDomain'
import type { ApiCredentialSummary, ApiPolicyImpactPreview } from '@/types/template'

const { t, te } = useI18n()
const { formatDateTime } = useLocaleFormatters()
const route = useRoute()
const router = useRouter()
const templatesStore = useTemplatesStore()
const apiPolicyStore = useApiPolicyStore()
const { confirmAction } = useConfirmAction()

const loadFailed = ref(false)
const previewLoading = ref(false)
const previewError = ref('')
const impactPreview = ref<ApiPolicyImpactPreview | null>(null)
const credentialsPanelRef = ref<InstanceType<typeof CredentialsPanel> | null>(null)
const domainEditorRef = ref<InstanceType<typeof ApiPolicyDomainEditor> | null>(null)
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

const template = computed(() => templatesStore.selectedTemplate)
const policy = computed(() => apiPolicyStore.apiPolicy)

const errorMessage = computed(() => {
  const key = apiPolicyStore.lastErrorMessageKey
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('templates.error.loadDetail')
})

const saveDisabled = computed(
  () =>
    apiPolicyStore.submitting ||
    previewLoading.value ||
    !policy.value ||
    impactPreview.value?.blocking === true,
)

const credentialsSource = computed(() => apiPolicyStore.credentials)
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

function selectDomain(domain: ApiPolicyDomain) {
  activeDomain.value = domain
  impactPreview.value = null
  previewError.value = ''
  router.replace(apiPolicyDetailPath(templateId.value, domain))
}

async function reloadPage() {
  loadFailed.value = false
  apiPolicyStore.setActiveTemplate(templateId.value)
  try {
    await templatesStore.fetchTemplate(templateId.value)
    await Promise.all([
      apiPolicyStore.fetchPolicy(templateId.value),
      apiPolicyStore.fetchCredentials(templateId.value),
    ])
    domainEditorRef.value?.syncFormsFromPolicy()
  } catch {
    loadFailed.value = true
  }
}

onMounted(async () => {
  await reloadPage()
})

watch(
  () => route.query.domain,
  (value) => {
    activeDomain.value = resolveDomain(value)
    impactPreview.value = null
    previewError.value = ''
  },
)

watch(activeDomain, () => {
  impactPreview.value = null
  previewError.value = ''
})

function handleFormEdited() {
  impactPreview.value = null
  previewError.value = ''
}

async function runImpactPreview(): Promise<ApiPolicyImpactPreview | null> {
  if (!policy.value || !domainEditorRef.value) {
    return null
  }
  previewLoading.value = true
  previewError.value = ''
  try {
    const payload = buildUpsertPayloadForDomain(
      policy.value,
      activeDomain.value,
      domainEditorRef.value.domainCandidate(activeDomain.value),
    )
    impactPreview.value = await apiPolicyStore.previewImpact(templateId.value, payload)
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
  if (!domainEditorRef.value) {
    return
  }
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
    await apiPolicyStore.savePolicyDomain(
      templateId.value,
      activeDomain.value,
      domainEditorRef.value.domainCandidate(activeDomain.value),
      true,
    )
    domainEditorRef.value.syncFormsFromPolicy()
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
    const created = await apiPolicyStore.createCredential(templateId.value)
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
    const rotated = await apiPolicyStore.rotateCredential(templateId.value, credentialId)
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
    await apiPolicyStore.revokeCredential(templateId.value, credentialId)
    ElMessage.success(t('templates.policy.revokeCredentialSuccess'))
  } catch {
    ElMessage.error(errorMessage.value || t('templates.error.revokeCredential'))
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

    <el-skeleton v-else-if="apiPolicyStore.loadingPolicy" :rows="8" animated />

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
            <ApiPolicyDomainEditor
              ref="domainEditorRef"
              variant="domain-console"
              :template-id="templateId"
              :api-policy="policy"
              :active-domain="activeDomain"
              :can-edit="true"
              :submitting="apiPolicyStore.submitting"
              @form-edited="handleFormEdited"
            >
              <template #console-actions>
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
                    :loading="apiPolicyStore.submitting"
                    :disabled="saveDisabled"
                    @click="handleSaveDomain"
                  >
                    {{ t('apiPolicy.detail.saveDomain') }}
                  </el-button>
                </div>
              </template>
            </ApiPolicyDomainEditor>
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
              :submitting="apiPolicyStore.submitting"
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
  gap: var(--space-6);
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
  margin-bottom: var(--space-6);

  h2 {
    margin: 0 0 var(--space-4);
    font-size: var(--font-size-lg);
  }
}

.action-row {
  display: flex;
  gap: var(--space-3);
  margin-top: var(--space-4);
}
</style>
