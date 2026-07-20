<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import TemplateExportActions from '@/components/templates/TemplateExportActions.vue'

defineProps<{
  templateId: string
  externalId: string
  submitting: boolean
  showExportActions: boolean
  showDeleteTemplateAction: boolean
  showMetadataEdit: boolean
  showApiSettingsAction: boolean
}>()

const emit = defineEmits<{
  delete: []
  editMetadata: []
  openProperties: []
  openApiSettings: []
}>()

const { t } = useI18n()
</script>

<template>
  <el-button data-testid="hub-properties-action" @click="emit('openProperties')">
    {{ t('templates.packageHub.properties') }}
  </el-button>
  <el-button
    v-if="showApiSettingsAction"
    data-testid="hub-api-settings-action"
    @click="emit('openApiSettings')"
  >
    {{ t('templates.packageHub.apiSettings') }}
  </el-button>
  <TemplateExportActions
    v-if="showExportActions"
    :template-id="templateId"
    :external-id="externalId"
  />
  <el-button
    v-if="showDeleteTemplateAction"
    type="danger"
    plain
    :loading="submitting"
    @click="emit('delete')"
  >
    {{ t('templates.deleteAction.button') }}
  </el-button>
  <el-button v-if="showMetadataEdit" @click="emit('editMetadata')">
    {{ t('templates.metadata.edit') }}
  </el-button>
</template>
