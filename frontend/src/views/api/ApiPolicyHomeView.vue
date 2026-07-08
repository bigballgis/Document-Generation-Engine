<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
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
import { templatePackageHubPath, ROUTE_PATH_BY_KEY, ROUTE_KEYS } from '@/routing/routeKeys'
import { useApiPolicyStore } from '@/stores/apiPolicy'
import { useTemplatesStore } from '@/stores/templates'
import type { ApiAccessAlert, ApiAccessAlertKind } from '@/types/template'
import type { TemplateSummary } from '@/types/template'

const { t } = useI18n()
const router = useRouter()
const { formatDateTime } = useLocaleFormatters()
const templatesStore = useTemplatesStore()
const apiPolicyStore = useApiPolicyStore()

const publishedTemplates = computed(() => templatesStore.publishedTemplates)
const packageLinksCollapse = ref<string[]>([])

const templateListErrorMessage = computed(() => {
  const key = templatesStore.lastErrorMessageKey
  if (!key) {
    return ''
  }
  return t(key)
})

const alertsErrorMessageKey = computed(() => apiPolicyStore.alertsErrorMessageKey)

const { reload: reloadTemplates } = useAbortableCatalogLoader((signal) =>
  templatesStore.fetchTemplates(0, templatesStore.templateListSize, { signal }),
)

const { reload: reloadAlerts } = useAbortableCatalogLoader(() => apiPolicyStore.fetchAlerts())

onMounted(async () => {
  await Promise.all([reloadAlerts(), reloadTemplates()])
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
  if (alert.alertKind === 'EXPIRING_CREDENTIAL' && alert.credentialExternalId) {
    const expiry = alert.credentialExpiresAt ? formatDateTime(alert.credentialExpiresAt) : '—'
    return t('apiPolicy.home.alerts.expiringCredentialDetail', {
      externalId: alert.credentialExternalId,
      expiresAt: expiry,
    })
  }
  return alert.templateExternalId
}

const { onRowClick: activateAlertRow } = useActivatableTableRow<ApiAccessAlert>((row) =>
  openPackageAccess(row.templateId),
)

const { onRowClick: activateTemplateRow } = useActivatableTableRow<TemplateSummary>((row) =>
  openPackageAccess(row.id),
)
</script>

<template>
  <AppPageLayout>
    <PageHeader
      :title="t('apiPolicy.home.title')"
      :description="t('apiPolicy.home.description')"
    />

    <el-alert
      class="page-alert monitoring-hint"
      type="info"
      :title="t('apiPolicy.home.monitoringHint')"
      show-icon
      :closable="false"
    />

    <el-card shadow="never" class="section-card alerts-card">
      <h2>{{ t('apiPolicy.home.alerts.title') }}</h2>
      <p class="section-description">{{ t('apiPolicy.home.alerts.description') }}</p>

      <LoadErrorPanel
        v-if="alertsErrorMessageKey && !apiPolicyStore.loadingAlerts"
        :message-key="alertsErrorMessageKey"
        @retry="reloadAlerts"
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
            />
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
                :link-to="templatePackageHubPath(row.templateId, 'apiAccess')"
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

    <el-card shadow="never" class="section-card package-links-card">
      <el-collapse v-model="packageLinksCollapse" class="package-links-collapse">
        <el-collapse-item name="packages" :title="t('apiPolicy.home.packageLinks.title')">
          <div class="section-header">
            <p class="section-description">{{ t('apiPolicy.home.packageLinks.description') }}</p>
            <el-button @click="openTemplateCatalog">
              {{ t('apiPolicy.home.packageLinks.browseTemplates') }}
            </el-button>
          </div>

          <LoadErrorPanel
            v-if="templateListErrorMessage && !templatesStore.loadingList"
            :message-key="templatesStore.lastErrorMessageKey ?? 'templates.error.loadList'"
            :retryable="templatesStore.lastListErrorRetryable"
            @retry="reloadTemplates"
          />

          <el-alert
            v-else-if="templateListErrorMessage"
            class="page-alert"
            type="error"
            :title="templateListErrorMessage"
            show-icon
            :closable="false"
          />

          <el-skeleton v-else-if="templatesStore.loadingList" :rows="4" animated />

          <template v-else>
            <AppDataTable activatable :data="publishedTemplates" @row-click="activateTemplateRow">
              <template #empty>
                <EmptyStatePanel title-key="apiPolicy.home.empty" />
              </template>
              <el-table-column prop="name" :label="t('templates.list.columns.name')" min-width="220" />
              <el-table-column
                prop="externalId"
                :label="t('templates.list.columns.externalId')"
                min-width="180"
              />
              <el-table-column
                prop="groupCode"
                :label="t('apiPolicy.home.groupCode')"
                width="140"
              />
              <el-table-column
                prop="releaseVersion"
                :label="t('templates.list.columns.releaseVersion')"
                width="140"
              />
              <el-table-column :label="t('apiPolicy.home.packageLinks.action')" width="180" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" link @click.stop="openPackageAccess(row.id)">
                    {{ t('apiPolicy.home.packageLinks.openAccess') }}
                  </el-button>
                </template>
              </el-table-column>
            </AppDataTable>
          </template>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </AppPageLayout>
</template>

<style scoped lang="scss">
.page-alert {
  margin-bottom: var(--space-4);
}

.monitoring-hint {
  margin-bottom: var(--space-6);
}

.section-card {
  margin-bottom: var(--space-6);

  h2 {
    margin: 0 0 var(--space-2);
    font-size: var(--font-size-lg);
  }
}

.alerts-card {
  h2 {
    margin-bottom: var(--space-2);
  }
}

.package-links-card {
  :deep(.el-collapse-item__header) {
    font-size: var(--font-size-md);
    font-weight: 600;
    color: var(--text-primary);
  }
}

.section-header {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-4);
}

.section-description {
  margin: 0;
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}
</style>
