<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppDataTable from '@/components/common/AppDataTable.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import TemplateStatusBadge from '@/components/templates/TemplateStatusBadge.vue'
import { useLocaleFormatters } from '@/composables/useLocaleFormatters'
import { useCapabilities } from '@/composables/useCapabilities'
import * as templatesApi from '@/api/templates'
import {
  templateDevVersionPath,
  templatePackageHubPath,
} from '@/routing/routeKeys'
import type { TemplateVersionLineDetail } from '@/types/template'
import { versionLineDisplayLabel } from '@/utils/templateVersionLine'

const { t } = useI18n()
const { formatDateTime } = useLocaleFormatters()
const route = useRoute()
const router = useRouter()
const { authorTemplates } = useCapabilities()

const loading = ref(false)
const loadFailed = ref(false)
const cloning = ref(false)
const releaseDetail = ref<TemplateVersionLineDetail | null>(null)

const templateId = computed(() => String(route.params.templateId ?? ''))
const releaseVersion = computed(() => String(route.params.releaseVersion ?? ''))

const lineLabel = computed(() => {
  if (!releaseDetail.value) {
    return ''
  }
  return versionLineDisplayLabel(t, releaseDetail.value)
})

onMounted(async () => {
  await loadReleaseDetail()
})

async function loadReleaseDetail() {
  loading.value = true
  loadFailed.value = false
  try {
    releaseDetail.value = await templatesApi.fetchReleaseVersionDetail(
      templateId.value,
      releaseVersion.value,
    )
  } catch {
    loadFailed.value = true
    releaseDetail.value = null
  } finally {
    loading.value = false
  }
}

function backToHub() {
  router.push(templatePackageHubPath(templateId.value))
}

async function handleClone() {
  if (!releaseVersion.value) {
    return
  }
  cloning.value = true
  try {
    const created = await templatesApi.cloneReleaseVersion(templateId.value, releaseVersion.value)
    ElMessage.success(t('templates.versionLines.cloneSuccess'))
    router.push(templateDevVersionPath(templateId.value, created.devVersionId))
  } catch {
    ElMessage.error(t('templates.versionLines.cloneError'))
  } finally {
    cloning.value = false
  }
}
</script>

<template>
  <div class="template-release-detail-page">
    <header class="page-header">
      <el-button link type="primary" @click="backToHub">
        {{ t('templates.releaseDetail.backToHub') }}
      </el-button>
      <div v-if="releaseDetail" class="header-content">
        <div>
          <h1>{{ lineLabel }}</h1>
          <p>{{ t('templates.releaseDetail.readOnlyHint') }}</p>
        </div>
        <div class="header-actions">
          <TemplateStatusBadge
            :status="releaseDetail.lifecycleStatus"
            :approval-sub-state="releaseDetail.approvalSubState"
          />
          <el-button
            v-if="authorTemplates && releaseDetail.cloneable !== false"
            type="primary"
            :loading="cloning"
            @click="handleClone"
          >
            {{ t('templates.versionLines.clone') }}
          </el-button>
        </div>
      </div>
      <h1 v-else>{{ t('templates.releaseDetail.loadingTitle') }}</h1>
    </header>

    <LoadErrorPanel
      v-if="loadFailed"
      message-key="templates.releaseDetail.loadError"
      @retry="loadReleaseDetail"
    />

    <el-skeleton v-else-if="loading" :rows="8" animated />

    <EmptyStatePanel
      v-else-if="!releaseDetail"
      title-key="templates.releaseDetail.notFoundTitle"
      description-key="templates.releaseDetail.notFoundDescription"
    />

    <template v-else>
      <el-card shadow="never" class="summary-card">
        <dl class="summary-grid">
          <div>
            <dt>{{ t('templates.releaseDetail.releaseVersion') }}</dt>
            <dd>{{ releaseDetail.releaseVersion }}</dd>
          </div>
          <div>
            <dt>{{ t('templates.releaseDetail.devVersionNumber') }}</dt>
            <dd>{{ releaseDetail.devVersionNumber }}</dd>
          </div>
          <div>
            <dt>{{ t('templates.releaseDetail.updatedAt') }}</dt>
            <dd>{{ formatDateTime(releaseDetail.updatedAt) }}</dd>
          </div>
          <div>
            <dt>{{ t('templates.releaseDetail.updatedBy') }}</dt>
            <dd>{{ releaseDetail.updatedBy }}</dd>
          </div>
        </dl>
      </el-card>

      <el-card shadow="never" class="section-card">
        <template #header>
          <span>{{ t('templates.releaseDetail.variablesTitle') }}</span>
        </template>
        <AppDataTable :data="releaseDetail.variables">
          <template #empty>
            <el-empty :description="t('templates.releaseDetail.noVariables')" />
          </template>
          <el-table-column prop="variableKey" :label="t('templates.releaseDetail.columns.variableKey')" />
          <el-table-column prop="variableType" :label="t('templates.releaseDetail.columns.variableType')" />
          <el-table-column prop="required" :label="t('templates.releaseDetail.columns.required')" />
        </AppDataTable>
      </el-card>

      <el-card shadow="never" class="section-card">
        <template #header>
          <span>{{ t('templates.releaseDetail.bindingsTitle') }}</span>
        </template>
        <AppDataTable :data="releaseDetail.bindings">
          <template #empty>
            <el-empty :description="t('templates.releaseDetail.noBindings')" />
          </template>
          <el-table-column prop="anchorId" :label="t('templates.releaseDetail.columns.anchorId')" />
          <el-table-column
            prop="declaredContentType"
            :label="t('templates.releaseDetail.columns.contentType')"
          />
        </AppDataTable>
      </el-card>

      <el-card shadow="never" class="section-card">
        <template #header>
          <span>{{ t('templates.releaseDetail.rulesTitle') }}</span>
        </template>
        <AppDataTable :data="releaseDetail.rules">
          <template #empty>
            <el-empty :description="t('templates.releaseDetail.noRules')" />
          </template>
          <el-table-column prop="ruleId" :label="t('templates.releaseDetail.columns.ruleId')" />
          <el-table-column
            prop="targetAnchorId"
            :label="t('templates.releaseDetail.columns.targetAnchor')"
          />
        </AppDataTable>
      </el-card>
    </template>
  </div>
</template>

<style scoped lang="scss">
.template-release-detail-page {
  min-height: 100vh;
  padding: 2rem;
  background: var(--surface-bg);
}

.page-header {
  margin-bottom: 1.5rem;
}

.header-content {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-top: 0.5rem;

  h1 {
    margin: 0 0 0.35rem;
    font-size: 1.75rem;
  }

  p {
    margin: 0;
    color: var(--text-muted);
  }
}

.header-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
}

.summary-card,
.section-card {
  margin-bottom: 1rem;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1rem;
  margin: 0;

  dt {
    margin: 0;
    font-size: 0.85rem;
    color: var(--text-muted);
  }

  dd {
    margin: 0.25rem 0 0;
    font-weight: 500;
  }
}
</style>
