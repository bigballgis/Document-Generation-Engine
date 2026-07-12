<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import VersionLinesTable from '@/components/templates/VersionLinesTable.vue'
import { useVersionLinesPanel } from '@/components/templates/useVersionLinesPanel'

const props = defineProps<{
  templateId: string
  canClone?: boolean
  canManageVersions?: boolean
}>()

const emit = defineEmits<{
  cloned: []
  changed: []
}>()

const { t } = useI18n()

const {
  templatesStore,
  formatDateTime,
  loadError,
  cloningReleaseVersion,
  abandoningDevVersionId,
  currentPage,
  pageSize,
  loading,
  versionLines,
  totalElements,
  latestPublishedLine,
  lineLabel,
  loadVersionLines,
  openVersionLine,
  onRowClick,
  showPagination,
  showCreateFromLatestRelease,
  canCloneRow,
  canAbandonRow,
  canDeactivateRow,
  canRestoreRow,
  handleClone,
  handleCreateFromLatestRelease,
  handleAbandon,
  handleVersionAction,
} = useVersionLinesPanel(props, emit)

defineExpose({
  reload: loadVersionLines,
})
</script>

<template>
  <el-card shadow="never" class="version-lines-card">
    <template #header>
      <div class="card-header">
        <div class="card-header-row">
          <span>{{ t('templates.versionLines.title') }}</span>
          <el-button
            v-if="showCreateFromLatestRelease"
            type="primary"
            data-version-line-create-from-latest
            :loading="cloningReleaseVersion === latestPublishedLine?.releaseVersion"
            @click="handleCreateFromLatestRelease"
          >
            {{ t('templates.versionLines.createFromLatestRelease') }}
          </el-button>
        </div>
        <p class="card-hint">{{ t('templates.versionLines.hint') }}</p>
      </div>
    </template>

    <LoadErrorPanel
      v-if="loadError"
      message-key="templates.versionLines.loadError"
      @retry="loadVersionLines"
    />

    <el-skeleton v-else-if="loading" :rows="4" animated />

    <VersionLinesTable
      v-else
      v-model:current-page="currentPage"
      :version-lines="versionLines"
      :show-pagination="showPagination"
      :page-size="pageSize"
      :total-elements="totalElements"
      :cloning-release-version="cloningReleaseVersion"
      :abandoning-dev-version-id="abandoningDevVersionId"
      :submitting="templatesStore.submitting"
      :line-label="lineLabel"
      :format-date-time="formatDateTime"
      :can-clone-row="canCloneRow"
      :can-abandon-row="canAbandonRow"
      :can-deactivate-row="canDeactivateRow"
      :can-restore-row="canRestoreRow"
      :on-row-click="onRowClick"
      @open="openVersionLine"
      @abandon="handleAbandon"
      @deactivate="handleVersionAction($event, 'deactivate')"
      @restore="handleVersionAction($event, 'restore')"
      @clone="handleClone"
    />
  </el-card>
</template>

<style scoped lang="scss">
.version-lines-card {
  margin-bottom: var(--space-4);
}

.card-header {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.card-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

.card-hint {
  margin: 0;
  font-size: var(--font-size-sm);
  color: var(--text-muted);
}
</style>
