<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import SectionPanelHeader from '@/components/common/SectionPanelHeader.vue'
import { useTemplatePanelDataStore } from '@/stores/templatePanelData'
import { ElMessage } from 'element-plus'

const props = withDefaults(
  defineProps<{
    templateId: string
    refreshToken?: number
    compact?: boolean
  }>(),
  {
    compact: false,
  },
)

const { t } = useI18n()
const panelDataStore = useTemplatePanelDataStore()
const entry = computed(() => panelDataStore.getEntry(props.templateId))
const loading = computed(() => entry.value.loadingChangeDiff)
const summary = computed(() => entry.value.changeDiff)

const dimensionLabelKey: Record<string, string> = {
  CONTENT: 'templates.changeDiff.dimensions.content',
  ANCHORS: 'templates.changeDiff.dimensions.anchors',
  VARIABLES: 'templates.changeDiff.dimensions.variables',
  RULES: 'templates.changeDiff.dimensions.rules',
  CONTRACT_SUMMARY: 'templates.changeDiff.dimensions.contractSummary',
}

async function loadChangeDiff() {
  try {
    await panelDataStore.fetchChangeDiff(props.templateId)
  } catch {
    ElMessage.error(t('templates.changeDiff.error.load'))
  }
}

function dimensionLabel(code: string): string {
  const key = dimensionLabelKey[code]
  return key ? t(key) : code
}

function dimensionChangeCount(dimension: NonNullable<typeof summary.value>['dimensions'][number]): number {
  return dimension.added.length + dimension.removed.length + dimension.modified.length
}

const hasAnyChanges = computed(() => summary.value?.hasChanges ?? false)

onMounted(() => {
  void loadChangeDiff()
})

watch(
  () => props.refreshToken,
  () => {
    void loadChangeDiff()
  },
)
</script>

<template>
  <div v-loading="loading" class="change-diff-panel">
    <SectionPanelHeader
      v-if="!compact"
      :title="t('templates.changeDiff.title')"
      :help-title="t('templates.changeDiff.helpTitle')"
      :help-content="t('templates.changeDiff.helpContent')"
    >
      <template #actions>
        <el-button link type="primary" @click="loadChangeDiff">
          {{ t('templates.changeDiff.refresh') }}
        </el-button>
      </template>
    </SectionPanelHeader>
    <div v-else class="change-diff-panel__toolbar">
      <el-button link type="primary" @click="loadChangeDiff">
        {{ t('templates.changeDiff.refresh') }}
      </el-button>
    </div>

    <template v-if="summary">
      <el-alert
        :type="hasAnyChanges ? 'warning' : 'success'"
        :closable="false"
        show-icon
        class="change-diff-alert"
      >
        <template #title>
          {{
            hasAnyChanges
              ? t('templates.changeDiff.status.hasChanges', { count: summary.totalChangeCount })
              : t('templates.changeDiff.status.noChanges')
          }}
        </template>
      </el-alert>

      <p v-if="summary.baselineReleaseVersion && !compact" class="baseline-hint">
        {{
          t('templates.changeDiff.baselineHint', {
            version: summary.baselineReleaseVersion,
          })
        }}
      </p>

      <el-empty
        v-if="!summary.dimensions.length"
        :description="t('templates.changeDiff.empty')"
      />

      <el-collapse v-else class="dimension-collapse">
        <el-collapse-item
          v-for="dimension in summary.dimensions"
          :key="dimension.dimension"
          :name="dimension.dimension"
        >
          <template #title>
            <span class="dimension-title">
              {{ dimensionLabel(dimension.dimension) }}
              <el-tag size="small" :type="dimensionChangeCount(dimension) ? 'warning' : 'info'">
                {{ dimensionChangeCount(dimension) }}
              </el-tag>
            </span>
          </template>

          <section v-if="dimension.added.length" class="diff-section">
            <h4>{{ t('templates.changeDiff.added') }}</h4>
            <ul>
              <li v-for="item in dimension.added" :key="`add-${item}`">{{ item }}</li>
            </ul>
          </section>

          <section v-if="dimension.removed.length" class="diff-section">
            <h4>{{ t('templates.changeDiff.removed') }}</h4>
            <ul>
              <li v-for="item in dimension.removed" :key="`remove-${item}`">{{ item }}</li>
            </ul>
          </section>

          <section v-if="dimension.modified.length" class="diff-section">
            <h4>{{ t('templates.changeDiff.modified') }}</h4>
            <ul>
              <li v-for="item in dimension.modified" :key="`${item.key}-${item.changeType}`">
                <strong>{{ item.key }}</strong>
                ({{ item.changeType }}): {{ item.summary }}
              </li>
            </ul>
          </section>

          <p
            v-if="
              !dimension.added.length &&
              !dimension.removed.length &&
              !dimension.modified.length
            "
            class="no-dimension-changes"
          >
            {{ t('templates.changeDiff.noDimensionChanges') }}
          </p>
        </el-collapse-item>
      </el-collapse>
    </template>
  </div>
</template>

<style scoped lang="scss">
.change-diff-panel__toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 0.5rem;
}

.change-diff-alert {
  margin: 1rem 0;
}

.baseline-hint {
  margin: 0 0 1rem;
  color: var(--text-muted);
  font-size: 0.875rem;
}

.dimension-collapse {
  border: none;
}

.dimension-title {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
}

.diff-section {
  margin-bottom: 0.75rem;

  h4 {
    margin: 0 0 0.35rem;
    font-size: 0.875rem;
    color: var(--text-muted);
  }

  ul {
    margin: 0;
    padding-left: 1.25rem;
  }
}

.no-dimension-changes {
  margin: 0;
  color: var(--text-muted);
  font-size: 0.875rem;
}
</style>
