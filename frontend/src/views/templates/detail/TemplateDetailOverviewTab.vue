<script setup lang="ts">
import { computed, onMounted, ref, toRef, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { getMaster } from '@/api/masters'
import EntityLinkCell from '@/components/common/EntityLinkCell.vue'
import LocaleVariantFamilyNav from '@/components/common/LocaleVariantFamilyNav.vue'
import TemplateApprovalMatrixModeField from '@/components/templates/TemplateApprovalMatrixModeField.vue'
import { useCapabilities } from '@/composables/useCapabilities'
import { useConfirmAction } from '@/composables/useConfirmAction'
import { useEntityLinkTargets } from '@/composables/useEntityLinkTargets'
import { useTemplateLocaleVariantSiblings } from '@/composables/useLocaleVariantFamilySiblings'
import { useAuthorWorkflowStore } from '@/stores/authorWorkflow'
import { useTemplatesStore } from '@/stores/templates'
import type { ApprovalMatrixMode } from '@/types/approvalMatrix'
import type { TemplateDetail } from '@/types/template'
import {
  approvalMatrixModeLabelKey,
  isApprovalMatrixModeWritable,
  normalizeApprovalMatrixMode,
} from '@/utils/approvalMatrix'

const props = defineProps<{
  template: TemplateDetail
  formatDateTime: (value: string) => string
}>()

const { t } = useI18n()
const { masterDetailLink, templateDetailLink, groupCatalogLink } = useEntityLinkTargets()
const { authorTemplates } = useCapabilities()
const { confirmAction } = useConfirmAction()
const templatesStore = useTemplatesStore()
const authorWorkflowStore = useAuthorWorkflowStore()
const masterName = ref<string | null>(null)
const completing = ref(false)
const savingMode = ref(false)
const draftApprovalMatrixMode = ref<ApprovalMatrixMode>(
  normalizeApprovalMatrixMode(props.template.approvalMatrixMode),
)
const { siblings: localeVariantSiblings, loading: localeVariantLoading } =
  useTemplateLocaleVariantSiblings(toRef(props, 'template'))

const nextReviewDueLabel = computed(() => {
  const due = props.template.nextReviewDue
  if (!due) {
    return t('templates.detail.nextReviewDueUnset')
  }
  return due
})

const modeWritable = computed(
  () =>
    authorTemplates.value &&
    isApprovalMatrixModeWritable({
      lifecycleStatus: props.template.lifecycleStatus,
      approvalSubState: props.template.approvalSubState,
    }),
)

const approvalMatrixModeLabel = computed(() =>
  t(approvalMatrixModeLabelKey(normalizeApprovalMatrixMode(props.template.approvalMatrixMode))),
)

/** N4 — locale facts live in LocaleVariantFamilyNav when that block is shown. */
const showStandaloneLocaleRow = computed(
  () => !(props.template.locale || props.template.localeVariantFamilyId),
)

watch(
  () => props.template.approvalMatrixMode,
  (mode) => {
    draftApprovalMatrixMode.value = normalizeApprovalMatrixMode(mode)
  },
)

async function saveApprovalMatrixMode() {
  if (!modeWritable.value) {
    return
  }
  savingMode.value = true
  try {
    await templatesStore.updateTemplateMetadata(props.template.id, {
      approvalMatrixMode: draftApprovalMatrixMode.value,
    })
    ElMessage.success(t('templates.approvalMatrix.saveSuccess'))
  } catch {
    const key = templatesStore.lastErrorMessageKey ?? 'templates.error.updateMetadata'
    ElMessage.error(t(key))
    draftApprovalMatrixMode.value = normalizeApprovalMatrixMode(props.template.approvalMatrixMode)
  } finally {
    savingMode.value = false
  }
}

onMounted(() => {
  void loadMasterName()
})

async function loadMasterName() {
  try {
    const master = await getMaster(props.template.masterId)
    masterName.value = master.name
  } catch {
    masterName.value = null
  }
}

async function completeAnnualReview() {
  const confirmed = await confirmAction({
    titleKey: 'templates.detail.annualReview.confirmTitle',
    messageKey: 'templates.detail.annualReview.confirmMessage',
    type: 'warning',
  })
  if (!confirmed) {
    return
  }
  completing.value = true
  try {
    await templatesStore.completeAnnualReview(props.template.id)
    ElMessage.success(t('templates.detail.annualReview.success'))
    if (authorTemplates.value) {
      void authorWorkflowStore.fetchAnnualReviewDueTasks().catch(() => {
        /* degrade — list may be stale until next dashboard load */
      })
    }
  } catch {
    const key = templatesStore.lastErrorMessageKey ?? 'templates.error.lifecycle'
    ElMessage.error(t(key))
  } finally {
    completing.value = false
  }
}
</script>

<template>
  <el-card shadow="never" class="section-card" data-testid="template-overview-summary">
    <h2>{{ t('templates.detail.summaryTitle') }}</h2>
    <dl class="summary-grid">
      <div>
        <dt>{{ t('templates.detail.name') }}</dt>
        <dd>{{ template.name }}</dd>
      </div>
      <div data-testid="template-overview-group-code">
        <dt>{{ t('templates.detail.groupCode') }}</dt>
        <dd>
          <EntityLinkCell
            :label="template.groupCode"
            :to="groupCatalogLink(template.groupCode)"
          />
        </dd>
      </div>
      <div data-testid="template-overview-external-id">
        <dt>{{ t('templates.detail.externalId') }}</dt>
        <dd>{{ template.externalId }}</dd>
      </div>
      <div v-if="showStandaloneLocaleRow" data-testid="template-overview-locale">
        <dt>{{ t('templates.detail.locale') }}</dt>
        <dd>{{ template.locale || '—' }}</dd>
      </div>
      <div data-testid="template-overview-approval-matrix-mode">
        <dt>{{ t('templates.approvalMatrix.label') }}</dt>
        <dd>{{ approvalMatrixModeLabel }}</dd>
      </div>
      <div>
        <dt>{{ t('templates.detail.masterId') }}</dt>
        <dd>
          <EntityLinkCell
            :label="masterName ?? template.masterId"
            :to="masterDetailLink(template.masterId)"
          />
        </dd>
      </div>
      <div>
        <dt>{{ t('templates.detail.releaseVersion') }}</dt>
        <dd>{{ template.releaseVersion ?? t('templates.detail.noReleaseVersion') }}</dd>
      </div>
      <div>
        <dt>{{ t('templates.detail.updatedAt') }}</dt>
        <dd>{{ formatDateTime(template.updatedAt) }}</dd>
      </div>
      <div data-testid="template-annual-review-due">
        <dt>{{ t('templates.detail.nextReviewDue') }}</dt>
        <dd data-testid="template-annual-review-due-value">{{ nextReviewDueLabel }}</dd>
      </div>
    </dl>
    <p class="description">
      {{ template.description ?? t('templates.detail.noDescription') }}
    </p>
    <div
      v-if="modeWritable"
      class="approval-matrix-edit"
      data-testid="template-overview-approval-matrix-edit"
    >
      <TemplateApprovalMatrixModeField v-model="draftApprovalMatrixMode" />
      <el-button
        type="primary"
        data-testid="template-overview-approval-matrix-save"
        :loading="savingMode"
        :disabled="savingMode || templatesStore.submitting"
        @click="saveApprovalMatrixMode"
      >
        {{ t('templates.approvalMatrix.save') }}
      </el-button>
    </div>

    <div v-if="authorTemplates" class="annual-review-actions">
      <el-button
        type="primary"
        data-testid="template-annual-review-complete"
        :loading="completing"
        :disabled="completing || templatesStore.submitting"
        @click="completeAnnualReview"
      >
        {{
          completing
            ? t('templates.detail.annualReview.completing')
            : t('templates.detail.annualReview.complete')
        }}
      </el-button>
    </div>

    <LocaleVariantFamilyNav
      v-if="template.locale || template.localeVariantFamilyId"
      :current-locale="template.locale"
      :siblings="localeVariantSiblings"
      :loading="localeVariantLoading"
      :sibling-link="templateDetailLink"
    />
  </el-card>
</template>

<style scoped lang="scss">
.section-card {
  margin-bottom: 1.5rem;

  h2 {
    margin: 0 0 1rem;
    font-size: 1.125rem;
  }
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1rem;
  margin: 0 0 1rem;

  dt {
    margin: 0;
    font-size: 0.85rem;
    color: var(--text-muted);
  }

  dd {
    margin: 0.25rem 0 0;
    font-weight: 500;
  }
}

.description {
  margin: 0;
  color: var(--text-muted);
}

.approval-matrix-edit {
  margin-top: var(--space-4);
  max-width: 28rem;
}

.annual-review-actions {
  margin-top: var(--space-4);
}
</style>
