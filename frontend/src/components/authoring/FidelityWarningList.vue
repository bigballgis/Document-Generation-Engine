<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FidelityWarning } from '@/types/template'
import {
  DEFAULT_FIDELITY_WARNING_FILTERS,
  filterFidelityWarnings,
  uniqueArtifacts,
  uniqueWarningCodes,
} from '@/utils/fidelityWarningFilters'

const props = defineProps<{
  warnings: FidelityWarning[]
  artifactHint?: string | null
}>()

const emit = defineEmits<{
  markViewed: [index: number]
}>()

const { t, te } = useI18n()

const localWarnings = ref<FidelityWarning[]>([])
const filters = ref({ ...DEFAULT_FIDELITY_WARNING_FILTERS })

watch(
  () => props.warnings,
  (value) => {
    localWarnings.value = value.map((warning) => ({
      code: warning.code ?? 'UNKNOWN',
      messageKey: warning.messageKey,
      location: warning.location ?? null,
      artifact: warning.artifact ?? props.artifactHint ?? null,
      viewed: warning.viewed ?? false,
    }))
  },
  { immediate: true, deep: true },
)

const warningCodeOptions = computed(() => uniqueWarningCodes(localWarnings.value))
const artifactOptions = computed(() => uniqueArtifacts(localWarnings.value))

const filteredWarnings = computed(() =>
  filterFidelityWarnings(localWarnings.value, filters.value),
)

function warningLabel(messageKey: string): string {
  return te(messageKey) ? t(messageKey) : messageKey
}

function severityTagType(code: string): 'danger' | 'warning' {
  return code.includes('UNRESOLVED') || code.includes('MISSING') ? 'danger' : 'warning'
}

function markViewed(index: number) {
  const warning = filteredWarnings.value[index]
  if (!warning) {
    return
  }
  const globalIndex = localWarnings.value.findIndex(
    (item) =>
      item.code === warning.code &&
      item.messageKey === warning.messageKey &&
      item.location === warning.location,
  )
  if (globalIndex >= 0) {
    localWarnings.value[globalIndex] = { ...localWarnings.value[globalIndex], viewed: true }
    emit('markViewed', globalIndex)
  }
}

function resetFilters() {
  filters.value = { ...DEFAULT_FIDELITY_WARNING_FILTERS }
}
</script>

<template>
  <div class="fidelity-warning-list" data-testid="fidelity-warning-list">
    <div class="filters">
      <el-input
        v-model="filters.warningCode"
        data-testid="filter-warning-code"
        :placeholder="t('templates.preview.warningFilters.warningCode')"
        clearable
      />
      <el-input
        v-model="filters.location"
        data-testid="filter-location"
        :placeholder="t('templates.preview.warningFilters.location')"
        clearable
      />
      <el-select
        v-model="filters.artifact"
        data-testid="filter-artifact"
        clearable
        filterable
        :placeholder="t('templates.preview.warningFilters.artifact')"
      >
        <el-option v-for="artifact in artifactOptions" :key="artifact" :label="artifact" :value="artifact" />
      </el-select>
      <el-select v-model="filters.viewed" data-testid="filter-viewed">
        <el-option :label="t('templates.preview.warningFilters.viewedAll')" value="all" />
        <el-option :label="t('templates.preview.warningFilters.viewedOnly')" value="viewed" />
        <el-option :label="t('templates.preview.warningFilters.unviewedOnly')" value="unviewed" />
      </el-select>
      <el-button @click="resetFilters">{{ t('templates.preview.warningFilters.reset') }}</el-button>
    </div>

    <el-empty
      v-if="!filteredWarnings.length"
      :description="t('templates.preview.noWarnings')"
    />

    <el-table v-else :data="filteredWarnings" size="small" class="warning-table">
      <el-table-column :label="t('templates.preview.warningFilters.warningCode')" prop="code" min-width="160" />
      <el-table-column :label="t('templates.preview.warningFilters.location')" min-width="140">
        <template #default="{ row }">
          {{ row.location ?? t('common.notAvailable') }}
        </template>
      </el-table-column>
      <el-table-column :label="t('templates.preview.warningFilters.artifact')" min-width="120">
        <template #default="{ row }">
          {{ row.artifact ?? t('common.notAvailable') }}
        </template>
      </el-table-column>
      <el-table-column :label="t('templates.preview.warningFilters.message')" min-width="220">
        <template #default="{ row }">
          <el-tag :type="severityTagType(row.code)" size="small" class="code-tag">
            {{ row.code }}
          </el-tag>
          {{ warningLabel(row.messageKey) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('templates.preview.warningFilters.viewed')" width="120">
        <template #default="{ row, $index }">
          <el-tag :type="row.viewed ? 'success' : 'info'" size="small">
            {{
              row.viewed
                ? t('templates.preview.warningFilters.viewedState.viewed')
                : t('templates.preview.warningFilters.viewedState.unviewed')
            }}
          </el-tag>
          <el-button
            v-if="!row.viewed"
            link
            type="primary"
            data-testid="mark-warning-viewed"
            @click="markViewed($index)"
          >
            {{ t('templates.preview.warningFilters.markViewed') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <p v-if="warningCodeOptions.length" class="filter-hint">
      {{ t('templates.preview.warningFilters.availableCodes', { count: warningCodeOptions.length }) }}
    </p>
  </div>
</template>

<style scoped lang="scss">
.filters {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.warning-table {
  width: 100%;
}

.code-tag {
  margin-right: 0.5rem;
}

.filter-hint {
  margin: 0.75rem 0 0;
  font-size: 0.85rem;
  color: var(--text-muted);
}
</style>
