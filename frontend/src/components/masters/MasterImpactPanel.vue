<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { templateDetailPath } from '@/routing/routeKeys'
import type { MasterImpactAnalysis } from '@/types/master'

defineProps<{
  impact: MasterImpactAnalysis | null
  loading?: boolean
}>()

const { t } = useI18n()
</script>

<template>
  <el-card shadow="never" class="impact-panel">
    <template #header>
      <span>{{ t('masters.impact.title') }}</span>
    </template>
    <el-skeleton v-if="loading" :rows="3" animated />
    <template v-else-if="impact">
      <p v-if="impact.retestRequired" class="retest-prompt">
        {{ t('masters.impact.retestRequired') }}
      </p>
      <p v-else class="retest-prompt muted">
        {{ t('masters.impact.retestNotRequired') }}
      </p>
      <div v-if="impact.referencedTemplateIds.length > 0" class="template-list">
        <p class="list-label">{{ t('masters.impact.referencedTemplates') }}</p>
        <ul>
          <li v-for="templateId in impact.referencedTemplateIds" :key="templateId">
            <router-link :to="templateDetailPath(templateId)">
              {{ templateId }}
            </router-link>
          </li>
        </ul>
      </div>
      <p v-else class="muted">{{ t('masters.impact.noReferencedTemplates') }}</p>
    </template>
    <p v-else class="muted">{{ t('masters.impact.unavailable') }}</p>
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
