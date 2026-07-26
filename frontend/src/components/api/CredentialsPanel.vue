<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import AppDataTable from '@/components/common/AppDataTable.vue'
import AppTablePagination from '@/components/common/AppTablePagination.vue'
import TableColumnHeader from '@/components/common/TableColumnHeader.vue'
import type { ApiCredentialSummary } from '@/types/template'

const props = defineProps<{
  credentials: ApiCredentialSummary[]
  credentialStatusFilterOptions: Array<{ label: string; value: string }>
  pageSize: number
  totalRows: number
  submitting?: boolean
  formatDateTime: (value: string) => string
  sortByCreatedAt: (a: ApiCredentialSummary, b: ApiCredentialSummary) => number
  showCreateButton?: boolean
}>()

const credentialColumnFilters = defineModel<Record<string, string>>('credentialColumnFilters', {
  required: true,
})

const currentPage = defineModel<number>('currentPage', { required: true })

const emit = defineEmits<{
  create: []
  rotate: [credentialId: string, externalId: string]
  revoke: [credentialId: string]
}>()

const { t } = useI18n()

const secretDialogVisible = ref(false)
const secretExternalId = ref('')
const secretValue = ref('')
const secretExpiresAt = ref<string | null>(null)
const secretGraceEndsAt = ref<string | null>(null)

const showCreate = computed(() => props.showCreateButton !== false)

function isCallableStatus(status: string): boolean {
  return status === 'ACTIVE' || status === 'EXPIRING_SOON'
}

function revealSecret(
  externalId: string,
  secret: string,
  meta?: { expiresAt?: string | null; rotationGracePeriodEndsAt?: string | null },
) {
  secretExternalId.value = externalId
  secretValue.value = secret
  secretExpiresAt.value = meta?.expiresAt ?? null
  secretGraceEndsAt.value = meta?.rotationGracePeriodEndsAt ?? null
  secretDialogVisible.value = true
}

async function copySecret() {
  try {
    await navigator.clipboard.writeText(secretValue.value)
    ElMessage.success(t('templates.policy.credentialSecretCopied'))
  } catch {
    ElMessage.error(t('templates.policy.credentialSecretCopyFailed'))
  }
}

defineExpose({ revealSecret })
</script>

<template>
  <div class="credentials-panel">
    <div v-if="showCreate" class="action-row action-row--compact">
      <el-button :loading="submitting" @click="emit('create')">
        {{ t('templates.policy.createCredential') }}
      </el-button>
    </div>

    <AppDataTable :data="credentials" empty-text="">
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
      <el-table-column sortable :sort-method="sortByCreatedAt" min-width="180">
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
      <el-table-column min-width="180">
        <template #header>
          <TableColumnHeader :label="t('templates.policy.credentialExpiresAt')" />
        </template>
        <template #default="{ row }">
          {{ row.expiresAt ? formatDateTime(row.expiresAt) : '—' }}
        </template>
      </el-table-column>
      <el-table-column :label="t('templates.policy.credentialActions')" min-width="200">
        <template #default="{ row }">
          <el-button
            v-if="isCallableStatus(row.status)"
            link
            type="primary"
            @click="emit('rotate', row.credentialId, row.externalId)"
          >
            {{ t('templates.policy.rotateCredential') }}
          </el-button>
          <el-button
            v-if="isCallableStatus(row.status)"
            link
            type="danger"
            @click="emit('revoke', row.credentialId)"
          >
            {{ t('templates.policy.revokeCredential') }}
          </el-button>
        </template>
      </el-table-column>
    </AppDataTable>

    <AppTablePagination
      v-model:current-page="currentPage"
      :page-size="pageSize"
      :total="totalRows"
    />

    <el-dialog
      v-model="secretDialogVisible"
      :title="t('templates.policy.credentialSecretDialogTitle')"
      width="32rem"
      :close-on-click-modal="false"
    >
      <p>{{ t('templates.policy.credentialSecretHint') }}</p>
      <p>{{ t('templates.policy.credentialExternalId') }}: {{ secretExternalId }}</p>
      <p v-if="secretExpiresAt">
        {{ t('templates.policy.credentialExpiresAt') }}: {{ formatDateTime(secretExpiresAt) }}
      </p>
      <p v-if="secretGraceEndsAt">
        {{ t('templates.policy.credentialRotationGraceEndsAt') }}:
        {{ formatDateTime(secretGraceEndsAt) }}
      </p>
      <el-input :model-value="secretValue" readonly type="textarea" :rows="3" />
      <template #footer>
        <el-button @click="copySecret">{{ t('templates.policy.copyCredentialSecret') }}</el-button>
        <el-button type="primary" @click="secretDialogVisible = false">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.credentials-panel {
  display: flex;
  flex-wrap: wrap;
  flex-direction: column;
  gap: var(--space-4);
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  align-items: center;
}

.action-row--compact {
  margin-bottom: 0;
}
</style>
