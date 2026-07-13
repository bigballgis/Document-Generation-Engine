<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import AppDataTable from '@/components/common/AppDataTable.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import EntityLinkCell from '@/components/common/EntityLinkCell.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import { useAbortableCatalogLoader } from '@/composables/useAbortableCatalogLoader'
import { useActivatableTableRow } from '@/composables/useActivatableTableRow'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { ROUTE_PATH_BY_KEY, ROUTE_KEYS, templatePackageHubPath } from '@/routing/routeKeys'
import { useApiPolicyStore } from '@/stores/apiPolicy'
import { useSessionStore } from '@/stores/session'
import type { ApiAccessAlert, ApiAccessAlertKind } from '@/types/template'

const { t } = useI18n()
const router = useRouter()
const { formatDateTime } = useLocaleFormatters()
const apiPolicyStore = useApiPolicyStore()
const sessionStore = useSessionStore()

const alertsErrorMessageKey = computed(() => apiPolicyStore.alertsErrorMessageKey)
const summaryErrorMessageKey = computed(() => apiPolicyStore.summaryErrorMessageKey)
const canBrowseTemplates = computed(() =>
  sessionStore.canAccessRoute(ROUTE_KEYS.templateManagement),
)

const summaryCards = computed(() => [
  {
    key: 'publishedInScope',
    count: apiPolicyStore.readinessSummary.publishedInScopeCount,
    titleKey: 'apiPolicy.home.summary.publishedInScope',
    descriptionKey: 'apiPolicy.home.summary.publishedInScopeDescription',
  },
  {
    key: 'attention',
    count: apiPolicyStore.readinessSummary.attentionCount,
    titleKey: 'apiPolicy.home.summary.attention',
    descriptionKey: 'apiPolicy.home.summary.attentionDescription',
  },
  {
    key: 'pendingReleaseNeedingSetup',
    count: apiPolicyStore.readinessSummary.pendingReleaseNeedingSetupCount,
    titleKey: 'apiPolicy.home.summary.pendingReleaseNeedingSetup',
    descriptionKey: 'apiPolicy.home.summary.pendingReleaseNeedingSetupDescription',
  },
])

const { reload: reloadOverview } = useAbortableCatalogLoader(async () => {
  await Promise.all([apiPolicyStore.fetchAlerts(), apiPolicyStore.fetchReadinessSummary()])
})

onMounted(async () => {
  await reloadOverview()
})

function openPackageAccess(templateId: string) {
  router.push(templatePackageHubPath(templateId, 'apiAccess'))
}

function openTemplateCatalog() {
  router.push(ROUTE_PATH_BY_KEY[ROUTE_KEYS.templateManagement])
}

function alertMessageKey(kind: ApiAccessAlertKind): string {
  switch (kind) {
    case 'MISSING_AD_GROUP':
      return 'apiPolicy.home.alerts.missingAdGroup'
    case 'EXPIRING_CREDENTIAL':
      return 'apiPolicy.home.alerts.expiringCredential'
    case 'NO_CREDENTIALS':
      return 'apiPolicy.home.alerts.noCredentials'
    default:
      return 'apiPolicy.home.alerts.title'
  }
}

function formatAlertDetail(alert: ApiAccessAlert): string {
  if (alert.alertKind === 'EXPIRING_CREDENTIAL' && alert.credentialExpiresAt) {
    return t('apiPolicy.home.alerts.expiringCredentialDetail', {
      externalId: alert.credentialExternalId ?? '—',
      expiresAt: formatDateTime(alert.credentialExpiresAt),
    })
  }
  if (alert.alertKind === 'NO_CREDENTIALS') {
    return t('apiPolicy.home.alerts.noCredentialsDetail', {
      externalId: alert.templateExternalId,
    })
  }
  return t('apiPolicy.home.alerts.missingAdGroupDetail', {
    externalId: alert.templateExternalId,
  })
}

const { onRowClick: activateAlertRow } = useActivatableTableRow<ApiAccessAlert>((row) =>
  openPackageAccess(row.templateId),
)
</script>

