<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import * as templatesApi from '@/api/templates'
import type { AnchorBinding, PreviewRecord } from '@/types/template'

const props = defineProps<{
  templateId: string
  bindings: AnchorBinding[]
  preview: PreviewRecord | null
}>()

const { t, te } = useI18n()
const loading = ref(false)
const latestPreview = ref<PreviewRecord | null>(props.preview)

watch(
  () => props.preview,
  (value) => {
    latestPreview.value = value
  },
)

const comparisonRows = computed(() =>
  props.bindings.map((binding) => ({
    anchorId: binding.anchorId,
    bindingType: binding.declaredContentType,
    previewStatus: latestPreview.value?.status ?? '—',
  })),
)

async function refreshPreview() {
  if (!latestPreview.value?.previewId) {
    return
  }
  loading.value = true
  try {
    latestPreview.value = await templatesApi.getPreview(
      props.templateId,
      latestPreview.value.previewId,
    )
  } finally {
    loading.value = false
  }
}

function warningLabel(messageKey: string) {
  return te(messageKey) ? t(messageKey) : messageKey
}
</script>

<template>
  <div class="preview-panel">
    <template v-if="latestPreview">
      <dl class="preview-meta">
        <div>
          <dt>{{ t('templates.preview.previewId') }}</dt>
          <dd>{{ latestPreview.previewId }}</dd>
        </div>
        <div>
          <dt>{{ t('templates.preview.status') }}</dt>
          <dd>{{ latestPreview.status }}</dd>
        </div>
        <div>
          <dt>{{ t('templates.preview.comparisonSummary') }}</dt>
          <dd>{{ latestPreview.comparisonSummary ?? t('templates.preview.noComparison') }}</dd>
        </div>
        <div v-if="latestPreview.testDataSetId">
          <dt>{{ t('templates.preview.testDataSetId') }}</dt>
          <dd>{{ latestPreview.testDataSetId }}</dd>
        </div>
      </dl>

      <el-button :loading="loading" @click="refreshPreview">
        {{ t('templates.preview.refresh') }}
      </el-button>

      <h3>{{ t('templates.preview.comparisonTitle') }}</h3>
      <el-table :data="comparisonRows" stripe>
        <el-table-column prop="anchorId" :label="t('templates.authoring.anchorId')" />
        <el-table-column prop="bindingType" :label="t('templates.authoring.contentType')" />
        <el-table-column prop="previewStatus" :label="t('templates.preview.previewStatusColumn')" />
      </el-table>

      <h3>{{ t('templates.preview.warningsTitle') }}</h3>
      <el-empty
        v-if="!latestPreview.fidelityWarnings.length"
        :description="t('templates.preview.noWarnings')"
      />
      <ul v-else class="warning-list">
        <li v-for="warning in latestPreview.fidelityWarnings" :key="warning.messageKey">
          {{ warningLabel(warning.messageKey) }}
        </li>
      </ul>
    </template>
    <el-empty v-else :description="t('templates.preview.empty')" />
  </div>
</template>

<style scoped lang="scss">
.preview-meta {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1rem;
  margin: 0 0 1rem;

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

h3 {
  margin: 1.25rem 0 0.75rem;
  font-size: 1rem;
}

.warning-list {
  margin: 0;
  padding-left: 1.25rem;
}
</style>
