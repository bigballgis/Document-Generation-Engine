<script setup lang="ts">
import { toRef } from 'vue'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import ScopedGroupSelect from '@/components/common/ScopedGroupSelect.vue'
import TemplateRiskPromptConfigPanel from '@/components/templates/TemplateRiskPromptConfigPanel.vue'
import { useTemplateCreateDialog } from '@/components/templates/useTemplateCreateDialog'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  created: [templateId: string]
}>()

const {
  t,
  templatesStore,
  formRef,
  visible,
  form,
  advancedSections,
  riskPromptFormState,
  formRules,
  apiErrorMessage,
  masterOptions,
  handleSubmit,
} = useTemplateCreateDialog({
  modelValue: toRef(props, 'modelValue'),
  emitModelValue: (value) => emit('update:modelValue', value),
  emitCreated: (templateId) => emit('created', templateId),
})
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="t('templates.create.title')"
    width="560px"
    destroy-on-close
  >
    <el-alert
      v-if="apiErrorMessage"
      type="error"
      :closable="false"
      show-icon
      class="create-error"
      :title="apiErrorMessage"
    />
    <el-form ref="formRef" :model="form" :rules="formRules" label-position="top">
      <el-form-item :label="t('templates.create.groupCode')" prop="groupCode">
        <ScopedGroupSelect
          v-model="form.groupCode"
          :placeholder="t('templates.create.groupCodePlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="t('templates.create.master')" prop="masterId">
        <AppSearchSelect
          v-model="form.masterId"
          :placeholder="t('templates.create.masterPlaceholder')"
          :disabled="!form.groupCode"
        >
          <el-option
            v-for="option in masterOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </AppSearchSelect>
      </el-form-item>
      <el-form-item :label="t('templates.create.externalId')" prop="externalId">
        <el-input v-model="form.externalId" :placeholder="t('templates.create.externalIdPlaceholder')" />
      </el-form-item>
      <el-form-item :label="t('templates.create.name')" prop="name">
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item :label="t('templates.create.description')">
        <el-input v-model="form.description" type="textarea" :rows="3" />
      </el-form-item>

      <el-collapse v-model="advancedSections" class="create-advanced">
        <el-collapse-item :title="t('templates.riskPrompt.createSectionTitle')" name="riskPrompt">
          <TemplateRiskPromptConfigPanel
            v-model:form-state="riskPromptFormState"
            create-mode
            :show-save="false"
          />
        </el-collapse-item>
      </el-collapse>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">{{ t('templates.create.cancel') }}</el-button>
      <el-button type="primary" :loading="templatesStore.submitting" @click="handleSubmit">
        {{ t('templates.create.submit') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss" src="./TemplateCreateDialog.scss"></style>
