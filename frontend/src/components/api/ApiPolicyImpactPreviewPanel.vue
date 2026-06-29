<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ApiPolicyImpactPreview } from '@/types/template'

const props = defineProps<{
  preview: ApiPolicyImpactPreview | null
  loading?: boolean
  errorMessage?: string
}>()

const { t, te } = useI18n()

const summaryText = computed(() => {
  if (!props.preview?.summaryMessageKey) {
    return ''
  }
  return te(props.preview.summaryMessageKey)
    ? t(props.preview.summaryMessageKey)
    : props.preview.summaryMessageKey
})

const warningMessages = computed(() =>
  (props.preview?.warnings ?? []).map((key) => (te(key) ? t(key) : key)),
)

const changedAreasText = computed(() => {
  const areas = props.preview?.changedAreas ?? []
  if (areas.length === 0) {
    return t('apiPolicy.detail.impact.noChanges')
  }
  return areas
    .map((area) => (te(`apiPolicy.detail.domains.${area}`) ? t(`apiPolicy.detail.domains.${area}`) : area))
    .join(', ')
})
</script>

<template>
  <section class="impact-panel" aria-live="polite">
    <header class="impact-panel__header">
      <h3>{{ t('apiPolicy.detail.impact.title') }}</h3>
      <el-tag v-if="preview?.blocking" type="danger" effect="plain">
        {{ t('apiPolicy.detail.impact.hardBlock') }}
      </el-tag>
      <el-tag v-else-if="preview && preview.warnings.length > 0" type="warning" effect="plain">
        {{ t('apiPolicy.detail.impact.warning') }}
      </el-tag>
      <el-tag v-else-if="preview" type="success" effect="plain">
        {{ t('apiPolicy.detail.impact.safe') }}
      </el-tag>
    </header>

    <el-skeleton v-if="loading" :rows="3" animated />

    <el-alert
      v-else-if="errorMessage"
      type="error"
      :title="errorMessage"
      show-icon
      :closable="false"
    />

    <el-empty
      v-else-if="!preview"
      :description="t('apiPolicy.detail.impact.empty')"
    />

    <template v-else>
      <p class="impact-panel__summary">{{ summaryText }}</p>
      <dl class="impact-panel__meta">
        <div>
          <dt>{{ t('apiPolicy.detail.policyVersion') }}</dt>
          <dd>
            {{
              t('apiPolicy.detail.impact.versionChange', {
                currentVersion: preview.currentPolicyVersion,
                nextVersion: preview.nextPolicyVersion,
              })
            }}
          </dd>
        </div>
        <div>
          <dt>{{ t('apiPolicy.detail.impact.changedAreas') }}</dt>
          <dd>{{ changedAreasText }}</dd>
        </div>
      </dl>

      <ul v-if="warningMessages.length > 0" class="impact-panel__warnings">
        <li v-for="(message, index) in warningMessages" :key="`${message}-${index}`">
          {{ message }}
        </li>
      </ul>

      <p v-if="preview.contractDiffSummary" class="impact-panel__detail">
        {{ preview.contractDiffSummary }}
      </p>
      <p v-if="preview.idempotencyImpactSummary" class="impact-panel__detail">
        {{
          te(preview.idempotencyImpactSummary)
            ? t(preview.idempotencyImpactSummary)
            : preview.idempotencyImpactSummary
        }}
      </p>
    </template>
  </section>
</template>

<style scoped lang="scss">
.impact-panel {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 1rem 1.25rem;
  background: var(--surface-muted, #fafbfc);
}

.impact-panel__header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.75rem;

  h3 {
    margin: 0;
    font-size: 1rem;
    font-weight: 600;
  }
}

.impact-panel__summary {
  margin: 0 0 0.75rem;
  color: var(--text-primary);
}

.impact-panel__meta {
  display: grid;
  gap: 0.5rem;
  margin: 0 0 0.75rem;

  div {
    display: grid;
    grid-template-columns: 10rem 1fr;
    gap: 0.75rem;
  }

  dt {
    margin: 0;
    color: var(--text-muted);
    font-weight: 500;
  }

  dd {
    margin: 0;
  }
}

.impact-panel__warnings {
  margin: 0 0 0.75rem;
  padding-left: 1.25rem;
  color: var(--warning-text, #b88230);
}

.impact-panel__detail {
  margin: 0.25rem 0 0;
  color: var(--text-muted);
  font-size: 0.875rem;
}
</style>
