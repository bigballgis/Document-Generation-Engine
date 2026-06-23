<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import TemplateStatusBadge from '@/components/templates/TemplateStatusBadge.vue'
import { useCapabilities } from '@/composables/useCapabilities'
import { useConfirmAction } from '@/composables/useConfirmAction'
import * as templatesApi from '@/api/templates'
import { useTemplatesStore } from '@/stores/templates'
import type {
  LifecycleGovernanceAction,
  TemplateLifecycleStatus,
  TemplateReleaseVersion,
} from '@/types/template'

const props = defineProps<{
  templateId: string
  templateLifecycleStatus: TemplateLifecycleStatus
}>()

const emit = defineEmits<{
  changed: []
}>()

const { t, te } = useI18n()
const templatesStore = useTemplatesStore()
const { manageReleaseVersionState } = useCapabilities()
const { confirmAction } = useConfirmAction()

const loading = ref(false)
const loadError = ref(false)
const versions = ref<TemplateReleaseVersion[]>([])

const canManageVersions = computed(
  () =>
    manageReleaseVersionState.value &&
    props.templateLifecycleStatus === 'PUBLISHED',
)

const showWorkflowHint = computed(() =>
  ['DRAFT', 'TESTING', 'APPROVAL', 'PENDING_RELEASE'].includes(props.templateLifecycleStatus),
)

const errorMessage = computed(() => {
  const key = templatesStore.lastErrorMessageKey
  if (!key) {
    return ''
  }
  return te(key) ? t(key) : t('templates.error.loadDetail')
})

async function loadVersions() {
  loading.value = true
  loadError.value = false
  try {
    versions.value = await templatesApi.fetchReleaseVersions(props.templateId)
  } catch {
    loadError.value = true
    versions.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadVersions()
})

watch(
  () => props.templateId,
  () => {
    void loadVersions()
  },
)

async function buildImpactPreviewMessage(
  action: LifecycleGovernanceAction,
  releaseVersion: string,
): Promise<string> {
  const preview = await templatesStore.fetchLifecycleImpactPreview(props.templateId, {
    action,
    releaseVersion,
  })
  const summary = te(preview.summaryMessageKey)
    ? t(preview.summaryMessageKey)
    : t(`templates.governance.impactSummary.${action}`)
  const callable = preview.callableReleaseVersions.length
    ? t('templates.governance.impactCallableVersions', {
        versions: preview.callableReleaseVersions.join(', '),
      })
    : t('templates.governance.impactNoCallableVersions')
  const defaultRoute = preview.defaultRouteReleaseVersion
    ? t('templates.governance.impactDefaultRoute', {
        version: preview.defaultRouteReleaseVersion,
      })
    : ''
  const routeImpact = preview.defaultRouteImpacted
    ? t('templates.governance.impactDefaultRouteAffected')
    : ''
  return [summary, callable, defaultRoute, routeImpact, t('templates.governance.impactConfirmPrompt')]
    .filter(Boolean)
    .join('\n\n')
}

