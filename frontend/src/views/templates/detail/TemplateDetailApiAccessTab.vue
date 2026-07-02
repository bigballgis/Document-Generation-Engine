<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { DEFAULT_ENVIRONMENT, type RuntimeEnvironment } from '@/config/environments'
import CredentialsPanel from '@/components/api/CredentialsPanel.vue'
import TemplateCallerContractPanel from '@/components/templates/TemplateCallerContractPanel.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
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
  openApiPolicyConsole: []
  createCredential: []
  rotateCredential: [credentialId: string, externalId: string]
  revokeCredential: [credentialId: string]
  retryPolicyLoad: []
}>()

const { t } = useI18n()

const credentialsPanelRef = ref<InstanceType<typeof CredentialsPanel> | null>(null)

const allowedAdGroupsText = computed(() => props.apiPolicy?.allowedAdGroups.join(', ') ?? '')

function revealCredentialSecret(externalId: string, secret: string) {
  credentialsPanelRef.value?.revealSecret(externalId, secret)
}

defineExpose({ revealCredentialSecret })
</script>

<template>
  <el-card shadow="never" class="section-card">
    <h2>{{ t('templates.policy.title') }}</h2>
    <el-skeleton v-if="loadingPolicy" :rows="4" animated />
    <LoadErrorPanel
      v-else-if="policyLoadFailed"
      :message-key="policyLoadErrorKey ?? 'templates.error.loadPolicy'"
      @retry="emit('retryPolicyLoad')"
    />
    <EmptyStatePanel
      v-else-if="!apiPolicy"
      title-key="templates.policy.notConfiguredTitle"
      description-key="templates.policy.notConfiguredDescription"
    />
    <template v-else>
      <dl class="policy-summary">
        <div>
          <dt>{{ t('templates.policy.policyVersion') }}</dt>
          <dd>v{{ apiPolicy.policyVersion }}</dd>
        </div>
        <div>
          <dt>{{ t('templates.policy.defaultRouteReleaseVersion') }}</dt>
          <dd>{{ apiPolicy.defaultRouteReleaseVersion }}</dd>
        </div>
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
            <span v-else>—</span>
          </dd>
        </div>
        <div>
          <dt>{{ t('templates.policy.outputFormats') }}</dt>
          <dd>{{ apiPolicy.outputFormats.join(', ') }}</dd>
        </div>
      </dl>
      <p class="policy-console-hint">{{ t('apiPolicy.detail.templateTabHint') }}</p>
      <div class="action-row">
        <el-button type="primary" @click="emit('openApiPolicyConsole')">
          {{ t('apiPolicy.detail.openConsole') }}
        </el-button>
      </div>
    </template>
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

  <el-card v-if="showPolicyPanel" shadow="never" class="section-card">
    <h2>{{ t('templates.contract.title') }}</h2>
    <TemplateCallerContractPanel
      :template-id="templateId"
      :environment="selectedContractEnvironment ?? DEFAULT_ENVIRONMENT"
      @update:environment="selectedContractEnvironment = $event"
    />
  </el-card>
</template>

<style scoped lang="scss">
.section-card {
  margin-bottom: 1.5rem;

  h2 {
    margin: 0 0 1rem;
    font-size: 1.125rem;
  }
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: center;
}

.policy-summary {
  display: grid;
  gap: 0.75rem;
  margin: 0 0 1rem;

  div {
    display: grid;
    grid-template-columns: 12rem minmax(0, 1fr);
    gap: 0.75rem;
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

.policy-console-hint {
  margin: 0 0 1rem;
  color: var(--text-muted);
  font-size: 0.875rem;
}
</style>
