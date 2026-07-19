<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { ApprovalMatrixMode, ApprovalStage, ApprovalSubState } from '@/types/approvalMatrix'
import {
  approvalStageLabelKey,
  deriveApprovalStage,
  normalizeApprovalMatrixMode,
} from '@/utils/approvalMatrix'

const props = defineProps<{
  approvalMatrixMode?: ApprovalMatrixMode | null
  approvalSubState?: ApprovalSubState | null
  approvalStage?: ApprovalStage | null
}>()

const { t } = useI18n()

const mode = computed(() => normalizeApprovalMatrixMode(props.approvalMatrixMode))

const stage = computed(
  () => props.approvalStage ?? deriveApprovalStage(props.approvalSubState),
)

const visible = computed(
  () => mode.value === 'LEGAL_THEN_COMPLIANCE' && stage.value != null,
)
</script>

<template>
  <el-alert
    v-if="visible"
    class="approval-stage-indicator"
    type="info"
    :closable="false"
    show-icon
    data-testid="approval-stage-indicator"
  >
    <template #title>
      {{ t('templates.approvalMatrix.stageBannerTitle') }}
    </template>
    <p class="approval-stage-indicator__body">
      {{ t(approvalStageLabelKey(stage!)) }}
      —
      {{ t('templates.approvalMatrix.stageBannerBody') }}
    </p>
  </el-alert>
</template>

<style scoped lang="scss">
.approval-stage-indicator {
  margin-bottom: 1rem;

  &__body {
    margin: 0;
    color: var(--text-primary);
  }
}
</style>
