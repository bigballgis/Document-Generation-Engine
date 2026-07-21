<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import { DEFAULT_ENVIRONMENT, type RuntimeEnvironment } from '@/config/environments'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { CLIENT_TABLE_PAGE_SIZE } from '@/constants/tablePagination'
import { ROUTE_PATH_BY_KEY, ROUTE_KEYS, templatePackageHubPath } from '@/routing/routeKeys'
import { useTemplatesStore } from '@/stores/templates'
import { API_POLICY_DOMAINS } from '@/types/apiPolicyDomain'
import TemplateDetailApiAccessTab from '@/views/templates/detail/TemplateDetailApiAccessTab.vue'
import { useTemplatePolicyCredentials } from '@/views/templates/useTemplatePolicyCredentials'

const KNOWN_PANELS = new Set(['domain', 'credentials', 'routes', 'contract', 'invocations', ...API_POLICY_DOMAINS])

const { t, te } = useI18n()
const { formatDateTime } = useLocaleFormatters()
const route = useRoute()
const router = useRouter()
const templatesStore = useTemplatesStore()

const loadFailed = ref(false)
const selectedContractEnvironment = ref<RuntimeEnvironment>(DEFAULT_ENVIRONMENT)

const templateId = computed(() => String(route.params.templateId ?? ''))
const releaseVersionContext = computed(() => {
  const value = route.query.releaseVersion
  return typeof value === 'string' && value.length > 0 ? value : null
})

const panelQuery = computed(() => {
  const value = route.query.panel
  return typeof value === 'string' && value.length > 0 ? value : null
})

const unknownPanel = computed(() => {
  if (!panelQuery.value) {
    return null
  }
  if (KNOWN_PANELS.has(panelQuery.value)) {
    return null
  }
  return panelQuery.value
})

const template = computed(() => {
  const selected = templatesStore.selectedTemplate
  if (!selected || selected.id !== templateId.value) {
    return null
  }
  return selected
})

const errorMessage = computed(() => {
  const key = templatesStore.lastErrorMessageKey
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('templates.error.loadDetail')
})

const {
  showPolicyPanel,
  policyLoadFailed,
  apiPolicy,
  loadingPolicy,
  policySubmitting,
  policyLoadErrorKey,
  credentialColumnFilters,
  credentialsCurrentPage,
  paginatedCredentials,
  credentialStatusFilterOptions,
  totalCredentialRows,
  sortCredentialsByCreatedAt,
  loadPolicyData,
  handleCreateCredential,
  handleRotateCredential,
  handleRevokeCredential,
} = useTemplatePolicyCredentials({
  templateId,
  template,
  errorMessage,
})

async function loadPage() {
  loadFailed.value = false
  try {
    await templatesStore.fetchTemplate(templateId.value)
    await loadPolicyData()
  } catch {
    loadFailed.value = true
  }
}

function backToHub() {
  void router.push(templatePackageHubPath(templateId.value))
}

function backToApiHome() {
  void router.push(ROUTE_PATH_BY_KEY[ROUTE_KEYS.apiPolicyManagement])
}

function openPackageInvocations() {
  void router.push({
    path: '/api/invocations',
    query: { templateId: templateId.value },
  })
}

onMounted(() => {
  void loadPage()
})

onUnmounted(() => {
  templatesStore.clearSelected()
})

watch(templateId, () => {
  void loadPage()
})
</script>

<template>
  <AppPageLayout layout-variant="fluid">
    <PageHeader
      show-back
      :back-label="t('apiPolicy.packageSettings.backToHub')"
      :title="
        template
          ? t('apiPolicy.packageSettings.title', { name: template.name })
          : t('apiPolicy.packageSettings.loadingTitle')
      "
      :description="t('apiPolicy.packageSettings.description')"
      @back="backToHub"
    >
      <template #actions>
        <el-button data-testid="api-package-settings-open-invocations" @click="openPackageInvocations">
          {{ t('apiPolicy.packageSettings.openInvocations') }}
        </el-button>
        <el-button @click="backToApiHome">
          {{ t('apiPolicy.packageSettings.backToExternalServices') }}
        </el-button>
      </template>
    </PageHeader>

    <p
      v-if="releaseVersionContext"
      class="version-context"
      data-testid="api-package-settings-release-context"
    >
      {{ t('apiPolicy.packageSettings.releaseContext', { releaseVersion: releaseVersionContext }) }}
    </p>

    <el-alert
      v-if="unknownPanel"
      class="unknown-panel"
      type="info"
      :closable="false"
      show-icon
      data-testid="api-package-settings-unknown-panel"
      :title="t('apiPolicy.packageSettings.unknownPanelNotice', { panel: unknownPanel })"
    />

    <LoadErrorPanel
      v-if="loadFailed"
      :message-key="templatesStore.lastErrorMessageKey ?? 'templates.error.loadDetail'"
      @retry="loadPage"
    />

    <el-skeleton v-else-if="templatesStore.loadingDetail" :rows="6" animated />

    <EmptyStatePanel
      v-else-if="!template"
      title-key="templates.empty.notFoundTitle"
      description-key="templates.empty.notFoundDescription"
    />

    <TemplateDetailApiAccessTab
      v-else
      v-model:credential-column-filters="credentialColumnFilters"
      v-model:credentials-current-page="credentialsCurrentPage"
      v-model:selected-contract-environment="selectedContractEnvironment"
      :template-id="templateId"
      :show-policy-panel="showPolicyPanel"
      :loading-policy="loadingPolicy"
      :api-policy="apiPolicy"
      :policy-load-failed="policyLoadFailed"
      :policy-load-error-key="policyLoadErrorKey"
      :paginated-credentials="paginatedCredentials"
      :credential-status-filter-options="credentialStatusFilterOptions"
      :page-size="CLIENT_TABLE_PAGE_SIZE"
      :total-credential-rows="totalCredentialRows"
      :submitting="policySubmitting"
      :format-date-time="formatDateTime"
      :sort-credentials-by-created-at="sortCredentialsByCreatedAt"
      data-testid="api-package-settings-panel"
      @create-credential="handleCreateCredential"
      @rotate-credential="(credentialId, externalId) => handleRotateCredential(credentialId, externalId)"
      @revoke-credential="handleRevokeCredential"
      @retry-policy-load="loadPolicyData"
    />
  </AppPageLayout>
</template>

<style scoped lang="scss">
.version-context {
  margin: calc(-1 * var(--space-4)) 0 var(--space-4);
  color: var(--text-secondary);
  font-size: var(--font-size-sm);
}

.unknown-panel {
  margin-bottom: var(--space-4);
}
</style>
