<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { fetchRoutesSummary } from '@/api/apiPolicy'
import AppDataTable from '@/components/common/AppDataTable.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import type { RoutesSummary } from '@/types/template'

const props = defineProps<{
  templateId: string
}>()

const { t } = useI18n()

const loading = ref(true)
const loadFailed = ref(false)
const summary = ref<RoutesSummary | null>(null)

const explicitPaths = computed(() => summary.value?.explicitPaths ?? [])

const defaultRouteBadgeLabel = computed(() => {
  const version = summary.value?.defaultRouteReleaseVersion?.trim()
  if (!version) {
    return t('templates.policy.routesSummary.defaultRouteUnset')
  }
  return t('templates.policy.routesSummary.defaultRouteBadge', { version })
})

async function loadSummary() {
  loading.value = true
  loadFailed.value = false
  try {
    summary.value = await fetchRoutesSummary(props.templateId)
  } catch {
    summary.value = null
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadSummary()
})
</script>

<template>
  <el-card shadow="never" class="section-card" data-testid="route-summary-panel">
    <h2>{{ t('templates.policy.routesSummary.title') }}</h2>
    <p class="section-description">{{ t('templates.policy.routesSummary.description') }}</p>

    <el-skeleton v-if="loading" :rows="4" animated />

    <LoadErrorPanel
      v-else-if="loadFailed"
      message-key="templates.policy.routesSummary.loadFailed"
      @retry="loadSummary"
    />

    <template v-else-if="summary">
      <dl class="summary-grid">
        <div class="summary-item">
          <dt>{{ t('templates.policy.routesSummary.externalId') }}</dt>
          <dd>{{ summary.externalId }}</dd>
        </div>
        <div class="summary-item">
          <dt>{{ t('templates.policy.routesSummary.defaultPath') }}</dt>
          <dd class="path-value">{{ summary.defaultPath }}</dd>
        </div>
        <div class="summary-item">
          <dt>{{ t('templates.policy.routesSummary.defaultRelease') }}</dt>
          <dd>
            <el-tag type="info" effect="plain" size="small">
              {{ defaultRouteBadgeLabel }}
            </el-tag>
          </dd>
        </div>
      </dl>

      <div v-if="explicitPaths.length > 0" class="paths-section">
        <h3>{{ t('templates.policy.routesSummary.explicitPathsTitle') }}</h3>
        <p class="section-description">{{ t('templates.policy.routesSummary.explicitPathsDescription') }}</p>
        <AppDataTable :data="explicitPaths">
          <el-table-column
            prop="releaseVersion"
            :label="t('templates.policy.routesSummary.columns.releaseVersion')"
            width="160"
          />
          <el-table-column
            prop="explicitVersionUrl"
            :label="t('templates.policy.routesSummary.columns.path')"
            min-width="320"
          />
        </AppDataTable>
      </div>

      <EmptyStatePanel
        v-else
        title-key="templates.policy.routesSummary.noExplicitPathsTitle"
        description-key="templates.policy.routesSummary.noExplicitPathsDescription"
      />
    </template>
  </el-card>
</template>

<style scoped lang="scss">
.section-card {
  margin-bottom: 0;

  h2 {
    margin: 0 0 var(--space-2);
    font-size: var(--font-size-lg);
  }

  h3 {
    margin: var(--space-6) 0 var(--space-2);
    font-size: var(--font-size-md);
    font-weight: 600;
  }
}

.section-description {
  margin: 0 0 var(--space-4);
  color: var(--text-muted);
  font-size: var(--font-size-sm);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(14rem, 1fr));
  gap: var(--space-4);
  margin: 0;
}

.summary-item {
  dt {
    margin: 0 0 var(--space-1);
    color: var(--text-muted);
    font-size: var(--font-size-sm);
    font-weight: 500;
  }

  dd {
    margin: 0;
    color: var(--text-primary);
    font-size: var(--font-size-sm);
    word-break: break-word;
  }
}

.path-value {
  font-family: var(--font-family-mono, ui-monospace, monospace);
}

.paths-section {
  margin-top: var(--space-2);
}
</style>
