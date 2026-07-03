<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import AppDataTable from '@/components/common/AppDataTable.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import { useActivatableTableRow } from '@/composables/useActivatableTableRow'
import { templatePackageHubPath, ROUTE_PATH_BY_KEY, ROUTE_KEYS } from '@/routing/routeKeys'
import { useTemplatesStore } from '@/stores/templates'
import type { TemplateSummary } from '@/types/template'

const { t, te } = useI18n()
const router = useRouter()
const templatesStore = useTemplatesStore()

const publishedTemplates = computed(() => templatesStore.publishedTemplates)

const errorMessage = computed(() => {
  const key = templatesStore.lastErrorMessageKey
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('templates.error.loadList')
})

onMounted(async () => {
  try {
    await templatesStore.fetchTemplates()
  } catch {
    // Error surfaced via store message key.
  }
})

function openPackageAccess(templateId: string) {
  router.push(templatePackageHubPath(templateId, 'apiAccess'))
}

function openTemplateCatalog() {
  router.push(ROUTE_PATH_BY_KEY[ROUTE_KEYS.templateManagement])
}

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
      v-if="errorMessage"
      class="page-alert"
      type="error"
      :title="errorMessage"
      show-icon
      :closable="false"
    />

    <el-alert
      class="page-alert monitoring-hint"
      type="info"
      :title="t('apiPolicy.home.monitoringHint')"
      show-icon
      :closable="false"
    />

    <el-card shadow="never" class="section-card">
      <h2>{{ t('apiPolicy.home.alerts.title') }}</h2>
      <p class="section-description">{{ t('apiPolicy.home.alerts.description') }}</p>
      <EmptyStatePanel
        title-key="apiPolicy.home.alerts.comingSoonTitle"
        description-key="apiPolicy.home.alerts.comingSoonDescription"
      />
    </el-card>

    <el-card shadow="never" class="section-card">
      <div class="section-header">
        <div>
          <h2>{{ t('apiPolicy.home.packageLinks.title') }}</h2>
          <p class="section-description">{{ t('apiPolicy.home.packageLinks.description') }}</p>
        </div>
        <el-button @click="openTemplateCatalog">
          {{ t('apiPolicy.home.packageLinks.browseTemplates') }}
        </el-button>
      </div>

      <el-skeleton v-if="templatesStore.loadingList" :rows="4" animated />

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
