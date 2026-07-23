<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import EntityLinkCell from '@/components/common/EntityLinkCell.vue'
import { useEntityLinkTargets } from '@/composables/useEntityLinkTargets'
import type { MasterImpactAnalysis, MasterReferencedTemplate } from '@/types/master'

const props = defineProps<{
  impact: MasterImpactAnalysis | null
  loading?: boolean
}>()

const { t } = useI18n()
const { templateDetailLink } = useEntityLinkTargets()

const referencedTemplates = computed((): MasterReferencedTemplate[] => {
  const impact = props.impact
  if (!impact) {
    return []
  }
  if (impact.referencedTemplates?.length) {
    return impact.referencedTemplates
  }
  return (impact.referencedTemplateIds ?? []).map((templateId) => ({
    templateId,
    name: templateId,
  }))
})

const hasReferences = computed(() => referencedTemplates.value.length > 0)

function referencedTemplateLabel(template: MasterReferencedTemplate): string {
  const name = template.name?.trim()
  if (name) {
    return name
  }
  const id = template.templateId?.trim()
  return id || '—'
}
</script>

<template>
  <el-card shadow="never" class="impact-panel" data-testid="master-impact-panel">
    <template #header>
      <span>{{ t('masters.impact.title') }}</span>
    </template>
    <el-skeleton v-if="loading" :rows="3" animated />
    <template v-else-if="impact">
      <p
        v-if="impact.retestRequired"
        class="retest-prompt"
        data-testid="master-impact-retest-required"
      >
        {{ t('masters.impact.retestRequired') }}
      </p>
      <p v-else class="retest-prompt muted" data-testid="master-impact-retest-not-required">
        {{ t('masters.impact.retestNotRequired') }}
      </p>
      <div v-if="hasReferences" class="template-list" data-testid="master-impact-template-list">
        <p class="list-label">{{ t('masters.impact.referencedTemplates') }}</p>
        <ul>
          <li v-for="template in referencedTemplates" :key="template.templateId">
            <EntityLinkCell
              data-testid="master-impact-template-cell"
              :label="referencedTemplateLabel(template)"
              :subtitle="template.externalId"
              :to="templateDetailLink(template.templateId)"
            />
          </li>
        </ul>
      </div>
      <p v-else class="muted" data-testid="master-impact-empty">
        {{ t('masters.impact.noReferencedTemplates') }}
      </p>
    </template>
    <p v-else class="muted" data-testid="master-impact-unavailable">
      {{ t('masters.impact.unavailable') }}
    </p>
  </el-card>
</template>

<style scoped lang="scss">
.impact-panel {
  margin-top: 1rem;
}

.retest-prompt {
  margin: 0 0 1rem;
  font-weight: 600;
}

.list-label {
  margin: 0 0 0.5rem;
  font-weight: 600;
}

.template-list ul {
  margin: 0;
  padding-left: 1.25rem;
}

.muted {
  margin: 0;
  color: var(--text-muted);
}
</style>
