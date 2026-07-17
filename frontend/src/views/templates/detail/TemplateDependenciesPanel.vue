<script setup lang="ts">
import { toRef } from 'vue'
import { useI18n } from 'vue-i18n'
import AppDataTable from '@/components/common/AppDataTable.vue'
import EmptyStatePanel from '@/components/common/EmptyStatePanel.vue'
import EntityLinkCell from '@/components/common/EntityLinkCell.vue'
import LoadErrorPanel from '@/components/common/LoadErrorPanel.vue'
import SectionPanelHeader from '@/components/common/SectionPanelHeader.vue'
import { useTemplateDependenciesPanel } from '@/views/templates/detail/useTemplateDependenciesPanel'
import type { TemplateDetail, TemplateVersionLineSummary } from '@/types/template'

const props = defineProps<{
  template: TemplateDetail
}>()

const { t } = useI18n()

const {
  masterDetailLink,
  contentModuleDetailLink,
  masterName,
  versionLines,
  versionLinesLoading,
  versionLinesError,
  masterPin,
  pinReleaseVersion,
  pinLoading,
  pinError,
  pinHashSummary,
  showPinnedMaster,
  showNotPinned,
  workingRevision,
  workingRevisionLoading,
  clauseReferences,
  clauseLoading,
  clauseError,
  anchorRows,
  retryVersionLines,
  retryPin,
  retryClauses,
  lineLabel,
  openVersionLine,
} = useTemplateDependenciesPanel(toRef(props, 'template'))

function onReleaseLineRowClick(row: unknown) {
  openVersionLine(row as TemplateVersionLineSummary)
}
</script>

