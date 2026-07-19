<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import RenderedComparePanel from '@/components/templates/RenderedComparePanel.vue'
import type { PreviewRunSummary } from '@/types/template'

const props = defineProps<{
  modelValue: boolean
  templateId: string
  runA: PreviewRunSummary | null
  runB: PreviewRunSummary | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const { t } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="t('templates.previewHistory.renderedCompare.title')"
    width="92%"
    top="4vh"
    :close-on-click-modal="false"
    class="rendered-compare-dialog"
    data-testid="rendered-compare-dialog"
  >
    <RenderedComparePanel
      v-if="runA && runB"
      :template-id="templateId"
      :run-a="runA"
      :run-b="runB"
    />
    <template #footer>
      <el-button type="primary" data-testid="rendered-compare-close" @click="visible = false">
        {{ t('common.cancel') }}
      </el-button>
    </template>
  </el-dialog>
</template>