async function handleVersionAction(
  releaseVersion: string,
  action: 'deactivate' | 'restore',
) {
  const previewAction: LifecycleGovernanceAction =
    action === 'deactivate' ? 'DEACTIVATE_VERSION' : 'RESTORE_VERSION'
  const reasonKey =
    action === 'deactivate'
      ? 'templates.versions.deactivateReasonPrompt'
      : 'templates.versions.restoreReasonPrompt'
  const titleKey =
    action === 'deactivate'
      ? 'templates.versions.deactivateTitle'
      : 'templates.versions.restoreTitle'
  const confirmTitleKey =
    action === 'deactivate'
      ? 'templates.versions.confirmDeactivateTitle'
      : 'templates.versions.confirmRestoreTitle'
  const confirmMessageKey =
    action === 'deactivate'
      ? 'templates.versions.confirmDeactivateMessage'
      : 'templates.versions.confirmRestoreMessage'
  const successKey =
    action === 'deactivate'
      ? 'templates.versions.deactivateSuccess'
      : 'templates.versions.restoreSuccess'

  let reason = ''
  try {
    const result = await ElMessageBox.prompt(t(reasonKey), t(titleKey), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      inputValidator: (value) =>
        value.trim().length > 0 ? true : t('templates.lifecycle.reasonRequired'),
    })
    reason = result.value.trim()
  } catch {
    return
  }

  try {
    const impactMessage = await buildImpactPreviewMessage(previewAction, releaseVersion)
    await ElMessageBox.confirm(impactMessage, t('templates.governance.impactPreviewTitle'), {
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
      type: 'warning',
    })
  } catch {
    return
  }

  const confirmed = await confirmAction({
    titleKey: confirmTitleKey,
    messageKey: confirmMessageKey,
    type: 'warning',
  })
  if (!confirmed) {
    return
  }

  const payload = { reason, confirmed: true }
  try {
    if (action === 'deactivate') {
      await templatesStore.deactivateTemplateVersion(props.templateId, releaseVersion, payload)
    } else {
      await templatesStore.restoreTemplateVersion(props.templateId, releaseVersion, payload)
    }
    ElMessage.success(t(successKey))
    await loadVersions()
    emit('changed')
  } catch {
    ElMessage.error(errorMessage.value || t('templates.error.lifecycle'))
  }
}
</script>

<template>
  <div class="release-version-history">
    <header class="panel-header">
      <h2>{{ t('templates.versions.title') }}</h2>
      <p>{{ t('templates.versions.description') }}</p>
    </header>

    <el-alert
      v-if="showWorkflowHint"
      type="info"
      :title="t('templates.versions.workflowHintTitle')"
      :description="t('templates.versions.workflowHintDescription')"
      show-icon
      :closable="false"
      class="workflow-hint"
    />

    <el-alert
      v-if="loadError"
      type="error"
      :title="t('templates.versions.loadError')"
      show-icon
      :closable="false"
    />

    <el-skeleton v-else-if="loading" :rows="4" animated />

    <el-table v-else :data="versions" stripe>
      <template #empty>
        <el-empty :description="t('templates.versions.empty')" />
      </template>
      <el-table-column
        prop="releaseVersion"
        :label="t('templates.versions.releaseVersion')"
        min-width="140"
      />
      <el-table-column
        prop="devVersionNumber"
        :label="t('templates.versions.devVersionNumber')"
        width="120"
      />
      <el-table-column :label="t('templates.versions.status')" width="160">
        <template #default="{ row }">
          <TemplateStatusBadge :status="row.lifecycleStatus" />
        </template>
      </el-table-column>
      <el-table-column :label="t('templates.versions.defaultRoute')" width="140">
        <template #default="{ row }">
          <el-tag v-if="row.defaultRouteTarget" type="success" size="small">
            {{ t('templates.versions.defaultRouteYes') }}
          </el-tag>
          <span v-else>{{ t('templates.versions.defaultRouteNo') }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="t('templates.versions.updatedAt')" min-width="180">
        <template #default="{ row }">
          {{ new Date(row.updatedAt).toLocaleString() }}
        </template>
      </el-table-column>
      <el-table-column
        v-if="canManageVersions"
        :label="t('templates.versions.actions')"
        min-width="220"
      >
        <template #default="{ row }">
          <el-button
            v-if="row.lifecycleStatus === 'PUBLISHED'"
            link
            type="warning"
            :loading="templatesStore.submitting"
            @click="handleVersionAction(row.releaseVersion, 'deactivate')"
          >
            {{ t('templates.versions.deactivate') }}
          </el-button>
          <el-button
            v-if="row.lifecycleStatus === 'STOPPED'"
            link
            type="primary"
            :loading="templatesStore.submitting"
            @click="handleVersionAction(row.releaseVersion, 'restore')"
          >
            {{ t('templates.versions.restore') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped lang="scss">
.release-version-history {
  padding: 0.25rem 0;
}

.panel-header {
  margin-bottom: 1rem;

  h2 {
    margin: 0 0 0.25rem;
    font-size: 1.125rem;
  }

  p {
    margin: 0;
    color: var(--text-muted);
  }
}

.workflow-hint {
  margin-bottom: 1rem;
}
</style>
