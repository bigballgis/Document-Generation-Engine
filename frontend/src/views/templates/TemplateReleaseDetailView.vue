<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import AppPageLayout from '@/components/layout/AppPageLayout.vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import TemplateStatusBadge from '@/components/templates/TemplateStatusBadge.vue'
import ReleaseSectionTable from '@/components/templates/ReleaseSectionTable.vue'
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

const variableColumns = computed(() => [
  { prop: 'variableKey', label: t('templates.releaseDetail.columns.variableKey') },
  { prop: 'variableType', label: t('templates.releaseDetail.columns.variableType') },
  { prop: 'required', label: t('templates.releaseDetail.columns.required') },
])

const bindingColumns = computed(() => [
  { prop: 'anchorId', label: t('templates.releaseDetail.columns.anchorId') },
  { prop: 'declaredContentType', label: t('templates.releaseDetail.columns.contentType') },
])

const ruleColumns = computed(() => [
  { prop: 'ruleId', label: t('templates.releaseDetail.columns.ruleId') },
  { prop: 'targetAnchorId', label: t('templates.releaseDetail.columns.targetAnchor') },
])

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
  <AppPageLayout>
    <PageHeader
      show-back
      :back-label="t('templates.releaseDetail.backToHub')"
      :title="releaseDetail ? lineLabel : t('templates.releaseDetail.loadingTitle')"
      :description="releaseDetail ? t('templates.releaseDetail.readOnlyHint') : undefined"
      @back="backToHub"
    >
      <template v-if="releaseDetail" #actions>
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
      </template>
    </PageHeader>

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

      <ReleaseSectionTable
        :title="t('templates.releaseDetail.variablesTitle')"
        :columns="variableColumns"
        :data="releaseDetail.variables"
        :empty-text="t('templates.releaseDetail.noVariables')"
      />

      <ReleaseSectionTable
        :title="t('templates.releaseDetail.bindingsTitle')"
        :columns="bindingColumns"
        :data="releaseDetail.bindings"
        :empty-text="t('templates.releaseDetail.noBindings')"
      />

      <ReleaseSectionTable
        :title="t('templates.releaseDetail.rulesTitle')"
        :columns="ruleColumns"
        :data="releaseDetail.rules"
        :empty-text="t('templates.releaseDetail.noRules')"
      />
    </template>
  </AppPageLayout>
</template>

<style scoped lang="scss">
.summary-card {
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
