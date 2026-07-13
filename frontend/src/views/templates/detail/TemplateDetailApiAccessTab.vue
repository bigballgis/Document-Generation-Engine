<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import RouteSummaryPanel from '@/components/templates/RouteSummaryPanel.vue'
import ApiPolicyDomainEditor from '@/components/api/ApiPolicyDomainEditor.vue'
import CredentialsPanel from '@/components/api/CredentialsPanel.vue'
import TemplateCallerContractPanel from '@/components/templates/TemplateCallerContractPanel.vue'
import TemplateInvocationsPanel from '@/components/templates/TemplateInvocationsPanel.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import { DEFAULT_ENVIRONMENT, type RuntimeEnvironment } from '@/config/environments'
import { useCapabilities } from '@/composables/useCapabilities'
import { API_POLICY_DOMAINS, type ApiPolicyDomain } from '@/types/apiPolicyDomain'
import type { ApiCredentialSummary, ApiPolicy } from '@/types/template'
import { hasConfiguredAdGroups } from '@/utils/apiAccessDiagnostics'

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

const { t } = useI18n()
const route = useRoute()
const { manageApiPolicy } = useCapabilities()

const credentialsPanelRef = ref<InstanceType<typeof CredentialsPanel> | null>(null)
const contractExpanded = ref<string[]>([])

const canEditPolicy = computed(() => manageApiPolicy.value && Boolean(props.apiPolicy))

const showAdGroupsNotConfiguredWarning = computed(
  () => Boolean(props.apiPolicy) && !hasConfiguredAdGroups(props.apiPolicy),
)

function resolveDomainAnchor(value: unknown): ApiPolicyDomain | null {
  if (typeof value !== 'string' || value.length === 0) {
    return null
  }
  if (API_POLICY_DOMAINS.includes(value as ApiPolicyDomain)) {
    return value as ApiPolicyDomain
  }
  return null
}

const domainAnchor = computed((): ApiPolicyDomain | null => {
  const hashMatch = /^#domain=(.+)$/.exec(route.hash)
  if (hashMatch) {
    const fromHash = resolveDomainAnchor(decodeURIComponent(hashMatch[1]))
    if (fromHash) {
      return fromHash
    }
  }
  return resolveDomainAnchor(route.query.domain)
})

function revealCredentialSecret(externalId: string, secret: string) {
  credentialsPanelRef.value?.revealSecret(externalId, secret)
}

defineExpose({ revealCredentialSecret })
</script>

<template>
  <div class="api-access-layout">
    <RouteSummaryPanel :template-id="templateId" />

    <el-alert
      v-if="showAdGroupsNotConfiguredWarning"
      class="runtime-callable-warning"
      type="warning"
      :title="t('templates.policy.runtimeCallable.warningTitle')"
      :description="t('templates.policy.runtimeCallable.warningDescription')"
      show-icon
      :closable="false"
      data-testid="ad-groups-not-configured-warning"
    />
    <el-alert
      v-if="showAdGroupsNotConfiguredWarning"
      class="runtime-callable-hint"
      type="info"
      :title="t('templates.policy.runtimeCallable.publishedVsCallableHint')"
      show-icon
      :closable="false"
      data-testid="published-vs-callable-hint"
    />

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
      <ApiPolicyDomainEditor
        v-else
        variant="tab-sections"
        :template-id="templateId"
        :api-policy="apiPolicy"
        :can-edit="canEditPolicy"
        :submitting="submitting"
        :initial-domain-anchor="domainAnchor"
      />
    </el-card>

    <el-card v-if="showPolicyPanel" shadow="never" class="section-card">
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

    <TemplateInvocationsPanel :template-id="templateId" />

    <el-card shadow="never" class="section-card">
      <el-collapse v-model="contractExpanded" class="contract-collapse">
        <el-collapse-item name="contract" :title="t('templates.contract.title')">
          <p class="contract-hint">{{ t('templates.contract.description') }}</p>
          <TemplateCallerContractPanel
            :template-id="templateId"
            :environment="selectedContractEnvironment ?? DEFAULT_ENVIRONMENT"
            @update:environment="selectedContractEnvironment = $event"
          />
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.api-access-layout {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.runtime-callable-warning,
.runtime-callable-hint {
  margin: 0;
}

.section-card {
  margin-bottom: 0;

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

.contract-collapse {
  border: none;

  :deep(.el-collapse-item__header) {
    font-size: var(--font-size-lg);
    font-weight: 600;
    color: var(--text-primary);
  }

  :deep(.el-collapse-item__wrap) {
    border-bottom: none;
  }
}

.contract-hint {
  margin: 0 0 var(--space-4);
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}
</style>
