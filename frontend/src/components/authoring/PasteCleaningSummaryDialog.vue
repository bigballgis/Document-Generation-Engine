<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { PasteCleaningSummary } from '@/types/template'

const props = defineProps<{
  modelValue: boolean
  summary: PasteCleaningSummary | null
  blocked: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  accept: []
  cancel: []
  undo: []
}>()

const { t, te } = useI18n()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

function categoryLabel(category: string): string {
  const key = `templates.structuredEditor.pasteSummary.categories.${category}`
  return te(key) ? t(key) : category
}

function itemLabel(messageKey: string): string {
  return te(messageKey) ? t(messageKey) : messageKey
}

function handleCancel() {
  emit('cancel')
  emit('undo')
  visible.value = false
}

function handleAccept() {
  emit('accept')
  visible.value = false
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="t('templates.structuredEditor.pasteSummary.title')"
    width="560px"
    :close-on-click-modal="false"
    data-testid="paste-cleaning-summary-dialog"
  >
    <p class="intro">{{ t('templates.structuredEditor.pasteSummary.intro') }}</p>

    <el-alert
      v-if="blocked"
      type="error"
      :title="t('templates.structuredEditor.pasteSummary.blockedTitle')"
      :description="t('templates.structuredEditor.pasteSummary.blockedDescription')"
      show-icon
      class="blocked-alert"
    />

    <dl v-if="summary" class="counts">
      <div>
        <dt>{{ t('templates.structuredEditor.pasteSummary.counts.transformed') }}</dt>
        <dd>{{ summary.transformedCount }}</dd>
      </div>
      <div>
        <dt>{{ t('templates.structuredEditor.pasteSummary.counts.removed') }}</dt>
        <dd>{{ summary.removedCount }}</dd>
      </div>
      <div>
        <dt>{{ t('templates.structuredEditor.pasteSummary.counts.warning') }}</dt>
        <dd>{{ summary.warningCount }}</dd>
      </div>
      <div>
        <dt>{{ t('templates.structuredEditor.pasteSummary.counts.blocked') }}</dt>
        <dd>{{ summary.blockedCount }}</dd>
      </div>
    </dl>

    <el-table v-if="summary?.items.length" :data="summary.items" size="small" class="summary-table">
      <el-table-column :label="t('templates.structuredEditor.pasteSummary.categoryColumn')" width="140">
        <template #default="{ row }">
          {{ categoryLabel(row.category) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('templates.structuredEditor.pasteSummary.detailColumn')">
        <template #default="{ row }">
          {{ itemLabel(row.messageKey) }}
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <el-button data-testid="paste-summary-cancel" @click="handleCancel">
        {{ t('templates.structuredEditor.pasteSummary.cancel') }}
      </el-button>
      <el-button data-testid="paste-summary-undo" @click="handleCancel">
        {{ t('templates.structuredEditor.pasteSummary.undo') }}
      </el-button>
      <el-button
        type="primary"
        data-testid="paste-summary-accept"
        :disabled="blocked"
        @click="handleAccept"
      >
        {{ t('templates.structuredEditor.pasteSummary.accept') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.intro {
  margin: 0 0 1rem;
  color: var(--text-muted);
}

.blocked-alert {
  margin-bottom: 1rem;
}

.counts {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0.75rem;
  margin: 0 0 1rem;

  dt {
    margin: 0;
    font-size: 0.85rem;
    color: var(--text-muted);
  }

  dd {
    margin: 0.25rem 0 0;
    font-weight: 600;
  }
}

.summary-table {
  width: 100%;
}
</style>
