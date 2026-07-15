<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import MasterStatusBadge from '@/components/masters/MasterStatusBadge.vue'
import type { MasterDocumentStatus, MasterReviewDecision } from '@/types/master'

defineProps<{
  status: MasterDocumentStatus
  downloading: boolean
  canReplaceFile: boolean
  canEditMetadata: boolean
  canSubmitForReview: boolean
  canDecideReview: boolean
}>()

const emit = defineEmits<{
  download: []
  replaceFile: []
  editMetadata: []
  openSubmitReview: []
  openReview: [mode: MasterReviewDecision]
}>()

const { t } = useI18n()
</script>

<template>
  <MasterStatusBadge :status="status" />
  <el-button :loading="downloading" @click="emit('download')">
    {{ t('masters.download.action') }}
  </el-button>
  <el-button v-if="canReplaceFile" @click="emit('replaceFile')">
    {{ t('masters.replaceFile.open') }}
  </el-button>
  <el-button v-if="canEditMetadata" @click="emit('editMetadata')">
    {{ t('masters.metadata.edit') }}
  </el-button>
  <el-button v-if="canSubmitForReview" type="primary" @click="emit('openSubmitReview')">
    {{ t('masters.submitReview.open') }}
  </el-button>
  <template v-if="canDecideReview">
    <el-button type="success" @click="emit('openReview', 'APPROVED')">
      {{ t('masters.review.approve') }}
    </el-button>
    <el-button type="danger" @click="emit('openReview', 'REJECTED')">
      {{ t('masters.review.reject') }}
    </el-button>
  </template>
</template>
