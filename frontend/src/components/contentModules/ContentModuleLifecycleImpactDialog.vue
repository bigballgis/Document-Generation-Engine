<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ContentModuleLifecycleImpactSummary } from '@/types/contentModule'

const props = defineProps<{
  modelValue: boolean
  loading: boolean
  impact: ContentModuleLifecycleImpactSummary | null
  operationLabelKey: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: []
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
    :title="t('contentModules.lifecycle.impactTitle')"
    width="720px"
    destroy-on-close
  >
    <p class="intro">{{ t(operationLabelKey) }}</p>
    <el-skeleton v-if="loading" :rows="5" animated />
    <template v-else-if="impact">
      <dl class="impact-grid">
        <div>
          <dt>{{ t('contentModules.lifecycle.impact.referenceTemplates') }}</dt>
          <dd>{{ impact.referenceTemplateCount }}</dd>
        </div>
        <div class="full-width">
          <dt>{{ t('contentModules.lifecycle.impact.templateList') }}</dt>
          <dd>{{ impact.referenceTemplateListHint }}</dd>
        </div>
        <div class="full-width">
          <dt>{{ t('contentModules.lifecycle.impact.releaseVersions') }}</dt>
          <dd>{{ impact.impactedReleaseVersionsHint }}</dd>
        </div>
        <div>
          <dt>{{ t('contentModules.lifecycle.impact.defaultRoute') }}</dt>
          <dd>
            {{
              impact.defaultRouteAffected
                ? t('contentModules.lifecycle.impact.defaultRouteAffected')
                : t('contentModules.lifecycle.impact.defaultRouteClear')
            }}
          </dd>
        </div>
        <div class="full-width">
          <dt>{{ t('contentModules.lifecycle.impact.recentCalls') }}</dt>
          <dd>{{ impact.recentCallSummary }}</dd>
        </div>
        <div class="full-width">
          <dt>{{ t('contentModules.lifecycle.impact.remediation') }}</dt>
          <dd>{{ impact.remediationHint }}</dd>
        </div>
      </dl>
      <el-alert
        type="warning"
        :title="t('contentModules.lifecycle.impact.confirmPrompt')"
        show-icon
        :closable="false"
      />
    </template>
    <template #footer>
      <el-button @click="visible = false">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :disabled="loading || !impact" @click="emit('confirm')">
        {{ t('contentModules.lifecycle.impact.confirmAction') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.intro {
  margin: 0 0 1rem;
  color: var(--text-muted);
}

.impact-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem 1.25rem;
  margin: 0 0 1rem;
}

.impact-grid dt {
  margin: 0;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: var(--text-muted);
}

.impact-grid dd {
  margin: 0.2rem 0 0;
  color: var(--text-primary);
}

.full-width {
  grid-column: 1 / -1;
}
</style>
