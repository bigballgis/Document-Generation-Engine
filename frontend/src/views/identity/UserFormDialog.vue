<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { FormInstance } from 'element-plus'
import AppSearchSelect from '@/components/common/AppSearchSelect.vue'
import type { ManagementRole } from '@/types/identity'
import { createPasswordValidator, createUserFormRules } from '@/views/identity/userPasswordRules'

export interface UserFormState {
  username: string
  displayName: string
  email: string
  initialPassword: string
  roles: ManagementRole[]
  authorizedGroupCodes: string[]
}

const visible = defineModel<boolean>('visible', { required: true })
const form = defineModel<UserFormState>('form', { required: true })

defineProps<{
  mode: 'create' | 'edit'
  roleOptions: ManagementRole[]
  groupOptions: string[]
  submitting: boolean
}>()

const emit = defineEmits<{
  submit: []
}>()

const formRef = ref<FormInstance>()

const { t, te } = useI18n()

const passwordValidator = createPasswordValidator(t)
const formRules = computed(() => createUserFormRules(t, passwordValidator))

function roleLabel(role: string): string {
  return te(`identity.roles.${role}`) ? t(`identity.roles.${role}`) : role
}

async function submitForm() {
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  emit('submit')
}

defineExpose({ submitForm })
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="mode === 'create' ? t('identity.users.createTitle') : t('identity.users.editTitle')"
    width="560px"
    destroy-on-close
  >
    <el-form ref="formRef" :model="form" :rules="formRules" label-position="top">
      <el-form-item
        v-if="mode === 'create'"
        :label="t('identity.users.form.username')"
        prop="username"
      >
        <el-input
          v-model="form.username"
          maxlength="8"
          :placeholder="t('identity.users.form.usernamePlaceholder')"
        />
      </el-form-item>
      <el-form-item :label="t('identity.users.form.displayName')" prop="displayName">
        <el-input v-model="form.displayName" maxlength="128" />
      </el-form-item>
      <el-form-item :label="t('identity.users.form.email')" prop="email">
        <el-input v-model="form.email" maxlength="256" />
      </el-form-item>
      <el-form-item
        v-if="mode === 'create'"
        :label="t('identity.users.form.initialPassword')"
        prop="initialPassword"
      >
        <el-input v-model="form.initialPassword" type="password" show-password maxlength="128" />
        <div class="field-hint">{{ t('identity.users.form.passwordHint') }}</div>
      </el-form-item>
      <el-form-item :label="t('identity.users.form.roles')" prop="roles">
        <AppSearchSelect
          v-model="form.roles"
          multiple
          class="full-width"
          :placeholder="t('identity.users.form.rolesPlaceholder')"
        >
          <el-option
            v-for="role in roleOptions"
            :key="role"
            :label="roleLabel(role)"
            :value="role"
          />
        </AppSearchSelect>
      </el-form-item>
      <el-form-item
        :label="t('identity.users.form.authorizedGroupCodes')"
        prop="authorizedGroupCodes"
      >
        <AppSearchSelect
          v-model="form.authorizedGroupCodes"
          multiple
          class="full-width"
          :placeholder="t('identity.users.form.authorizedGroupCodesPlaceholder')"
        >
          <el-option v-for="code in groupOptions" :key="code" :label="code" :value="code" />
        </AppSearchSelect>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">{{ t('identity.actions.cancel') }}</el-button>
      <el-button type="primary" :loading="submitting" @click="submitForm">
        {{ t('identity.actions.save') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.full-width {
  width: 100%;
}

.field-hint {
  margin-top: 0.35rem;
  color: var(--text-muted);
  font-size: 0.8125rem;
}
</style>