<template>
  <div class="template-dependencies-panel" data-testid="template-dependencies-panel">
    <el-card
      shadow="never"
      class="dependency-section"
      data-testid="template-dependencies-master-section"
    >
      <SectionPanelHeader :title="t('templates.dependencies.masterRevision.title')" />
      <dl class="dependency-dl">
        <div>
          <dt>{{ t('templates.dependencies.masterRevision.masterLabel') }}</dt>
          <dd data-testid="template-dependencies-master-link">
            <EntityLinkCell
              :label="masterName ?? template.masterId"
              :to="masterDetailLink(template.masterId)"
            />
          </dd>
        </div>
      </dl>

      <LoadErrorPanel
        v-if="pinError"
        message-key="templates.dependencies.masterRevision.pinLoadError"
        data-testid="template-dependencies-pin-error"
        @retry="retryPin"
      />
      <el-skeleton v-else-if="pinLoading" :rows="2" animated data-testid="template-dependencies-pin-loading" />
      <template v-else-if="showPinnedMaster && masterPin">
        <p class="pin-status" data-testid="template-dependencies-pinned">
          {{ t('templates.dependencies.masterRevision.pinned') }}
        </p>
        <dl class="dependency-dl">
          <div v-if="pinReleaseVersion">
            <dt>{{ t('templates.dependencies.masterRevision.releaseVersion') }}</dt>
            <dd data-testid="template-dependencies-pin-release">{{ pinReleaseVersion }}</dd>
          </div>
          <div>
            <dt>{{ t('templates.dependencies.masterRevision.revisionId') }}</dt>
            <dd data-testid="template-dependencies-pin-revision-id">
              {{ masterPin.masterRevisionId }}
            </dd>
          </div>
          <div v-if="masterPin.revisionSequence != null">
            <dt>{{ t('templates.dependencies.masterRevision.revisionSequence') }}</dt>
            <dd data-testid="template-dependencies-pin-sequence">
              {{ masterPin.revisionSequence }}
            </dd>
          </div>
          <div v-if="pinHashSummary">
            <dt>{{ t('templates.dependencies.masterRevision.fileHashSummary') }}</dt>
            <dd class="mono" data-testid="template-dependencies-pin-hash">{{ pinHashSummary }}</dd>
          </div>
          <div v-if="masterPin.pinOrigin">
            <dt>{{ t('templates.dependencies.masterRevision.pinOrigin') }}</dt>
            <dd data-testid="template-dependencies-pin-origin">{{ masterPin.pinOrigin }}</dd>
          </div>
        </dl>
      </template>
      <template v-else-if="showNotPinned">
        <p class="not-pinned" data-testid="template-dependencies-not-pinned">
          {{ t('templates.dependencies.masterRevision.notPinnedUntilPublish') }}
        </p>
        <el-skeleton
          v-if="workingRevisionLoading"
          :rows="1"
          animated
          data-testid="template-dependencies-working-loading"
        />
        <p
          v-else-if="workingRevision"
          class="working-context"
          data-testid="template-dependencies-working-revision"
        >
          {{
            t('templates.dependencies.masterRevision.workingContext', {
              revisionId: workingRevision.id,
              sequence: workingRevision.revisionSequence ?? '—',
            })
          }}
        </p>
      </template>
    </el-card>

    <el-card
      shadow="never"
      class="dependency-section"
      data-testid="template-dependencies-anchors-section"
    >
      <SectionPanelHeader :title="t('templates.dependencies.anchors.title')" />
      <AppDataTable
        v-if="anchorRows.length > 0"
        :data="anchorRows"
        data-testid="template-dependencies-anchors-table"
      >
        <el-table-column
          prop="anchorId"
          :label="t('templates.dependencies.anchors.columns.anchorId')"
          min-width="160"
        />
        <el-table-column
          prop="declaredContentType"
          :label="t('templates.dependencies.anchors.columns.contentType')"
          min-width="140"
        />
        <el-table-column
          :label="t('templates.dependencies.anchors.columns.displayLabel')"
          min-width="160"
        >
          <template #default="{ row }">
            {{ row.displayLabel ?? t('templates.dependencies.anchors.noDisplayLabel') }}
          </template>
        </el-table-column>
      </AppDataTable>
      <EmptyStatePanel
        v-else
        title-key="templates.dependencies.anchors.empty"
        description-key="templates.dependencies.anchors.emptyDescription"
        data-testid="template-dependencies-anchors-empty"
      />
    </el-card>

    <el-card
      shadow="never"
      class="dependency-section"
      data-testid="template-dependencies-clauses-section"
    >
      <SectionPanelHeader :title="t('templates.dependencies.clauses.title')" />
      <LoadErrorPanel
        v-if="clauseError"
        message-key="templates.dependencies.clauses.loadError"
        data-testid="template-dependencies-clauses-error"
        @retry="retryClauses"
      />
      <el-skeleton
        v-else-if="clauseLoading"
        :rows="3"
        animated
        data-testid="template-dependencies-clauses-loading"
      />
      <AppDataTable
        v-else-if="clauseReferences.length > 0"
        :data="clauseReferences"
        data-testid="template-dependencies-clauses-table"
      >
        <el-table-column
          prop="referenceKey"
          :label="t('templates.dependencies.clauses.columns.referenceKey')"
          min-width="160"
        />
        <el-table-column
          :label="t('templates.dependencies.clauses.columns.module')"
          min-width="180"
        >
          <template #default="{ row }">
            <EntityLinkCell
              :label="row.moduleId"
              :to="contentModuleDetailLink(row.moduleId)"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="semanticVersion"
          :label="t('templates.dependencies.clauses.columns.semanticVersion')"
          width="120"
        />
        <el-table-column
          :label="t('templates.dependencies.clauses.columns.locked')"
          width="100"
        >
          <template #default="{ row }">
            {{
              row.locked
                ? t('templates.dependencies.clauses.lockedYes')
                : t('templates.dependencies.clauses.lockedNo')
            }}
          </template>
        </el-table-column>
        <el-table-column
          :label="t('templates.dependencies.clauses.columns.outOfDate')"
          min-width="160"
        >
          <template #default="{ row }">
            <span v-if="row.outOfDate" data-testid="template-dependencies-clause-outdated">
              {{
                t('templates.dependencies.clauses.outOfDateYes', {
                  latest: row.latestApprovedSemanticVersion ?? '—',
                })
              }}
            </span>
            <span v-else>{{ t('templates.dependencies.clauses.outOfDateNo') }}</span>
          </template>
        </el-table-column>
      </AppDataTable>
      <EmptyStatePanel
        v-else
        title-key="templates.dependencies.clauses.empty"
        description-key="templates.dependencies.clauses.emptyDescription"
        data-testid="template-dependencies-clauses-empty"
      />
    </el-card>

    <el-card
      shadow="never"
      class="dependency-section"
      data-testid="template-dependencies-release-lines-section"
    >
      <SectionPanelHeader :title="t('templates.dependencies.releaseLines.title')" />
      <p class="section-hint">{{ t('templates.dependencies.releaseLines.hint') }}</p>
      <LoadErrorPanel
        v-if="versionLinesError"
        message-key="templates.dependencies.releaseLines.loadError"
        data-testid="template-dependencies-release-lines-error"
        @retry="retryVersionLines"
      />
      <el-skeleton
        v-else-if="versionLinesLoading"
        :rows="3"
        animated
        data-testid="template-dependencies-release-lines-loading"
      />
      <AppDataTable
        v-else-if="versionLines.length > 0"
        activatable
        :data="versionLines"
        class="release-lines-table"
        data-testid="template-dependencies-release-lines-table"
        @row-click="onReleaseLineRowClick"
      >
        <el-table-column
          :label="t('templates.dependencies.releaseLines.columns.line')"
          min-width="200"
        >
          <template #default="{ row }">
            <button
              type="button"
              class="line-nav-button"
              data-testid="template-dependencies-release-line-link"
              @click.stop="openVersionLine(row)"
            >
              {{ lineLabel(row) }}
            </button>
          </template>
        </el-table-column>
        <el-table-column
          prop="lineKind"
          :label="t('templates.dependencies.releaseLines.columns.lineKind')"
          width="120"
        />
        <el-table-column
          prop="lifecycleStatus"
          :label="t('templates.dependencies.releaseLines.columns.status')"
          width="140"
        />
        <el-table-column
          :label="t('templates.dependencies.releaseLines.columns.defaultRoute')"
          width="120"
        >
          <template #default="{ row }">
            {{
              row.defaultRouteTarget
                ? t('templates.dependencies.releaseLines.defaultRouteYes')
                : t('templates.dependencies.releaseLines.defaultRouteNo')
            }}
          </template>
        </el-table-column>
      </AppDataTable>
      <EmptyStatePanel
        v-else
        title-key="templates.dependencies.releaseLines.empty"
        description-key="templates.dependencies.releaseLines.emptyDescription"
        data-testid="template-dependencies-release-lines-empty"
      />
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.template-dependencies-panel {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.dependency-section {
  margin: 0;
}

.dependency-dl {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: var(--space-3);
  margin: 0 0 var(--space-3);

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

.mono {
  font-family: var(--font-mono, ui-monospace, monospace);
  font-weight: 400;
  word-break: break-all;
}

.pin-status {
  margin: 0 0 var(--space-2);
  font-weight: 600;
  color: var(--text-primary);
}

.not-pinned {
  margin: 0 0 var(--space-2);
  font-weight: 600;
  color: var(--text-muted);
}

.working-context {
  margin: 0;
  color: var(--text-muted);
  font-size: 0.9rem;
}

.section-hint {
  margin: 0 0 var(--space-3);
  color: var(--text-muted);
  font-size: 0.9rem;
}

.line-nav-button {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--color-primary);
  cursor: pointer;
  font: inherit;
  text-align: left;

  &:hover {
    text-decoration: underline;
  }

  &:focus-visible {
    outline: 2px solid var(--color-primary);
    outline-offset: 2px;
  }
}

.release-lines-table {
  :deep(.el-table__row) {
    cursor: pointer;
  }
}
</style>
