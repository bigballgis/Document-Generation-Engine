<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import MasterStatusBadge from '@/components/masters/MasterStatusBadge.vue'
import type { MasterDocumentStatus } from '@/types/master'

defineProps<{
  status: MasterDocumentStatus
  downloading: boolean
  canReplaceFile: boolean
  canEditMetadata: boolean
}>()

const emit = defineEmits<{
  download: []
  replaceFile: []
  editMetadata: []
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
</template>
