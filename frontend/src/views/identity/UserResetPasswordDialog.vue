<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FormInstance } from 'element-plus'
import { createPasswordValidator, createResetPasswordRules } from '@/views/identity/userPasswordRules'

const visible = defineModel<boolean>('visible', { required: true })

defineProps<{
  submitting: boolean
}>()

const emit = defineEmits<{
  submit: [newPassword: string]
}>()

const { t } = useI18n()
const resetForm = reactive({ newPassword: '' })
const resetFormRef = ref<FormInstance>()

const passwordValidator = createPasswordValidator(t)
const resetRules = computed(() => createResetPasswordRules(passwordValidator))

watch(visible, (open) => {
  if (open) {
    resetForm.newPassword = ''
  }
})

async function submitResetPassword() {
  if (!resetFormRef.value) {
    return
  }
  const valid = await resetFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  emit('submit', resetForm.newPassword)
}

defineExpose({ submitResetPassword, resetForm })
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="t('identity.users.resetPasswordTitle')"
    width="480px"
    destroy-on-close
  >
    <el-form ref="resetFormRef" :model="resetForm" :rules="resetRules" label-position="top">
      <el-form-item :label="t('identity.users.form.newPassword')" prop="newPassword">
        <el-input v-model="resetForm.newPassword" type="password" show-password maxlength="128" />
        <div class="field-hint">{{ t('identity.users.form.passwordHint') }}</div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">{{ t('identity.actions.cancel') }}</el-button>
      <el-button type="primary" :loading="submitting" @click="submitResetPassword">
        {{ t('identity.actions.confirm') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.field-hint {
  margin-top: 0.35rem;
  color: var(--text-muted);
  font-size: 0.8125rem;
}
</style>
