<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import * as templatesApi from '@/api/templates'
import type { ChangeDiffSummary } from '@/types/template'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  templateId: string
  refreshToken?: number
}>()

const { t } = useI18n()
const loading = ref(false)
const summary = ref<ChangeDiffSummary | null>(null)

const dimensionLabelKey: Record<string, string> = {
  CONTENT: 'templates.changeDiff.dimensions.content',
  ANCHORS: 'templates.changeDiff.dimensions.anchors',
  VARIABLES: 'templates.changeDiff.dimensions.variables',
  RULES: 'templates.changeDiff.dimensions.rules',
  CONTRACT_SUMMARY: 'templates.changeDiff.dimensions.contractSummary',
}

async function loadChangeDiff() {
  loading.value = true
  try {
    summary.value = await templatesApi.fetchChangeDiff(props.templateId)
  } catch {
    ElMessage.error(t('templates.changeDiff.error.load'))
  } finally {
    loading.value = false
  }
}

function dimensionLabel(code: string): string {
  const key = dimensionLabelKey[code]
  return key ? t(key) : code
}

function dimensionChangeCount(dimension: ChangeDiffSummary['dimensions'][number]): number {
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
    <div class="change-diff-header">
      <h3>{{ t('templates.changeDiff.title') }}</h3>
      <el-button link type="primary" @click="loadChangeDiff">
        {{ t('templates.changeDiff.refresh') }}
      </el-button>
    </div>
    <p>{{ t('templates.changeDiff.description') }}</p>

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

      <p v-if="summary.baselineReleaseVersion" class="baseline-hint">
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
.change-diff-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.5rem;

  h3 {
    margin: 0;
  }
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
