<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import ApiPolicyDomainEditor from '@/components/api/ApiPolicyDomainEditor.vue'
import CredentialsPanel from '@/components/api/CredentialsPanel.vue'
import TemplateCallerContractPanel from '@/components/templates/TemplateCallerContractPanel.vue'
import TemplateRecentInvocationsPanel from '@/components/templates/TemplateRecentInvocationsPanel.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import { DEFAULT_ENVIRONMENT, type RuntimeEnvironment } from '@/config/environments'
import { useCapabilities } from '@/composables/useCapabilities'
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

const { t } = useI18n()
const { manageApiPolicy } = useCapabilities()

const credentialsPanelRef = ref<InstanceType<typeof CredentialsPanel> | null>(null)

const canEditPolicy = computed(() => manageApiPolicy.value && Boolean(props.apiPolicy))

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
    <ApiPolicyDomainEditor
      v-else
      variant="tab-sections"
      :template-id="templateId"
      :api-policy="apiPolicy"
      :can-edit="canEditPolicy"
      :submitting="submitting"
    />
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
</style>
