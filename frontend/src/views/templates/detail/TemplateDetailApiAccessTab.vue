<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { DEFAULT_ENVIRONMENT, type RuntimeEnvironment } from '@/config/environments'
import TemplateCallerContractPanel from '@/components/templates/TemplateCallerContractPanel.vue'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import type { ApiCredentialSummary, ApiPolicy } from '@/types/template'

defineProps<{
  templateId: string
  showPolicyPanel: boolean
  loadingPolicy: boolean
  apiPolicy: ApiPolicy | null
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
}>()

const { t } = useI18n()
</script>

<template>
  <el-card shadow="never" class="section-card">
    <h2>{{ t('templates.policy.title') }}</h2>
    <el-skeleton v-if="loadingPolicy" :rows="4" animated />
    <template v-else-if="apiPolicy">
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
          <dd>{{ apiPolicy.allowedAdGroups.join(', ') || '—' }}</dd>
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
    <div class="action-row action-row--compact">
      <el-button :loading="submitting" @click="emit('createCredential')">
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
            @click="emit('rotateCredential', row.credentialId, row.externalId)"
          >
            {{ t('templates.policy.rotateCredential') }}
          </el-button>
          <el-button
            v-if="row.status === 'ACTIVE'"
            link
            type="danger"
            @click="emit('revokeCredential', row.credentialId)"
          >
            {{ t('templates.policy.revokeCredential') }}
          </el-button>
        </template>
      </el-table-column>
    </AppDataTable>
    <AppTablePagination
      v-model:current-page="credentialsCurrentPage"
      :page-size="pageSize"
      :total="totalCredentialRows"
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

.action-row--compact {
  margin-bottom: 1rem;
}

.policy-summary {
  display: grid;
  gap: 0.75rem;
  margin: 0 0 1rem;

  div {
    display: grid;
    grid-template-columns: 12rem 1fr;
    gap: 0.75rem;
  }

  dt {
    margin: 0;
    color: var(--text-muted);
    font-weight: 500;
  }

  dd {
    margin: 0;
  }
}

.policy-console-hint {
  margin: 0 0 1rem;
  color: var(--text-muted);
  font-size: 0.875rem;
}
</style>
