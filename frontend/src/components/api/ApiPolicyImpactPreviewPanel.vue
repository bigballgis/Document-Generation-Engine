<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ApiPolicyImpactPreview } from '@/types/template'
import { buildApiPolicyImpactFindings } from '@/utils/apiPolicyImpactFindings'

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

const hardBlockFindings = computed(() =>
  props.preview ? buildApiPolicyImpactFindings(props.preview) : [],
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
  <section class="impact-panel" aria-live="polite" data-testid="api-policy-impact-preview-panel">
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
        <div v-if="preview.defaultRouteImpacted">
          <dt>{{ t('apiPolicy.detail.impact.routeTargets') }}</dt>
          <dd>
            {{
              t('apiPolicy.detail.impact.routeTargetChange', {
                current: preview.currentDefaultRouteTarget || t('apiPolicy.detail.impact.routeTargetNone'),
                candidate: preview.candidateDefaultRouteTarget || t('apiPolicy.detail.impact.routeTargetNone'),
              })
            }}
          </dd>
        </div>
      </dl>

      <ul
        v-if="hardBlockFindings.length > 0"
        class="impact-panel__findings"
        data-testid="api-policy-hard-block-finding"
      >
        <li
          v-for="finding in hardBlockFindings"
          :key="finding.code"
          class="impact-panel__finding"
        >
          <dl>
            <div>
              <dt>{{ t('apiPolicy.detail.impact.reasonLabel') }}</dt>
              <dd data-testid="api-policy-hard-block-reason">{{ t(finding.reasonKey) }}</dd>
            </div>
            <div>
              <dt>{{ t('apiPolicy.detail.impact.impactLabel') }}</dt>
              <dd data-testid="api-policy-hard-block-impact">{{ t(finding.impactKey) }}</dd>
            </div>
            <div>
              <dt>{{ t('apiPolicy.detail.impact.adviceLabel') }}</dt>
              <dd data-testid="api-policy-hard-block-advice">{{ t(finding.adviceKey) }}</dd>
            </div>
            <div>
              <dt>{{ t('apiPolicy.detail.impact.expectedErrorCodeLabel') }}</dt>
              <dd data-testid="api-policy-hard-block-error-code">{{ finding.expectedErrorCode }}</dd>
            </div>
          </dl>
        </li>
      </ul>

      <ul v-else-if="warningMessages.length > 0" class="impact-panel__warnings">
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

<style scoped lang="scss" src="./ApiPolicyImpactPreviewPanel.scss"></style>