<template>
  <AppPageLayout layout-variant="fluid">
    <PageHeader
      :title="t('apiPolicy.home.title')"
      :description="t('apiPolicy.home.description')"
    >
      <template #actions>
        <el-button v-if="canBrowseTemplates" @click="openTemplateCatalog">
          {{ t('apiPolicy.home.packageLinks.browseTemplates') }}
        </el-button>
      </template>
    </PageHeader>

    <el-alert
      class="page-alert monitoring-hint"
      type="info"
      :title="t('apiPolicy.home.monitoringHint')"
      show-icon
      :closable="false"
    />

    <section class="summary-section" data-testid="api-readiness-summary" aria-labelledby="api-readiness-summary-title">
      <h2 id="api-readiness-summary-title">{{ t('apiPolicy.home.summary.title') }}</h2>
      <p class="section-description">{{ t('apiPolicy.home.summary.description') }}</p>

      <LoadErrorPanel
        v-if="summaryErrorMessageKey && !apiPolicyStore.loadingSummary"
        :message-key="summaryErrorMessageKey"
        :retryable="apiPolicyStore.summaryErrorRetryable"
        @retry="reloadOverview"
      />

      <el-skeleton v-else-if="apiPolicyStore.loadingSummary" :rows="2" animated />

      <div v-else class="summary-grid">
        <el-card
          v-for="card in summaryCards"
          :key="card.key"
          shadow="never"
          class="summary-card"
          :data-testid="`summary-card-${card.key}`"
        >
          <p class="summary-count">{{ card.count }}</p>
          <h3>{{ t(card.titleKey) }}</h3>
          <p class="summary-card-description">{{ t(card.descriptionKey) }}</p>
        </el-card>
      </div>
    </section>

    <el-card shadow="never" class="section-card alerts-card">
      <h2>{{ t('apiPolicy.home.alerts.title') }}</h2>
      <p class="section-description">{{ t('apiPolicy.home.alerts.description') }}</p>

      <LoadErrorPanel
        v-if="alertsErrorMessageKey && !apiPolicyStore.loadingAlerts"
        :message-key="alertsErrorMessageKey"
        :retryable="apiPolicyStore.alertsErrorRetryable"
        @retry="reloadOverview"
      />

      <el-skeleton v-else-if="apiPolicyStore.loadingAlerts" :rows="4" animated />

      <template v-else>
        <AppDataTable
          activatable
          :data="apiPolicyStore.alerts"
          @row-click="activateAlertRow"
        >
          <template #empty>
            <EmptyStatePanel
              title-key="apiPolicy.home.alerts.emptyTitle"
              description-key="apiPolicy.home.alerts.emptyDescription"
            >
              <template v-if="canBrowseTemplates" #actions>
                <el-button type="primary" @click="openTemplateCatalog">
                  {{ t('apiPolicy.home.packageLinks.browseTemplates') }}
                </el-button>
              </template>
            </EmptyStatePanel>
          </template>
          <el-table-column :label="t('apiPolicy.home.alerts.columns.issue')" min-width="220">
            <template #default="{ row }">
              {{ t(alertMessageKey(row.alertKind)) }}
            </template>
          </el-table-column>
          <el-table-column :label="t('apiPolicy.home.alerts.columns.package')" min-width="220">
            <template #default="{ row }">
              <EntityLinkCell
                :label="row.templateName"
                :subtitle="row.templateExternalId"
                :to="templatePackageHubPath(row.templateId, 'apiAccess')"
              />
            </template>
          </el-table-column>
          <el-table-column :label="t('apiPolicy.home.alerts.columns.detail')" min-width="240">
            <template #default="{ row }">
              {{ formatAlertDetail(row) }}
            </template>
          </el-table-column>
          <el-table-column :label="t('apiPolicy.home.packageLinks.action')" width="180" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click.stop="openPackageAccess(row.templateId)">
                {{ t('apiPolicy.home.packageLinks.openAccess') }}
              </el-button>
            </template>
          </el-table-column>
        </AppDataTable>
      </template>
    </el-card>
  </AppPageLayout>
</template>

<style scoped lang="scss">
.page-alert {
  margin-bottom: var(--space-6);
}

.summary-section {
  margin-bottom: var(--space-6);

  h2 {
    margin: 0 0 0.35rem;
    font-size: var(--font-size-lg);
  }
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-4);
}

.summary-card {
  h3 {
    margin: 0 0 var(--space-2);
    font-size: var(--font-size-md);
    font-weight: 600;
  }
}

.summary-count {
  margin: 0 0 var(--space-2);
  font-size: var(--font-size-2xl);
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.2;
}

.summary-card-description {
  margin: 0;
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

.section-card {
  h2 {
    margin: 0 0 0.35rem;
    font-size: var(--font-size-lg);
  }
}

.section-description {
  margin: 0 0 var(--space-4);
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

@media (max-width: 960px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
