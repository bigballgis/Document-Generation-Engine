<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { useI18n } from 'vue-i18n'
import type { FidelityWarning } from '@/types/template'
import {
  DEFAULT_FIDELITY_WARNING_FILTERS,
  filterFidelityWarnings,
  uniqueArtifacts,
  uniqueWarningCodes,
} from '@/utils/fidelityWarningFilters'
import { buildFidelityBindingEditLink } from '@/utils/fidelityBindingEditLink'
import {
  friendlyArtifactLabel,
  resolveFidelityEditAnchorId,
} from '@/utils/fidelityArtifactLabel'

const props = defineProps<{
  warnings: FidelityWarning[]
  artifactHint?: string | null
  templateId?: string
  devVersionId?: string
  markingViewedIndex?: number | null
}>()

const emit = defineEmits<{
  markViewed: [index: number]
}>()

const { t, te } = useI18n()

const localWarnings = ref<FidelityWarning[]>([])
const filters = ref({ ...DEFAULT_FIDELITY_WARNING_FILTERS })
const expandedCodes = ref<Record<number, boolean>>({})

watch(
  () => props.warnings,
  (value) => {
    localWarnings.value = value.map((warning) => ({
      code: warning.code ?? 'UNKNOWN',
      messageKey: warning.messageKey,
      location: warning.location ?? null,
      // Keep payload artifact only — never merge storage-key hints into edit targets.
      artifact: warning.artifact ?? null,
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

const hasActiveFilters = computed(() => {
  const f = filters.value
  return (
    f.warningCode.trim() !== '' ||
    f.location.trim() !== '' ||
    f.artifact.trim() !== '' ||
    f.viewed !== 'all'
  )
})

const emptyDescriptionKey = computed(() =>
  localWarnings.value.length === 0
    ? 'templates.preview.noWarnings'
    : hasActiveFilters.value
      ? 'templates.preview.noMatchingWarnings'
      : 'templates.preview.noWarnings',
)

function humanWarningLabel(warning: FidelityWarning): string {
  if (te(warning.messageKey)) {
    return t(warning.messageKey)
  }
  const codeKey = `templates.preview.fidelityMessages.${warning.code}`
  if (te(codeKey)) {
    return t(codeKey)
  }
  return warning.messageKey
}

function severityTagType(code: string): 'danger' | 'warning' {
  return code.includes('UNRESOLVED') || code.includes('MISSING') ? 'danger' : 'warning'
}

function globalIndexForFiltered(index: number): number {
  const warning = filteredWarnings.value[index]
  if (!warning) {
    return -1
  }
  return localWarnings.value.findIndex(
    (item) =>
      item.code === warning.code &&
      item.messageKey === warning.messageKey &&
      item.location === warning.location,
  )
}

function markViewed(index: number) {
  const globalIndex = globalIndexForFiltered(index)
  if (globalIndex >= 0) {
    emit('markViewed', globalIndex)
  }
}

function bindingEditLink(warning: FidelityWarning): string | null {
  if (!props.templateId || !props.devVersionId) {
    return null
  }
  return buildFidelityBindingEditLink({
    templateId: props.templateId,
    devVersionId: props.devVersionId,
    anchorId: resolveFidelityEditAnchorId(warning),
  })
}

function artifactColumnLabel(warning: FidelityWarning): string {
  return friendlyArtifactLabel(
    warning.artifact,
    props.artifactHint,
    t('common.notAvailable'),
  )
}

function toggleTechnicalDetails(index: number) {
  expandedCodes.value = {
    ...expandedCodes.value,
    [index]: !expandedCodes.value[index],
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
      :description="t(emptyDescriptionKey)"
    />

    <el-table v-else :data="filteredWarnings" size="small" class="warning-table">
      <el-table-column :label="t('templates.preview.warningFilters.message')" min-width="280">
        <template #default="{ row, $index }">
          <p class="human-message" data-testid="fidelity-warning-human-message">
            {{ humanWarningLabel(row) }}
          </p>
          <button
            type="button"
            class="technical-toggle"
            data-testid="fidelity-warning-technical-toggle"
            @click="toggleTechnicalDetails($index)"
          >
            {{
              expandedCodes[$index]
                ? t('templates.preview.warningFilters.hideTechnical')
                : t('templates.preview.warningFilters.showTechnical')
            }}
          </button>
          <div
            v-if="expandedCodes[$index]"
            class="technical-details"
            data-testid="fidelity-warning-technical-details"
          >
            <el-tag :type="severityTagType(row.code)" size="small">{{ row.code }}</el-tag>
            <span class="technical-location">{{ row.location ?? t('common.notAvailable') }}</span>
          </div>
          <RouterLink
            v-if="bindingEditLink(row)"
            :to="bindingEditLink(row)!"
            class="binding-edit-link"
            data-testid="fidelity-warning-edit-binding"
          >
            {{ t('templates.preview.warningFilters.editBinding') }}
          </RouterLink>
        </template>
      </el-table-column>
      <el-table-column :label="t('templates.preview.warningFilters.artifact')" min-width="120">
        <template #default="{ row }">
          {{ artifactColumnLabel(row) }}
        </template>
      </el-table-column>
      <el-table-column :label="t('templates.preview.warningFilters.viewed')" width="140">
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
            :loading="markingViewedIndex === globalIndexForFiltered($index)"
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

.human-message {
  margin: 0 0 0.35rem;
  line-height: 1.45;
}

.technical-toggle {
  margin: 0;
  padding: 0;
  border: 0;
  background: none;
  color: var(--color-primary);
  font-size: 0.85rem;
  cursor: pointer;
}

.technical-details {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.35rem;
  font-size: 0.85rem;
  color: var(--text-muted);
}

.binding-edit-link {
  display: inline-block;
  margin-top: 0.35rem;
  font-size: 0.85rem;
}

.filter-hint {
  margin: 0.75rem 0 0;
  font-size: 0.85rem;
  color: var(--text-muted);
}
</style>
