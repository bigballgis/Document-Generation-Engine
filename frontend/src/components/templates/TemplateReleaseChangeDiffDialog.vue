<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import ChangeDiffHumanReadableList from '@/components/templates/ChangeDiffHumanReadableList.vue'
import * as templatesApi from '@/api/templates'
import type { ChangeDiffSummary } from '@/types/template'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  modelValue: boolean
  templateId: string
  releaseVersionA: string | null
  releaseVersionB: string | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const { t } = useI18n()
const loading = ref(false)
const summary = ref<ChangeDiffSummary | null>(null)

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

async function loadDiff() {
  if (!props.releaseVersionA || !props.releaseVersionB) {
    summary.value = null
    return
  }
  loading.value = true
  try {
    summary.value = await templatesApi.fetchReleaseChangeDiff(
      props.templateId,
      props.releaseVersionA,
      props.releaseVersionB,
    )
  } catch {
    summary.value = null
    ElMessage.error(t('templates.versions.compareLoadError'))
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.modelValue, props.releaseVersionA, props.releaseVersionB, props.templateId] as const,
  ([open]) => {
    if (open) {
      void loadDiff()
    }
  },
)
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="t('templates.versions.compareTitle')"
    width="640px"
    :close-on-click-modal="false"
  >
    <p class="compare-intro">
      {{
        t('templates.versions.compareDescription', {
          a: releaseVersionA ?? '—',
          b: releaseVersionB ?? '—',
        })
      }}
    </p>
    <div v-loading="loading">
      <ChangeDiffHumanReadableList :change-diff-summary="summary" />
    </div>
    <template #footer>
      <el-button type="primary" @click="visible = false">{{ t('common.cancel') }}</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.compare-intro {
  margin: 0 0 0.75rem;
  color: var(--text-muted);
}
</style>
