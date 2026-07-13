<script setup lang="ts">
/* eslint-disable vue/no-mutating-props -- reactive form/filter bag owned by parent */
import { useI18n } from 'vue-i18n'

defineProps<{
  modelValue: boolean
  editingId: string | null
  saving: boolean
  form: {
    name: string
    description: string
    scenarioName: string
    required: boolean
  }
  coverageTagsText: string
  variablesJson: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'update:coverageTagsText': [value: string]
  'update:variablesJson': [value: string]
  save: []
}>()

const { t } = useI18n()
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="editingId ? t('templates.testDataSets.editTitle') : t('templates.testDataSets.createTitle')"
    width="520px"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form label-position="top">
      <el-form-item :label="t('templates.testDataSets.name')">
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item :label="t('templates.testDataSets.descriptionLabel')">
        <el-input v-model="form.description" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item :label="t('templates.testDataSets.scenarioName')">
        <el-input v-model="form.scenarioName" />
      </el-form-item>
      <el-form-item :label="t('templates.testDataSets.coverageTags')">
        <el-input
          :model-value="coverageTagsText"
          @update:model-value="emit('update:coverageTagsText', $event)"
        />
      </el-form-item>
      <el-form-item :label="t('templates.testDataSets.required')">
        <el-switch v-model="form.required" />
      </el-form-item>
      <el-form-item :label="t('templates.testDataSets.variablesJson')">
        <el-input
          :model-value="variablesJson"
          type="textarea"
          :rows="8"
          @update:model-value="emit('update:variablesJson', $event)"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">{{ t('templates.testDataSets.cancel') }}</el-button>
      <el-button type="primary" :loading="saving" @click="emit('save')">
        {{ t('templates.testDataSets.save') }}
      </el-button>
    </template>
  </el-dialog>
</template>

